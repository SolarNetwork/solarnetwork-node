/* ==================================================================
 * JoinDatumFilterService.java - 17/02/2022 8:35:38 AM
 *
 * Copyright 2022 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.datum.filter.std;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static net.solarnetwork.service.OptionalService.service;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.DatumSourceIdProvider;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Datum filter service that joins multiple datum into a new datum stream.
 *
 * @author matt
 * @version 1.2
 */
public class JoinDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider, DatumSourceIdProvider {

	/** The template parameter name for a property name to be mapped. */
	public static final String PROPERTY_NAME_PARAMETER_KEY = "p";

	private static final String PROPERTY_NAME_PARAMETER = "{p}";

	/** The {@code coalesceThreshold} property default value. */
	public static final int DEFAULT_COALESCE_THRESHOLD = 1;

	/** The {@code swallowInput} property default value. */
	public static final boolean DEFAULT_SWALLOW_INPUT = false;

	private final DatumSamples mergedSamples = new DatumSamples();
	private final Set<String> coalescedSourceIds = new HashSet<>(4, 0.9f);

	private final OptionalService<DatumQueue> datumQueue;
	private String outputSourceId;
	private int coalesceThreshold = DEFAULT_COALESCE_THRESHOLD;
	private boolean swallowInput = DEFAULT_SWALLOW_INPUT;
	private PatternKeyValuePair[] propertySourceMappings;

	/**
	 * Constructor.
	 *
	 * @param datumQueue
	 *        the datum queue
	 */
	public JoinDatumFilterService(OptionalService<DatumQueue> datumQueue) {
		super();
		this.datumQueue = datumQueue;
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> parameters) {
		final long start = incrementInputStats();
		final String outputSourceId = resolvePlaceholders(getOutputSourceId(), parameters);
		if ( !(outputSourceId != null && !outputSourceId.trim().isEmpty()
				&& !outputSourceId.equals(datum.getSourceId())
				&& conditionsMatch(datum, samples, parameters)) ) {
			incrementIgnoredStats(start);
			return samples;
		}
		final String[] propSourceMapping = propertySourceMapping(datum);
		synchronized ( mergedSamples ) {
			coalescedSourceIds.add(datum.getSourceId());
			if ( propSourceMapping == null ) {
				// simple case
				mergedSamples.mergeFrom(samples);
			} else {
				// mapping case
				for ( DatumSamplesType type : DatumSamplesType.values() ) {
					if ( type != DatumSamplesType.Tag ) {
						Map<String, ?> data = samples.getSampleData(type);
						if ( data != null && !data.isEmpty() ) {
							for ( Entry<String, ?> e : data.entrySet() ) {
								Map<String, Object> params = new HashMap<>(propSourceMapping.length);
								params.put("p", e.getKey());
								for ( int i = 1; i < propSourceMapping.length; i++ ) {
									params.put(String.valueOf(i), propSourceMapping[i]);
								}
								String p = StringUtils.expandTemplateString(propSourceMapping[0],
										params);
								mergedSamples.putSampleValue(type, p, e.getValue());
							}
						}
					}
				}
			}
			if ( coalescedSourceIds.size() >= coalesceThreshold ) {
				// generate datum
				SimpleDatum d = SimpleDatum.nodeDatum(outputSourceId,
						datum.getTimestamp() != null ? datum.getTimestamp() : Instant.now(),
						new DatumSamples(mergedSamples));
				log.debug("Generated merged datum {}", d);
				DatumQueue dq = service(datumQueue);
				if ( dq != null ) {
					dq.offer(d, true);
				}
				coalescedSourceIds.clear();
			}
		}
		DatumSamplesOperations result = (swallowInput ? null : samples);
		incrementStats(start, samples, result);
		return result;
	}

	/**
	 * Return an array with the property mapping value an optional regular
	 * expression parameter values.
	 *
	 * @param datum
	 *        the datum to find the property source mapping for
	 * @return the mapping data, or {@literal null}
	 */
	private String[] propertySourceMapping(Datum datum) {
		final String inputSourceId = datum.getSourceId();
		final PatternKeyValuePair[] mappings = getPropertySourceMappings();
		if ( mappings != null && mappings.length > 0 ) {
			for ( PatternKeyValuePair mapping : mappings ) {
				String t = mapping.getValue();
				if ( t == null || t.isEmpty() || !t.contains(PROPERTY_NAME_PARAMETER) ) {
					continue;
				}
				String[] result = mapping.keyMatches(inputSourceId);
				if ( result != null ) {
					result[0] = t;
					return result;
				}
			}
		}
		return null;
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = getOutputSourceId();
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.filter.std.join";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return settingSpecifiers(false);
	}

	@Override
	public List<SettingSpecifier> templateSettingSpecifiers() {
		return settingSpecifiers(true);
	}

	private List<SettingSpecifier> settingSpecifiers(final boolean template) {
		List<SettingSpecifier> result = baseIdentifiableSettings("");
		populateBaseSampleTransformSupportSettings(result);
		populateStatusSettings(result);

		result.add(new BasicTextFieldSettingSpecifier("outputSourceId", null));

		result.add(new BasicTextFieldSettingSpecifier("coalesceThreshold",
				String.valueOf(DEFAULT_COALESCE_THRESHOLD)));

		result.add(new BasicToggleSettingSpecifier("swallowInput", DEFAULT_SWALLOW_INPUT));

		PatternKeyValuePair[] mappingConfs = getPropertySourceMappings();
		List<PatternKeyValuePair> mappingConfList = (template
				? Collections.singletonList(new PatternKeyValuePair())
				: (mappingConfs != null ? asList(mappingConfs) : emptyList()));
		result.add(SettingUtils.dynamicListSettingSpecifier("propertySourceMappings", mappingConfList,
				new SettingUtils.KeyedListCallback<PatternKeyValuePair>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(PatternKeyValuePair value,
							int index, String key) {
						SettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								PatternKeyValuePair.settings(key + "."));
						return Collections.singletonList(configGroup);
					}
				}));

		return result;
	}

	/**
	 * Get the source coalesce threshold.
	 *
	 * @return the threshold; defaults to {@link #DEFAULT_COALESCE_THRESHOLD}
	 */
	public int getCoalesceThreshold() {
		return coalesceThreshold;
	}

	/**
	 * Set the source coalesce threshold.
	 *
	 * @param coalesceThreshold
	 *        the coalesceThreshold to set
	 */
	public void setCoalesceThreshold(int coalesceThreshold) {
		this.coalesceThreshold = coalesceThreshold;
	}

	/**
	 * Get the "swallow input" mode.
	 *
	 * @return {@literal true} if input datum should be discarded after merging
	 *         their properties into the output stream, {@literal false} to
	 *         leave input datum unchanged; defaults to
	 *         {@link #DEFAULT_SWALLOW_INPUT}
	 */
	public boolean isSwallowInput() {
		return swallowInput;
	}

	/**
	 * Set the "swallow input" mode.
	 *
	 * @param swallowInput
	 *        {@literal true} if input datum should be discarded after merging
	 *        their properties into the output stream, {@literal false} to leave
	 *        input datum unchanged
	 */
	public void setSwallowInput(boolean swallowInput) {
		this.swallowInput = swallowInput;
	}

	/**
	 * Get the property source mappings.
	 *
	 * @return the mappings, or {@literal null}
	 */
	public PatternKeyValuePair[] getPropertySourceMappings() {
		return propertySourceMappings;
	}

	/**
	 * Set the property source mappings.
	 *
	 * @param propertySourceMappings
	 *        the mappings to set
	 */
	public void setPropertySourceMappings(PatternKeyValuePair[] propertySourceMappings) {
		this.propertySourceMappings = propertySourceMappings;
	}

	/**
	 * Get the number of configured property source mappings.
	 *
	 * @return the number of property source mappings
	 */
	public int getPropertySourceMappingsCount() {
		final PatternKeyValuePair[] mappings = getPropertySourceMappings();
		return (mappings != null ? mappings.length : 0);
	}

	/**
	 * Set the number of configured property source mappings.
	 *
	 * @param count
	 *        the number of mappings to set
	 */
	public void setPropertySourceMappingsCount(int count) {
		setPropertySourceMappings(ArrayUtils.arrayWithLength(getPropertySourceMappings(), count,
				PatternKeyValuePair.class, PatternKeyValuePair::new));
	}

	/**
	 * Get the generated datum source ID.
	 *
	 * @return the source ID to use for generated datum
	 */
	public String getOutputSourceId() {
		return outputSourceId;
	}

	/**
	 * Set the generated datum source ID.
	 *
	 * @param outputSourceId
	 *        the source ID to use for generated datum
	 */
	public void setOutputSourceId(String outputSourceId) {
		this.outputSourceId = outputSourceId;
	}

}

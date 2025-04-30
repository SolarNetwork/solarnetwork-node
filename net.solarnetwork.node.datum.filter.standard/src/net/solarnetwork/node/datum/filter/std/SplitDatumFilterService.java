/* ==================================================================
 * SplitDatumFilterService.java - 30/03/2023 6:34:37 am
 *
 * Copyright 2023 SolarNetwork.net Dev Team
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
import static java.util.Collections.singletonList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
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
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Datum filter service that splits datum into multiple new datum streams.
 *
 * @author matt
 * @version 1.3
 */
public class SplitDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider, DatumSourceIdProvider {

	/** The {@code swallowInput} property default value. */
	public static final boolean DEFAULT_SWALLOW_INPUT = true;

	private final OptionalService<DatumQueue> datumQueue;
	private boolean swallowInput = DEFAULT_SWALLOW_INPUT;
	private PatternKeyValuePair[] propertySourceMappings;

	/**
	 * Constructor.
	 *
	 * @param datumQueue
	 *        the datum queue
	 */
	public SplitDatumFilterService(OptionalService<DatumQueue> datumQueue) {
		super();
		this.datumQueue = datumQueue;
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> parameters) {
		final long start = incrementInputStats();
		final PatternKeyValuePair[] mappings = getPropertySourceMappings();

		if ( !conditionsMatch(datum, samples, parameters) || mappings == null || mappings.length < 1 ) {
			incrementIgnoredStats(start);
			return samples;
		}

		final Map<String, SimpleDatum> sources = new HashMap<>(4);
		for ( PatternKeyValuePair mapping : mappings ) {
			populateMatchingDatumProperties(datum, samples, parameters, DatumSamplesType.Instantaneous,
					sources, mapping);
			populateMatchingDatumProperties(datum, samples, parameters, DatumSamplesType.Accumulating,
					sources, mapping);
			populateMatchingDatumProperties(datum, samples, parameters, DatumSamplesType.Status, sources,
					mapping);
		}

		final DatumQueue q = OptionalService.service(datumQueue);
		if ( q != null ) {
			for ( SimpleDatum out : sources.values() ) {
				if ( !out.isEmpty() ) {
					q.offer(out);
				}
			}
		}

		DatumSamplesOperations result = (swallowInput ? null : samples);
		incrementStats(start, samples, result);
		return result;
	}

	private void populateMatchingDatumProperties(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> parameters, DatumSamplesType samplesType,
			Map<String, SimpleDatum> sources, PatternKeyValuePair mapping) {
		final String outputSourceId = resolvePlaceholders(mapping.getValue(), parameters);
		if ( outputSourceId == null || outputSourceId.isEmpty() ) {
			return;
		}
		Map<String, ?> props = samples.getSampleData(samplesType);
		if ( props != null ) {
			for ( Entry<String, ?> e : props.entrySet() ) {
				String[] match = mapping.keyMatches(e.getKey());
				if ( match != null ) {
					sources.computeIfAbsent(outputSourceId, k -> {
						SimpleDatum sd = new SimpleDatum(new DatumId(datum.getKind(),
								datum.getObjectId(), k, datum.getTimestamp()), new DatumSamples());
						sd.setTags(samples.getTags());
						return sd;
					}).putSampleValue(samplesType, e.getKey(), e.getValue());
				}
			}
		}
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final PatternKeyValuePair[] mappings = getPropertySourceMappings();
		if ( mappings == null || mappings.length < 1 ) {
			return Collections.emptySet();
		}
		final Set<String> sources = new TreeSet<>();
		for ( PatternKeyValuePair mapping : mappings ) {
			final String outputSourceId = resolvePlaceholders(mapping.getValue(), null);
			if ( outputSourceId == null || outputSourceId.isEmpty() ) {
				continue;
			}
			sources.add(outputSourceId);
		}
		return sources;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.filter.std.split";
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

		result.add(new BasicToggleSettingSpecifier("swallowInput", DEFAULT_SWALLOW_INPUT));

		PatternKeyValuePair[] mappingConfs = getPropertySourceMappings();
		List<PatternKeyValuePair> mappingConfList = (template ? singletonList(new PatternKeyValuePair())
				: (mappingConfs != null ? asList(mappingConfs) : emptyList()));
		result.add(SettingUtils.dynamicListSettingSpecifier("propertySourceMappings", mappingConfList,
				new SettingUtils.KeyedListCallback<PatternKeyValuePair>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(PatternKeyValuePair value,
							int index, String key) {
						SettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								PatternKeyValuePair.settings(key + "."));
						return singletonList(configGroup);
					}
				}));

		return result;
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
	 * <p>
	 * For each pair, the pattern key is the property name(s) to match and the
	 * associated value is the source ID to copy the property(s) to.
	 * </p>
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

}

/* ==================================================================
 * UnchangedPropertyDatumFilterService.java - 3/07/2023 6:37:04 am
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

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Datum filter service that can discard unchanged datum properties, within a
 * maximum time range.
 * 
 * @author matt
 * @version 1.0
 * @since 3.4
 */
public class UnchangedPropertyDatumFilterService extends DatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	/** The {@code unchangedPublishMaxSeconds} property default value. */
	public static final int DEFAULT_UNCHANGED_PUBLISH_MAX_SECONDS = 3599;

	private final ConcurrentMap<String, ConcurrentMap<String, SeenProperty>> seenSources = new ConcurrentHashMap<>(
			8, 0.9f, 2);

	private int unchangedPublishMaxSeconds = DEFAULT_UNCHANGED_PUBLISH_MAX_SECONDS;
	private PropertyFilterConfig[] propConfigs;
	private boolean preserveEmptyDatum;

	/**
	 * Constructor.
	 */
	public UnchangedPropertyDatumFilterService() {
		super();
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> params) {
		final long start = incrementInputStats();
		final PropertyFilterConfig[] configs = getPropConfigs();
		if ( !conditionsMatch(datum, samples, params) || configs == null || configs.length < 1 ) {
			incrementIgnoredStats(start);
			return samples;
		}

		final ConcurrentMap<String, SeenProperty> seenProperties = seenSources
				.computeIfAbsent(datum.getSourceId(), k -> new ConcurrentHashMap<>(4, 0.9f, 2));

		DatumSamples copy = null;

		CONFIG: for ( PropertyFilterConfig config : configs ) {
			final Pattern pat = (config != null ? config.getNamePattern() : null);
			if ( pat == null ) {
				continue;
			}
			for ( DatumSamplesType type : DatumSamplesType.values() ) {
				if ( type == DatumSamplesType.Tag ) {
					continue;
				}
				Map<String, ?> data = samples.getSampleData(type);
				if ( data == null ) {
					continue;
				}
				for ( Entry<String, ?> props : data.entrySet() ) {
					final String propName = props.getKey();
					final Object propVal = props.getValue();
					if ( pat.matcher(propName).find() ) {
						SeenProperty seen = seenProperties.computeIfAbsent(propName,
								k -> new SeenProperty(datum.getTimestamp(), propVal));
						if ( seen.shouldDiscard(datum, config, propName, propVal) ) {
							if ( log.isTraceEnabled() ) {
								log.trace(
										"Filter [{}] discarding datum [{}] unchanged property [{}] with \uD835\uDEE5t {}s",
										getUid(), datum.getSourceId(), propName,
										Duration.between(seen.timestamp, datum.getTimestamp())
												.getSeconds());
							}
							if ( copy == null ) {
								copy = new DatumSamples(samples);
							}
							copy.putSampleValue(type, propName, null);
						} else if ( seen.timestamp.compareTo(datum.getTimestamp()) != 0 ) {
							// update seen time to new datum
							if ( log.isTraceEnabled() ) {
								log.trace(
										"Filter [{}] PRESERVING datum [{}] {} property [{}] with \uD835\uDEE5t {}s",
										getUid(), datum.getSourceId(),
										(seen.value.equals(propVal) ? "unchanged" : "changed"), propName,
										Duration.between(seen.timestamp, datum.getTimestamp())
												.getSeconds());
							}
							seenProperties.replace(propName, seen,
									new SeenProperty(datum.getTimestamp(), propVal));
						}
						continue CONFIG;
					}
				}
			}
		}

		DatumSamplesOperations out = samples;

		// tidy up any empty maps we created during filtering
		if ( copy != null ) {
			for ( DatumSamplesType type : DatumSamplesType.values() ) {
				if ( type == DatumSamplesType.Tag ) {
					continue;
				}
				Map<String, ?> m = copy.getSampleData(type);
				if ( m != null && m.isEmpty() ) {
					copy.setSampleData(type, null);
				}
			}
			if ( copy.isEmpty() && !preserveEmptyDatum ) {
				// all properties removed; datum filtered completely
				out = null;
			} else {
				out = copy;
			}
		}

		incrementStats(start, samples, out);
		return out;
	}

	private final class SeenProperty {

		private final Instant timestamp;
		private final Object value;

		private SeenProperty(Instant timestamp, Object value) {
			super();
			this.timestamp = (timestamp != null ? timestamp : Instant.now());
			this.value = value;
		}

		/**
		 * Test if a datum property value should be discarded because it is
		 * unchanged within the configured maximum seconds threshold.
		 * 
		 * @param datum
		 *        the datum
		 * @param config
		 *        the property filter configuration
		 * @param propName
		 *        the name of the property to test
		 * @param propValue
		 *        the current value of the property to test
		 * @return {@literal true} if the samples should be discarded
		 */
		private boolean shouldDiscard(Datum datum, PropertyFilterConfig config, String propName,
				Object propValue) {
			if ( datum == null || propName == null ) {
				return false;
			}
			final Instant datumTimestamp = (datum.getTimestamp() != null ? datum.getTimestamp()
					: Instant.now());
			if ( datumTimestamp.compareTo(timestamp) == 0 ) {
				return false;
			}
			final int maxSecs = config.getFrequencySeconds() != null ? config.getFrequencySeconds()
					: unchangedPublishMaxSeconds;
			return ((maxSecs < 1 || timestamp.plusSeconds(maxSecs).isAfter(datumTimestamp))
					&& Objects.equals(propValue, value));
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("SeenProperty{");
			if ( timestamp != null ) {
				builder.append("timestamp=");
				builder.append(timestamp);
				builder.append(", ");
			}
			if ( value != null ) {
				builder.append("value=");
				builder.append(value);
			}
			builder.append("}");
			return builder.toString();
		}

	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.filter.std.unchangedprop";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = baseIdentifiableSettings("");
		populateBaseSampleTransformSupportSettings(result);
		populateStatusSettings(result);

		result.add(new BasicTextFieldSettingSpecifier("unchangedPublishMaxSeconds",
				String.valueOf(DEFAULT_UNCHANGED_PUBLISH_MAX_SECONDS)));
		result.add(new BasicToggleSettingSpecifier("preserveEmptyDatum", Boolean.FALSE));

		PropertyFilterConfig[] configs = getPropConfigs();
		List<PropertyFilterConfig> configList = (configs != null ? Arrays.asList(configs)
				: Collections.emptyList());
		result.add(SettingUtils.dynamicListSettingSpecifier("propConfigs", configList,
				new SettingUtils.KeyedListCallback<PropertyFilterConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(PropertyFilterConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								PropertyFilterConfig.settings(key + "."));
						return Collections.singletonList(configGroup);
					}
				}));
		return result;
	}

	/**
	 * Get the unchanged publish maximum seconds.
	 * 
	 * @return the maximum seconds to refrain from publishing an unchanged
	 *         status value, or {@literal 0} for no limit
	 */
	public int getUnchangedPublishMaxSeconds() {
		return unchangedPublishMaxSeconds;
	}

	/**
	 * Set the unchanged publish maximum seconds.
	 * 
	 * @param unchangedPublishMaxSeconds
	 *        the maximum seconds to refrain from publishing an unchanged status
	 *        value, or {@literal 0} for no limit
	 */
	public void setUnchangedPublishMaxSeconds(int unchangedPublishMaxSeconds) {
		this.unchangedPublishMaxSeconds = unchangedPublishMaxSeconds;
	}

	/**
	 * Get the property configurations.
	 * 
	 * @return The property configurations.
	 */
	public PropertyFilterConfig[] getPropConfigs() {
		return this.propConfigs;
	}

	/**
	 * Set an array of property configurations.
	 * 
	 * @param propConfigs
	 *        The property configurations.
	 */
	public void setPropConfigs(PropertyFilterConfig[] propConfigs) {
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 * 
	 * @return The number of {@code propConfigs} elements.
	 */
	public int getPropConfigsCount() {
		PropertyFilterConfig[] incs = this.propConfigs;
		return (incs == null ? 0 : incs.length);
	}

	/**
	 * Adjust the number of configured {@code propConfigs} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link PropertyFilterConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code propConfigs} elements.
	 */
	public void setPropConfigsCount(int count) {
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, count,
				PropertyFilterConfig.class, PropertyFilterConfig::new);
	}

	/**
	 * Get the "preserve empty datum" flag.
	 * 
	 * @return {@literal true} if datum whose properties are all discarded
	 *         should <b>not</b> be filtered (to allow further processing);
	 *         defaults to {@literal false}
	 */
	public boolean isPreserveEmptyDatum() {
		return preserveEmptyDatum;
	}

	/**
	 * Set the "preserve empty datum" flag.
	 * 
	 * @param preserveEmptyDatum
	 *        {@literal true} if datum whose properties are all discarded should
	 *        <b>not</b> be filtered (to allow further processing),
	 *        {@literal false} to otherwise discard the entire datum
	 */
	public void setPreserveEmptyDatum(boolean preserveEmptyDatum) {
		this.preserveEmptyDatum = preserveEmptyDatum;
	}

}

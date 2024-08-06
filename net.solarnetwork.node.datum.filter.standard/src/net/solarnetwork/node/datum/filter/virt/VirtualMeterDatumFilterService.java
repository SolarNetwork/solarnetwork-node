/* ==================================================================
 * VirtualMeterDatumFilterService.java - 16/09/2020 9:14:27 AM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.virt;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.datum.filter.std.DatumFilterSupport;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Samples transform service that simulates an accumulating meter property,
 * derived from another property.
 *
 * @author matt
 * @version 2.5
 * @since 1.4
 */
public class VirtualMeterDatumFilterService extends DatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	/** The datum metadata key for a virtual meter sample value. */
	public static final String VIRTUAL_METER_VALUE_KEY = "vm-value";

	/** The datum metadata key for a virtual meter reading date. */
	public static final String VIRTUAL_METER_DATE_KEY = "vm-date";

	/** The datum metadata key for a virtual meter reading value. */
	public static final String VIRTUAL_METER_READING_KEY = "vm-reading";

	/**
	 * The input and reading "diff" parameter name template.
	 *
	 * @since 1.3
	 */
	public static final String DIFF_PARAMETER_TEMPLATE = "%s_diff";

	private static final BigDecimal TWO = new BigDecimal("2");

	private static final Logger log = LoggerFactory.getLogger(VirtualMeterDatumFilterService.class);

	private final ConcurrentMap<String, GeneralDatumMetadata> sourceMetas = new ConcurrentHashMap<>(8,
			0.9f, 2);
	private final ConcurrentMap<String, PropertySamples> sourceSamples = new ConcurrentHashMap<>(8, 0.9f,
			2);
	private final OptionalService<DatumMetadataService> datumMetadataService;
	private VirtualMeterConfig[] virtualMeterConfigs;
	private VirtualMeterExpressionConfig[] expressionConfigs;

	/**
	 * Constructor.
	 *
	 * @param datumMetadataService
	 *        the metadata service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public VirtualMeterDatumFilterService(OptionalService<DatumMetadataService> datumMetadataService) {
		super();
		this.datumMetadataService = requireNonNullArgument(datumMetadataService, "datumMetadataService");
	}

	@Override
	public void configurationChanged(Map<String, Object> props) {
		super.configurationChanged(props);
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.samplefilter.virtmeter";
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
		List<SettingSpecifier> results = baseIdentifiableSettings();

		results.add(0, new BasicTitleSettingSpecifier("status", statusValue()));
		populateBaseSampleTransformSupportSettings(results);
		populateStatusSettings(results);

		VirtualMeterConfig[] meterConfs = getVirtualMeterConfigs();
		List<VirtualMeterConfig> meterConfsList = (template ? singletonList(new VirtualMeterConfig())
				: (meterConfs != null ? asList(meterConfs) : emptyList()));
		results.add(SettingUtils.dynamicListSettingSpecifier("virtualMeterConfigs", meterConfsList,
				new SettingUtils.KeyedListCallback<VirtualMeterConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(VirtualMeterConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								value.settings(key + ".", getExpressionServices()));
						return singletonList(configGroup);
					}
				}));

		Iterable<ExpressionService> exprServices = services(getExpressionServices());
		if ( exprServices != null ) {
			VirtualMeterExpressionConfig[] exprConfs = getExpressionConfigs();
			List<ExpressionConfig> exprConfsList = (template ? singletonList(new ExpressionConfig())
					: (exprConfs != null ? asList(exprConfs) : emptyList()));
			results.add(SettingUtils.dynamicListSettingSpecifier("expressionConfigs", exprConfsList,
					new SettingUtils.KeyedListCallback<ExpressionConfig>() {

						@Override
						public Collection<SettingSpecifier> mapListSettingKey(ExpressionConfig value,
								int index, String key) {
							BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
									VirtualMeterExpressionConfig.settings(key + ".", exprServices));
							return singletonList(configGroup);
						}
					}));
		}

		return results;
	}

	private String statusValue() {
		StringBuilder buf = new StringBuilder();
		for ( Map.Entry<String, GeneralDatumMetadata> me : sourceMetas.entrySet() ) {
			if ( buf.length() > 0 ) {
				buf.append("; ");
			}
			buf.append(me.getKey()).append(": ");
			Map<String, Map<String, Object>> pms = me.getValue().getPropertyInfo();
			boolean hasMeter = false;
			if ( pms != null ) {
				for ( Map.Entry<String, Map<String, Object>> pmEntry : pms.entrySet() ) {
					Map<String, Object> pm = pmEntry.getValue();
					if ( pm.containsKey(VIRTUAL_METER_READING_KEY) ) {
						if ( hasMeter ) {
							buf.append(", ");
						}
						hasMeter = true;
						buf.append(pmEntry.getKey()).append(" = ")
								.append(pm.get(VIRTUAL_METER_READING_KEY));
					}
				}
			}
			if ( !hasMeter ) {
				buf.append("N/A");
			}
		}
		return buf.toString();
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> parameters) {
		final long start = incrementInputStats();
		if ( virtualMeterConfigs == null || virtualMeterConfigs.length < 1 || datum == null
				|| datum.getSourceId() == null || samples == null ) {
			incrementIgnoredStats(start);
			return samples;
		}
		if ( !conditionsMatch(datum, samples, parameters) ) {
			incrementIgnoredStats(start);
			return samples;
		}
		DatumSamples s = new DatumSamples(samples);
		populateDatumProperties(datum, s, virtualMeterConfigs, parameters);
		incrementStats(start, samples, s);
		return s;
	}

	private GeneralDatumMetadata metadata(final DatumMetadataService service, final String sourceId) {
		return sourceMetas.computeIfAbsent(sourceId, k -> {
			log.info("Requesting datum metadata for source [{}]", sourceId);
			GeneralDatumMetadata m = service.getSourceMetadata(sourceId);
			if ( m == null ) {
				log.info("No existing datum metadata found for source [{}]", sourceId);
				m = new GeneralDatumMetadata();
			} else if ( log.isInfoEnabled() ) {
				log.info("Existing datum metadata found for source [{}]: {}", sourceId,
						m.getPropertyInfo());
			}
			return m;
		});
	}

	private PropertySamples samplesForConfig(VirtualMeterConfig config, String sourceId) {
		if ( config.getRollingAverageCount() < 2 ) {
			return null;
		}
		final String key = sourceId + "." + config.getPropertyKey();
		return sourceSamples.compute(key, (k, v) -> {
			if ( v == null || v.values.length != config.getRollingAverageCount() ) {
				return new PropertySamples(config.getRollingAverageCount());
			}
			return v;
		});
	}

	private void populateDatumProperties(Datum d, DatumSamples samples, VirtualMeterConfig[] meterConfs,
			Map<String, Object> parameters) {
		final DatumMetadataService service = OptionalService.service(datumMetadataService);
		if ( service == null ) {
			// the metadata service is required for virtual meters
			log.warn("DatumMetadataService is required for vitual meters, but not available");
			return;
		}
		final GeneralDatumMetadata metadata;
		try {
			metadata = metadata(service, d.getSourceId());
		} catch ( RuntimeException e ) {
			// catch IO errors and let slide
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			if ( root instanceof IOException ) {
				log.warn("Communication error acquiring metadata for source {}: {}", d.getSourceId(),
						root.toString());
			} else {
				log.error("Error accessing metadata for source {}: {}", d.getSourceId(), root.toString(),
						root);
			}
			return;
		}
		if ( metadata == null ) {
			// should not happen, more to let the compiler know what we're expecting
			log.error("Metadata not available for virtual meters");
			return;
		}

		final long date = d.getTimestamp() != null ? d.getTimestamp().toEpochMilli()
				: System.currentTimeMillis();

		for ( VirtualMeterConfig config : meterConfs ) {
			if ( config.getPropertyKey() == null || config.getPropertyKey().isEmpty()
					|| config.getTimeUnit() == null ) {
				continue;
			}
			final TimeUnit timeUnit = config.getTimeUnit();
			final String meterPropName = config.readingPropertyName();
			if ( samples.hasSampleValue(DatumSamplesType.Accumulating, meterPropName) ) {
				// accumulating value exists for this property already, do not overwrite
				log.warn(
						"Source {} accumulating property [{}] already exists, will not populate virtual meter reading for [{}]",
						d.getSourceId(), meterPropName, config.getPropertyKey());
				continue;
			}
			final BigDecimal currVal = samples.getSampleBigDecimal(config.getPropertyType(),
					config.getPropertyKey());
			final PropertySamples propSamples = samplesForConfig(config, d.getSourceId());
			if ( currVal == null ) {
				log.debug(
						"Source {} {} property [{}] not available, cannot populate virtual meter reading",
						d.getSourceId(), config.getPropertyType(), config.getPropertyKey());
				continue;
			}

			synchronized ( config ) {
				GeneralDatumMetadata metadataOut = new GeneralDatumMetadata();
				BigDecimal prevVal = metadata.getInfoBigDecimal(meterPropName, VIRTUAL_METER_VALUE_KEY);
				Long prevDate = null;
				BigDecimal prevReading = null;
				if ( config.getConfig() != null ) {
					// reset meter reading to this value
					prevReading = new BigDecimal(config.getConfig());
				} else {
					prevDate = metadata.getInfoLong(meterPropName, VIRTUAL_METER_DATE_KEY);
					prevReading = metadata.getInfoBigDecimal(meterPropName, VIRTUAL_METER_READING_KEY);
				}
				if ( prevDate == null || prevVal == null || prevReading == null ) {
					metadataOut.putInfoValue(meterPropName, VIRTUAL_METER_DATE_KEY, date);
					metadataOut.putInfoValue(meterPropName, VIRTUAL_METER_VALUE_KEY, currVal.toString());
					metadataOut.putInfoValue(meterPropName, VIRTUAL_METER_READING_KEY,
							prevReading != null ? prevReading.toString() : config.getMeterReading());
					if ( propSamples != null ) {
						propSamples.addValue(currVal);
					}
					log.info("Virtual meter {}.{} status: {}", d.getSourceId(), meterPropName,
							metadataOut.getPropertyInfo());
				} else if ( prevDate > date ) {
					log.warn(
							"Source [{}] virtual meter [{}] reading date [{}] newer than sample date [{}] by {}ms, will not populate reading",
							d.getSourceId(), meterPropName, new Date(prevDate), new Date(date),
							(prevDate - date));
					continue;
				} else if ( config.getMaxAgeSeconds() > 0
						&& (date - prevDate) > config.getMaxAgeSeconds() * 1000 ) {
					log.warn(
							"Source [{}] virtual meter [{}] previous reading date [{}] greater than allowed age {}s, will not populate reading",
							d.getSourceId(), meterPropName, new Date(prevDate),
							config.getMaxAgeSeconds());
					metadataOut.putInfoValue(meterPropName, VIRTUAL_METER_DATE_KEY, date);
					metadataOut.putInfoValue(meterPropName, VIRTUAL_METER_VALUE_KEY, currVal.toString());
					if ( propSamples != null ) {
						propSamples.addValue(currVal);
					}
				} else {
					final int scale = config.getVirtualMeterScale();
					final BigDecimal msDiff = new BigDecimal(date - prevDate);
					final BigDecimal unitMs = new BigDecimal(timeUnit.toMillis(1));
					BigDecimal meterDiff;
					BigDecimal newReading = null;
					VirtualMeterExpressionConfig exprConfig = expressionForConfig(meterPropName);
					if ( exprConfig != null ) {
						Map<String, Object> params = smartPlaceholders(parameters);
						VirtualMeterExpressionRootImpl root = new VirtualMeterExpressionRootImpl(d,
								samples, params, service(getDatumService()),
								service(getMetadataService()), service(getLocationService()), config,
								prevDate, date, prevVal, currVal, prevReading);
						root.setTariffScheduleProviders(getTariffScheduleProviders());
						populateExpressionDatumProperties(samples,
								new VirtualMeterExpressionConfig[] { exprConfig }, root);
						newReading = samples.getAccumulatingSampleBigDecimal(meterPropName);
						if ( newReading == null ) {
							// no new reading value
							log.debug(
									"Source [{}] virtual meter [{}] expression `{}` did not produce a new reading",
									d.getSourceId(), meterPropName, exprConfig.getExpression());
							continue;
						}
						if ( scale >= 0 && newReading.scale() > scale ) {
							newReading = newReading.setScale(scale, RoundingMode.HALF_UP)
									.stripTrailingZeros();
							samples.putAccumulatingSampleValue(meterPropName, newReading);
						} else {
							BigDecimal stripped = newReading.stripTrailingZeros();
							if ( !stripped.equals(newReading) ) {
								// we stripped off some zeros, so replace now
								newReading = stripped;
								samples.putAccumulatingSampleValue(meterPropName, newReading);
							}
						}
						meterDiff = newReading.subtract(prevReading); // for debug log
					} else {
						if ( config.getPropertyType() == DatumSamplesType.Accumulating ) {
							// accumulation is simply difference between current value and previous
							meterDiff = currVal.subtract(prevVal);
						} else {
							// accumulation is average of previous and current values multiplied by time diff
							meterDiff = prevVal.add(currVal).divide(TWO).multiply(msDiff);
							if ( scale >= 0 ) {
								meterDiff = meterDiff.divide(unitMs, scale, RoundingMode.HALF_UP);
							} else {
								meterDiff = meterDiff.divide(unitMs, RoundingMode.HALF_UP);
							}
						}
						newReading = prevReading.add(meterDiff).stripTrailingZeros();
						samples.putAccumulatingSampleValue(meterPropName, newReading);
					}
					if ( propSamples != null ) {
						propSamples.addValue(currVal);
						samples.putSampleValue(config.getPropertyType(), config.getPropertyKey(),
								propSamples.averageValue(scale));
					}

					if ( config.isIncludeInstantaneousDiffProperty() ) {
						String instDiffPropName = config.instantaneousDiffPropertyName();
						samples.putInstantaneousSampleValue(instDiffPropName,
								meterDiff.stripTrailingZeros());
					}

					if ( config.isTrackOnlyWhenReadingChanges() && newReading.equals(prevReading) ) {
						log.debug(
								"Source [{}] virtual meter [{}] has not changed from {}; configured to ignore",
								d.getSourceId(), meterPropName, prevReading);
						continue;
					}

					metadataOut.putInfoValue(meterPropName, VIRTUAL_METER_DATE_KEY, date);
					metadataOut.putInfoValue(meterPropName, VIRTUAL_METER_VALUE_KEY, currVal.toString());
					metadataOut.putInfoValue(meterPropName, VIRTUAL_METER_READING_KEY,
							newReading.toPlainString());

					// add our "input diff" and "reading diff" value as a transform parameter, for future transforms to access
					if ( parameters != null ) {
						try {
							parameters.put(
									String.format(DIFF_PARAMETER_TEMPLATE, config.getPropertyKey()),
									currVal.subtract(prevVal));
							parameters.put(String.format(DIFF_PARAMETER_TEMPLATE, meterPropName),
									meterDiff);
						} catch ( UnsupportedOperationException e ) {
							log.debug("Cannot populate input diff parameters because map is read-only");
						}
					}
					log.debug(
							"Source [{}] virtual meter [{}] adds {} from {} value {} \u2192 {} over {}ms to reach {}",
							d.getSourceId(), meterPropName, meterDiff, config.getPropertyType(), prevVal,
							currVal, msDiff, newReading);
				}

				metadata.merge(metadataOut, true);
				try {
					service.addSourceMetadata(d.getSourceId(), metadataOut);
					config.setConfig(null); // remove any manual reset
				} catch ( RuntimeException e ) {
					// catch IO errors and let slide
					Throwable root = e;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					if ( root instanceof IOException ) {
						log.warn(
								"Communication error posting metadata for source {} virutal meter {}: {}",
								d.getSourceId(), meterPropName, root.toString());
					} else {
						throw e;
					}
				}
			}
		}
	}

	/**
	 * Find an expression config whose name matches the given property name.
	 *
	 * @param propName
	 *        the property name to match the expression config name
	 * @return the first match, or {@literal null}
	 */
	private VirtualMeterExpressionConfig expressionForConfig(String propName) {
		VirtualMeterExpressionConfig[] configs = getExpressionConfigs();
		if ( configs != null ) {
			for ( VirtualMeterExpressionConfig config : configs ) {
				if ( propName.equalsIgnoreCase(config.getName()) && config.getExpression() != null
						&& config.getExpressionServiceId() != null ) {
					return config;
				}
			}
		}
		return null;
	}

	private static final class PropertySamples {

		private final BigDecimal[] values;
		private int head;

		private PropertySamples(int size) {
			super();
			this.values = new BigDecimal[size];
			this.head = -1;
		}

		private synchronized void addValue(BigDecimal value) {
			++head;
			if ( head >= values.length ) {
				head = 0;
			}
			values[head] = value;
		}

		private synchronized BigDecimal averageValue(int scale) {
			BigDecimal result = null;
			int count = 0;
			for ( int i = 0, len = values.length; i < len; i++ ) {
				if ( values[i] == null ) {
					continue;
				}
				if ( result == null ) {
					result = values[i];
				} else {
					result = result.add(values[i]);
				}
				count++;
			}
			if ( result != null && count > 1 ) {
				if ( scale >= 0 ) {
					result = result.divide(BigDecimal.valueOf(count), scale, RoundingMode.HALF_UP);
				} else {
					result = result.divide(BigDecimal.valueOf(count), RoundingMode.HALF_UP);
				}
				result = result.stripTrailingZeros();
			}
			return result;
		}
	}

	/**
	 * Get the virtual meter configurations.
	 *
	 * @return the virtual meter configurations
	 */
	public VirtualMeterConfig[] getVirtualMeterConfigs() {
		return virtualMeterConfigs;
	}

	/**
	 * Set the virtual meter configurations to use.
	 *
	 * @param virtualMeterConfigs
	 *        the configs to use
	 */
	public void setVirtualMeterConfigs(VirtualMeterConfig[] virtualMeterConfigs) {
		this.virtualMeterConfigs = virtualMeterConfigs;
	}

	/**
	 * Get the number of configured {@code virtualMeterConfigs} elements.
	 *
	 * @return the number of {@code virtualMeterConfigs} elements
	 */
	public int getVirtualMeterConfigsCount() {
		VirtualMeterConfig[] confs = this.virtualMeterConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code virtualMeterConfigs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link VirtualMeterConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code virtualMeterConfigs} elements.
	 */
	public void setVirtualMeterConfigsCount(int count) {
		this.virtualMeterConfigs = ArrayUtils.arrayWithLength(this.virtualMeterConfigs, count,
				VirtualMeterConfig.class, null);
	}

	/**
	 * Get the expression configurations.
	 *
	 * @return the expression configurations
	 * @since 1.4
	 */
	public VirtualMeterExpressionConfig[] getExpressionConfigs() {
		return expressionConfigs;
	}

	/**
	 * Set the expression configurations to use.
	 *
	 * @param expressionConfigs
	 *        the configs to use
	 * @since 1.4
	 */
	public void setExpressionConfigs(VirtualMeterExpressionConfig[] expressionConfigs) {
		this.expressionConfigs = expressionConfigs;
	}

	/**
	 * Get the number of configured {@code expressionConfigs} elements.
	 *
	 * @return the number of {@code expressionConfigs} elements
	 * @since 1.4
	 */
	public int getExpressionConfigsCount() {
		VirtualMeterExpressionConfig[] confs = this.expressionConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code VirtualMeterExpressionConfig}
	 * elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link VirtualMeterExpressionConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code expressionConfigs} elements.
	 * @since 1.4
	 */
	public void setExpressionConfigsCount(int count) {
		this.expressionConfigs = ArrayUtils.arrayWithLength(this.expressionConfigs, count,
				VirtualMeterExpressionConfig.class, null);
	}

}

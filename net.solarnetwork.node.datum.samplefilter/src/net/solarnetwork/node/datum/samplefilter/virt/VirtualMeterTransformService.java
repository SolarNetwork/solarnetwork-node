/* ==================================================================
 * VirtualMeterTransformService.java - 16/09/2020 9:14:27 AM
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

package net.solarnetwork.node.datum.samplefilter.virt;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.solarnetwork.util.OptionalServiceCollection.services;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.datum.samplefilter.SamplesTransformerSupport;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.ExpressionConfig;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.support.ExpressionService;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.OptionalService;

/**
 * Samples transform service that simulates an accumulating meter property,
 * derived from another property.
 * 
 * @author matt
 * @version 1.2
 * @since 1.4
 */
public class VirtualMeterTransformService extends SamplesTransformerSupport
		implements GeneralDatumSamplesTransformService, SettingSpecifierProvider {

	/** The datum metadata key for a virtual meter sample value. */
	public static final String VIRTUAL_METER_VALUE_KEY = "vm-value";

	/** The datum metadata key for a virtual meter reading date. */
	public static final String VIRTUAL_METER_DATE_KEY = "vm-date";

	/** The datum metadata key for a virtual meter reading value. */
	public static final String VIRTUAL_METER_READING_KEY = "vm-reading";

	private static final BigDecimal TWO = new BigDecimal("2");

	private static final Logger log = LoggerFactory.getLogger(VirtualMeterTransformService.class);

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
	public VirtualMeterTransformService(OptionalService<DatumMetadataService> datumMetadataService) {
		super();
		if ( datumMetadataService == null ) {
			throw new IllegalArgumentException("The datumMetadataService must not be null.");
		}
		this.datumMetadataService = datumMetadataService;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.samplefilter.virtmeter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = baseIdentifiableSettings(null);

		results.add(0, new BasicTitleSettingSpecifier("status", statusValue()));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));

		VirtualMeterConfig[] meterConfs = getVirtualMeterConfigs();
		List<VirtualMeterConfig> meterConfsList = (meterConfs != null ? asList(meterConfs)
				: emptyList());
		results.add(SettingsUtil.dynamicListSettingSpecifier("virtualMeterConfigs", meterConfsList,
				new SettingsUtil.KeyedListCallback<VirtualMeterConfig>() {

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
			List<ExpressionConfig> exprConfsList = (exprConfs != null ? asList(exprConfs) : emptyList());
			results.add(SettingsUtil.dynamicListSettingSpecifier("expressionConfigs", exprConfsList,
					new SettingsUtil.KeyedListCallback<ExpressionConfig>() {

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
	public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
			Map<String, Object> parameters) {
		if ( virtualMeterConfigs == null || virtualMeterConfigs.length < 1 || datum == null
				|| datum.getSourceId() == null || samples == null ) {
			return samples;
		}
		if ( sourceIdMatches(datum) ) {
			GeneralDatumSamples s = new GeneralDatumSamples(samples);
			populateDatumProperties(datum, s, virtualMeterConfigs, parameters);
			return s;
		}
		return samples;
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

	private void populateDatumProperties(Datum d, GeneralDatumSamples samples,
			VirtualMeterConfig[] meterConfs, Map<String, ?> parameters) {
		final DatumMetadataService service = OptionalService.service(datumMetadataService);
		if ( service == null ) {
			// the metadata service is required for virtual meters
			log.warn("DatumMetadataService is required for vitual meters, but not available");
			return;
		}
		final GeneralDatumMetadata metadata = metadata(service, d.getSourceId());
		if ( metadata == null ) {
			// should not happen, more to let the compiler know what we're expecting
			log.error("Metadata not available for virtual meters");
			return;
		}

		final long date = d.getCreated() != null ? d.getCreated().getTime() : System.currentTimeMillis();

		for ( VirtualMeterConfig config : meterConfs ) {
			if ( config.getPropertyKey() == null || config.getPropertyKey().isEmpty()
					|| config.getTimeUnit() == null ) {
				continue;
			}
			final TimeUnit timeUnit = config.getTimeUnit();
			final String meterPropName = config.readingPropertyName();
			if ( samples.hasSampleValue(GeneralDatumSamplesType.Accumulating, meterPropName) ) {
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
				Map<String, Map<String, Object>> pm = metadata.getPropertyInfo();
				if ( pm == null ) {
					pm = new LinkedHashMap<String, Map<String, Object>>(8);
					metadata.setPm(pm);
				}
				Long prevDate = metadata.getInfoLong(meterPropName, VIRTUAL_METER_DATE_KEY);
				BigDecimal prevVal = metadata.getInfoBigDecimal(meterPropName, VIRTUAL_METER_VALUE_KEY);
				BigDecimal prevReading = metadata.getInfoBigDecimal(meterPropName,
						VIRTUAL_METER_READING_KEY);
				if ( prevDate == null || prevVal == null || prevReading == null ) {
					Map<String, Object> meterPropMap = new LinkedHashMap<>(3);
					meterPropMap.put(VIRTUAL_METER_DATE_KEY, date);
					meterPropMap.put(VIRTUAL_METER_VALUE_KEY, currVal.toString());
					meterPropMap.put(VIRTUAL_METER_READING_KEY,
							prevReading != null ? prevReading.toString() : config.getMeterReading());
					pm.put(meterPropName, meterPropMap);
					if ( propSamples != null ) {
						propSamples.addValue(currVal);
					}
					log.info("Virtual meter {}.{} status: {}", d.getSourceId(), meterPropName,
							meterPropMap);
				} else if ( prevDate > date ) {
					log.warn(
							"Source [{}] virtual meter [{}] reading date [{}] not older than sample date [{}], will not populate reading",
							d.getSourceId(), meterPropName, prevDate, meterPropName, date);
					continue;
				} else if ( config.getMaxAgeSeconds() > 0
						&& (date - prevDate) > config.getMaxAgeSeconds() * 1000 ) {
					log.warn(
							"Source [{}] virtual meter [{}] previous reading date [{}] greater than allowed age {}s, will not populate reading",
							d.getSourceId(), meterPropName, new Date(prevDate),
							config.getMaxAgeSeconds());
					metadata.putInfoValue(meterPropName, VIRTUAL_METER_DATE_KEY, date);
					metadata.putInfoValue(meterPropName, VIRTUAL_METER_VALUE_KEY, currVal.toString());
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
						VirtualMeterExpressionRoot root = new VirtualMeterExpressionRootImpl(d, samples,
								parameters, config, prevDate, date, prevVal, currVal, prevReading);
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
							newReading = newReading.setScale(scale, RoundingMode.HALF_UP);
							samples.putAccumulatingSampleValue(meterPropName, newReading);
						} else {
							BigDecimal stripped = newReading.stripTrailingZeros();
							if ( !stripped.equals(newReading) ) {
								// we stripped off some zeros, so replace now
								samples.putAccumulatingSampleValue(meterPropName, newReading);
							}
						}
						meterDiff = newReading.subtract(prevReading); // for debug log
					} else {
						if ( config.getPropertyType() == GeneralDatumSamplesType.Accumulating ) {
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
						newReading = prevReading.add(meterDiff);
						samples.putAccumulatingSampleValue(meterPropName, newReading);
					}
					if ( propSamples != null ) {
						propSamples.addValue(currVal);
						samples.putSampleValue(config.getPropertyType(), config.getPropertyKey(),
								propSamples.averageValue(scale));
					}

					newReading = newReading.stripTrailingZeros();
					if ( config.isTrackOnlyWhenReadingChanges() && newReading.equals(prevReading) ) {
						log.debug(
								"Source [{}] virtual meter [{}] has not changed from {}; configured to ignore",
								d.getSourceId(), meterPropName, prevReading);
						continue;
					}

					metadata.putInfoValue(meterPropName, VIRTUAL_METER_DATE_KEY, date);
					metadata.putInfoValue(meterPropName, VIRTUAL_METER_VALUE_KEY, currVal.toString());
					metadata.putInfoValue(meterPropName, VIRTUAL_METER_READING_KEY,
							newReading.toPlainString());
					log.debug(
							"Source [{}] virtual meter [{}] adds {} from {} value {} -> {} over {}ms to reach {}",
							d.getSourceId(), meterPropName, meterDiff, config.getPropertyType(), prevVal,
							currVal, msDiff, newReading);
				}
				try {
					service.addSourceMetadata(d.getSourceId(), metadata);
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
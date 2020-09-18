/* ==================================================================
 * VirtualMeterSamplesTransformer.java - 16/09/2020 9:14:27 AM
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

package net.solarnetwork.node.datum.samplefilter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.VirtualMeterConfig;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.OptionalService;

/**
 * Samples transform service that simulates an accumulating meter property,
 * derived from an instantaneous property.
 * 
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public class VirtualMeterSamplesTransformer extends BaseIdentifiable
		implements GeneralDatumSamplesTransformService, SettingSpecifierProvider {

	/** The datum metadata key for a virtual meter sample value. */
	public static final String VIRTUAL_METER_VALUE_KEY = "vm-value";

	/** The datum metadata key for a virtual meter reading date. */
	public static final String VIRTUAL_METER_DATE_KEY = "vm-date";

	/** The datum metadata key for a virtual meter reading value. */
	public static final String VIRTUAL_METER_READING_KEY = "vm-reading";

	private static final int VIRTUAL_METER_SCALE = 6;

	private static final BigDecimal TWO = new BigDecimal("2");

	private static final Logger log = LoggerFactory.getLogger(VirtualMeterSamplesTransformer.class);

	private final ConcurrentMap<String, GeneralDatumMetadata> sourceMetas = new ConcurrentHashMap<>(8,
			0.9f, 2);
	private final OptionalService<DatumMetadataService> datumMetadataService;
	private VirtualMeterConfig[] virtualMeterConfigs;
	private Pattern sourceId;

	/**
	 * Constructor.
	 * 
	 * @param datumMetadataService
	 *        the metadata service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public VirtualMeterSamplesTransformer(OptionalService<DatumMetadataService> datumMetadataService) {
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
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(4);

		results.add(new BasicTitleSettingSpecifier("status", statusValue()));
		results.addAll(baseIdentifiableSettings(null));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));

		VirtualMeterConfig[] meterConfs = getVirtualMeterConfigs();
		List<VirtualMeterConfig> meterConfsList = (meterConfs != null ? Arrays.asList(meterConfs)
				: Collections.<VirtualMeterConfig> emptyList());
		results.add(SettingsUtil.dynamicListSettingSpecifier("virtualMeterConfigs", meterConfsList,
				new SettingsUtil.KeyedListCallback<VirtualMeterConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(VirtualMeterConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								VirtualMeterConfig.settings(key + ".", value.getMeterReading()));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

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
			Map<String, ?> parameters) {
		if ( virtualMeterConfigs == null || virtualMeterConfigs.length < 1 || datum == null
				|| datum.getSourceId() == null || samples == null ) {
			return samples;
		}
		if ( sourceId == null || sourceId.matcher(datum.getSourceId()).find() ) {
			populateDatumProperties(datum, samples, virtualMeterConfigs);
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

	private void populateDatumProperties(Datum d, GeneralDatumSamples samples,
			VirtualMeterConfig[] meterConfs) {
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
			TimeUnit timeUnit = config.getTimeUnit();
			String tuName = timeUnit.name().substring(0, 1) + timeUnit.name().substring(1).toLowerCase();
			String meterPropName = config.getPropertyKey() + tuName;
			if ( samples.hasSampleValue(GeneralDatumSamplesType.Accumulating, meterPropName) ) {
				// accumulating value exists for this property already, do not overwrite
				log.warn(
						"Source {} accumulating property [{}] already exists, will not populate virtual meter reading for [{}]",
						d.getSourceId(), meterPropName, config.getPropertyKey());
				continue;
			}
			BigDecimal currVal = samples.getInstantaneousSampleBigDecimal(config.getPropertyKey());
			if ( currVal == null ) {
				log.warn(
						"Source {} instantaneous property [{}] not available, cannot populate virtual meter reading",
						d.getSourceId(), config.getPropertyKey());
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
					log.info("Virtual meter {}.{} status: {}", d.getSourceId(), meterPropName,
							meterPropMap);
				} else if ( prevDate >= date ) {
					log.warn(
							"Source {} virtual meter reading date {} for {} not older than current time, will not populate reading",
							d.getSourceId(), new Date(prevDate), meterPropName);
					continue;
				} else if ( (date - prevDate) > config.getMaxAgeSeconds() * 1000 ) {
					log.warn(
							"Source {} virtual meter previous reading date {} for {} greater than allowed age {}s, will not populate reading",
							d.getSourceId(), new Date(prevDate), meterPropName);
					metadata.putInfoValue(meterPropName, VIRTUAL_METER_DATE_KEY, date);
					metadata.putInfoValue(meterPropName, VIRTUAL_METER_VALUE_KEY, currVal.toString());
				} else {
					BigDecimal msDiff = new BigDecimal(date - prevDate);
					msDiff.setScale(VIRTUAL_METER_SCALE);
					BigDecimal unitMs = new BigDecimal(timeUnit.toMillis(1));
					unitMs.setScale(VIRTUAL_METER_SCALE);
					BigDecimal meterValue = prevVal.add(currVal).divide(TWO).multiply(msDiff)
							.divide(unitMs, VIRTUAL_METER_SCALE, RoundingMode.HALF_UP);
					BigDecimal currReading = prevReading.add(meterValue);
					samples.putAccumulatingSampleValue(meterPropName, currReading);
					metadata.putInfoValue(meterPropName, VIRTUAL_METER_DATE_KEY, date);
					metadata.putInfoValue(meterPropName, VIRTUAL_METER_VALUE_KEY, currVal.toString());
					metadata.putInfoValue(meterPropName, VIRTUAL_METER_READING_KEY,
							currReading.toString());
					log.info(
							"Source {} virtual meter {} adds {} from instantaneous value {} -> {} over {}ms to reach {}",
							d.getSourceId(), meterPropName, meterValue, prevVal, currVal, msDiff,
							currReading);
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
	 * Get the source ID pattern.
	 * 
	 * @return The pattern.
	 */
	public String getSourceId() {
		return (sourceId != null ? sourceId.pattern() : null);
	}

	/**
	 * Set a source ID pattern to match samples against.
	 * 
	 * Samples will only be considered for filtering if
	 * {@link Datum#getSourceId()} matches this pattern.
	 * 
	 * The {@code sourceIdPattern} must be a valid {@link Pattern} regular
	 * expression. The expression will be allowed to match anywhere in
	 * {@link Datum#getSourceId()} values, so if the pattern must match the full
	 * value only then use pattern positional expressions like {@code ^} and
	 * {@code $}.
	 * 
	 * @param sourceIdPattern
	 *        The source ID regex to match. Syntax errors in the pattern will be
	 *        ignored and a {@code null} value will be set instead.
	 */
	public void setSourceId(String sourceIdPattern) {
		try {
			this.sourceId = (sourceIdPattern != null
					? Pattern.compile(sourceIdPattern, Pattern.CASE_INSENSITIVE)
					: null);
		} catch ( PatternSyntaxException e ) {
			log.warn("Error compiling regex [{}]", sourceIdPattern, e);
			this.sourceId = null;
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
}
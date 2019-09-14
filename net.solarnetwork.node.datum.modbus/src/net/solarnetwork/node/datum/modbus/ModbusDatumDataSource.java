/* ==================================================================
 * ModbusDatumDataSource.java - 20/12/2017 7:04:42 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.modbus;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.expression.ExpressionException;
import bak.pcj.set.IntRange;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.support.ExpressionService;
import net.solarnetwork.support.ExpressionServiceExpression;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.OptionalServiceCollection;
import net.solarnetwork.util.StringUtils;

/**
 * Generic Modbus device datum data source.
 * 
 * @author matt
 * @version 1.6
 */
public class ModbusDatumDataSource extends ModbusDeviceDatumDataSourceSupport implements
		DatumDataSource<GeneralNodeDatum>, SettingSpecifierProvider, ModbusConnectionAction<ModbusData> {

	/** The datum metadata key for a virtual meter sample value. */
	public static final String VIRTUAL_METER_VALUE_KEY = "vm-value";

	/** The datum metadata key for a virtual meter reading date. */
	public static final String VIRTUAL_METER_DATE_KEY = "vm-date";

	/** The datum metadata key for a virtual meter reading value. */
	public static final String VIRTUAL_METER_READING_KEY = "vm-reading";

	private static final int VIRTUAL_METER_SCALE = 6;

	private static final BigDecimal TWO = new BigDecimal("2");

	private String sourceId;
	private long sampleCacheMs;
	private int maxReadWordCount;
	private ModbusPropertyConfig[] propConfigs;
	private ExpressionConfig[] expressionConfigs;
	private VirtualMeterConfig[] virtualMeterConfigs;
	private OptionalServiceCollection<ExpressionService> expressionServices;

	private final ModbusData sample;

	private final AtomicReference<GeneralDatumMetadata> sourceMetadata = new AtomicReference<GeneralDatumMetadata>(
			null);

	public ModbusDatumDataSource() {
		super();
		sample = new ModbusData();
		sourceId = "modbus";
		sampleCacheMs = 5000;
		maxReadWordCount = 64;
		setWordOrder(ModbusWordOrder.MostToLeastSignificant);
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		return Collections.emptyMap();
	}

	@Override
	public Class<? extends GeneralNodeDatum> getDatumType() {
		return GeneralNodeDatum.class;
	}

	@Override
	public GeneralNodeDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final ModbusData currSample = getCurrentSample();
		if ( currSample == null ) {
			return null;
		}
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setCreated(new Date(currSample.getDataTimestamp()));
		d.setSourceId(sourceId);
		populateDatumProperties(currSample, d, propConfigs);
		populateDatumProperties(currSample, d, virtualMeterConfigs);
		populateDatumProperties(currSample, d, expressionConfigs);
		if ( currSample.getDataTimestamp() >= start ) {
			// we read from the device
			postDatumCapturedEvent(d);
		}
		return d;
	}

	private void populateDatumProperties(ModbusData sample, GeneralNodeDatum d,
			ModbusPropertyConfig[] propConfs) {
		if ( propConfs == null ) {
			return;
		}
		for ( ModbusPropertyConfig conf : propConfs ) {
			// skip configurations without a property to set
			if ( conf.getPropertyKey() == null || conf.getPropertyKey().length() < 1 ) {
				continue;
			}
			Object propVal = null;
			switch (conf.getDataType()) {
				case Boolean:
					propVal = sample.getBoolean(conf.getAddress());
					break;

				case Bytes:
					// can't set on datum currently
					break;

				case Float32:
					propVal = sample.getFloat32(conf.getAddress());
					break;

				case Float64:
					propVal = sample.getFloat64(conf.getAddress());
					break;

				case Int16:
					propVal = sample.getSignedInt16(conf.getAddress());
					break;

				case UInt16:
					propVal = sample.getInt16(conf.getAddress());
					break;

				case Int32:
					propVal = sample.getSignedInt32(conf.getAddress());
					break;

				case UInt32:
					propVal = sample.getInt32(conf.getAddress());
					break;

				case Int64:
					propVal = sample.getInt64(conf.getAddress());
					break;

				case UInt64:
					propVal = sample.getUnsignedInt64(conf.getAddress());
					break;

				case StringAscii:
					propVal = sample.getAsciiString(conf.getAddress(), conf.getWordLength(), true);
					break;

				case StringUtf8:
					propVal = sample.getUtf8String(conf.getAddress(), conf.getWordLength(), true);
					break;
			}

			if ( propVal instanceof Number ) {
				if ( conf.getUnitMultiplier() != null ) {
					propVal = applyUnitMultiplier((Number) propVal, conf.getUnitMultiplier());
				}
				if ( conf.getDecimalScale() >= 0 ) {
					propVal = applyDecimalScale((Number) propVal, conf.getDecimalScale());
				}
			}

			if ( propVal != null ) {
				switch (conf.getPropertyType()) {
					case Accumulating:
					case Instantaneous:
						if ( !(propVal instanceof Number) ) {
							log.warn(
									"Cannot set datum accumulating property {} to non-number value [{}]",
									conf.getPropertyKey(), propVal);
							continue;
						}

					default:
						// nothing
				}
				d.putSampleValue(conf.getPropertyType(), conf.getPropertyKey(), propVal);
			}
		}
	}

	private GeneralDatumMetadata metadata(DatumMetadataService service) {
		GeneralDatumMetadata metadata = sourceMetadata.get();
		if ( metadata == null ) {
			log.info("Requesting datum metadata for source [{}]", this.sourceId);
			metadata = service.getSourceMetadata(this.sourceId);
			if ( metadata == null ) {
				log.info("No existing datum metadata found for source [{}]", this.sourceId);
				metadata = new GeneralDatumMetadata();
			} else if ( log.isInfoEnabled() ) {
				log.info("Existing datum metadata found for source [{}]: {}", this.sourceId,
						metadata.getPropertyInfo());
			}
			if ( !sourceMetadata.compareAndSet(null, metadata) ) {
				metadata = sourceMetadata.get();
			}
		}
		return metadata;
	}

	private void populateDatumProperties(ModbusData sample, GeneralNodeDatum d,
			VirtualMeterConfig[] meterConfs) {
		if ( meterConfs == null || meterConfs.length < 1 ) {
			return;
		}
		final DatumMetadataService service = datumMetadataService();
		if ( service == null ) {
			// the metadata service is required for virtual meters
			log.warn("DatumMetadataService is required for vitual meters, but not available");
			return;
		}
		final GeneralDatumMetadata metadata = metadata(service);
		if ( metadata == null ) {
			// should not happen, more to let the compiler know what we're expecting
			log.error("Metadata not available for virtual meters");
			return;
		}

		final long date = sample.getDataTimestamp();

		for ( VirtualMeterConfig config : meterConfs ) {
			if ( config.getPropertyKey() == null || config.getPropertyKey().isEmpty()
					|| config.getTimeUnit() == null ) {
				continue;
			}
			TimeUnit timeUnit = config.getTimeUnit();
			String tuName = timeUnit.name().substring(0, 1) + timeUnit.name().substring(1).toLowerCase();
			String meterPropName = config.getPropertyKey() + tuName;
			if ( d.getSamples().hasSampleValue(GeneralDatumSamplesType.Accumulating, meterPropName) ) {
				// accumulating value exists for this property already, do not overwrite
				log.warn(
						"Accumulating property [{}] already exists, will not populate virtual meter reading for [{}]",
						meterPropName, config.getPropertyKey());
				continue;
			}
			BigDecimal currVal = d.getInstantaneousSampleBigDecimal(config.getPropertyKey());
			if ( currVal == null ) {
				log.warn(
						"Instantaneous property [{}] not available, cannot populate virtual meter reading",
						config.getPropertyKey());
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
					log.info("Virtual meter {} status: {}", meterPropName, meterPropMap);
				} else if ( prevDate >= date ) {
					log.warn(
							"Virtual meter reading date {} for {} not older than current time, will not populate reading",
							new Date(prevDate), meterPropName);
					continue;
				} else if ( (date - prevDate) > config.getMaxAgeSeconds() * 1000 ) {
					log.warn(
							"Virtual meter previous reading date {} for {} greater than allowed age {}s, will not populate reading",
							new Date(prevDate), meterPropName);
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
					d.putAccumulatingSampleValue(meterPropName, currReading);
					metadata.putInfoValue(meterPropName, VIRTUAL_METER_DATE_KEY, date);
					metadata.putInfoValue(meterPropName, VIRTUAL_METER_VALUE_KEY, currVal.toString());
					metadata.putInfoValue(meterPropName, VIRTUAL_METER_READING_KEY,
							currReading.toString());
					log.info(
							"Virtual meter {} adds {} from instantaneous value {} -> {} over {}ms to reach {}",
							meterPropName, meterValue, prevVal, currVal, msDiff, currReading);
				}
				try {
					service.addSourceMetadata(this.sourceId, metadata);
				} catch ( RuntimeException e ) {
					// catch IO errors and let slide
					Throwable root = e;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					if ( root instanceof IOException ) {
						log.warn(
								"Communication error posting metadata for source {} virutal meter {}: {}",
								this.sourceId, meterPropName, root.toString());
					} else {
						throw e;
					}
				}
			}
		}
	}

	private void populateDatumProperties(ModbusData sample, GeneralNodeDatum d,
			ExpressionConfig[] expressionConfs) {
		Iterable<ExpressionService> services = (expressionServices != null
				? expressionServices.services()
				: null);
		if ( services == null || expressionConfs == null || expressionConfs.length < 1 || sample == null
				|| d == null ) {
			return;
		}
		for ( ExpressionConfig config : expressionConfs ) {
			if ( config.getName() == null || config.getName().isEmpty() || config.getExpression() == null
					|| config.getExpression().isEmpty() ) {
				continue;
			}
			final ExpressionServiceExpression expr;
			try {
				expr = config.getExpression(services);
			} catch ( ExpressionException e ) {
				log.warn("Error parsing property [{}] expression `{}`: {}", config.getName(),
						config.getExpression(), e.getMessage());
				return;
			}

			Object propValue = null;
			if ( expr != null ) {
				ExpressionRoot root = new ExpressionRoot(d, sample);
				try {
					propValue = expr.getService().evaluateExpression(expr.getExpression(), null, root,
							null, Object.class);
				} catch ( ExpressionException e ) {
					log.warn("Error evaluating property [{}] expression `{}`: {}", config.getName(),
							config.getExpression(), e.getMessage());
				}
			}
			if ( propValue != null ) {
				d.putSampleValue(config.getDatumPropertyType(), config.getName(), propValue);
			}
		}
	}

	private DatumMetadataService datumMetadataService() {
		OptionalService<DatumMetadataService> opt = getDatumMetadataService();
		return (opt != null ? opt.service() : null);
	}

	private Number applyDecimalScale(Number value, int decimalScale) {
		if ( decimalScale < 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		if ( v.scale() > decimalScale ) {
			v = v.setScale(decimalScale, RoundingMode.HALF_UP);
		}
		return v;
	}

	private Number applyUnitMultiplier(Number value, BigDecimal multiplier) {
		if ( BigDecimal.ONE.compareTo(multiplier) == 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		return v.multiply(multiplier);
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.modbus";
	}

	@Override
	public String getDisplayName() {
		return "Generic Modbus Device";
	}

	private static Map<ModbusReadFunction, List<ModbusPropertyConfig>> getReadFunctionSets(
			ModbusPropertyConfig[] configs) {
		if ( configs == null ) {
			return null;
		}
		Map<ModbusReadFunction, List<ModbusPropertyConfig>> confsByFunction = new LinkedHashMap<>(
				configs.length);
		for ( ModbusPropertyConfig config : configs ) {
			confsByFunction.computeIfAbsent(config.getFunction(), k -> new ArrayList<>(4)).add(config);
		}
		return confsByFunction;
	}

	private static IntRangeSet getRegisterAddressSet(List<ModbusPropertyConfig> configs) {
		IntRangeSet set = new IntRangeSet();
		if ( configs != null ) {
			for ( ModbusPropertyConfig config : configs ) {
				int len = config.getDataType().getWordLength();
				if ( len == -1 ) {
					len = config.getWordLength();
				}
				set.addAll(config.getAddress(), config.getAddress() + len);
			}
		}
		return set;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = getIdentifiableSettingSpecifiers();

		results.add(0, new BasicTitleSettingSpecifier("sample", getSampleMessage(sample.copy()), true));

		results.addAll(getModbusNetworkSettingSpecifiers());

		ModbusDatumDataSource defaults = new ModbusDatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.sampleCacheMs)));
		results.add(new BasicTextFieldSettingSpecifier("maxReadWordCount",
				String.valueOf(defaults.maxReadWordCount)));

		// drop-down menu for word order
		BasicMultiValueSettingSpecifier wordOrderSpec = new BasicMultiValueSettingSpecifier(
				"wordOrderKey", String.valueOf(defaults.getWordOrder().getKey()));
		Map<String, String> wordOrderTitles = new LinkedHashMap<String, String>(2);
		for ( ModbusWordOrder e : ModbusWordOrder.values() ) {
			wordOrderTitles.put(String.valueOf(e.getKey()), e.toDisplayString());
		}
		wordOrderSpec.setValueTitles(wordOrderTitles);
		results.add(wordOrderSpec);

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

		ModbusPropertyConfig[] confs = getPropConfigs();
		List<ModbusPropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<ModbusPropertyConfig> emptyList());
		results.add(SettingsUtil.dynamicListSettingSpecifier("propConfigs", confsList,
				new SettingsUtil.KeyedListCallback<ModbusPropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(ModbusPropertyConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								ModbusPropertyConfig.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		Iterable<ExpressionService> exprServices = (expressionServices != null
				? expressionServices.services()
				: null);
		if ( exprServices != null ) {
			ExpressionConfig[] exprConfs = getExpressionConfigs();
			List<ExpressionConfig> exprConfsList = (exprConfs != null ? Arrays.asList(exprConfs)
					: Collections.<ExpressionConfig> emptyList());
			results.add(SettingsUtil.dynamicListSettingSpecifier("expressionConfigs", exprConfsList,
					new SettingsUtil.KeyedListCallback<ExpressionConfig>() {

						@Override
						public Collection<SettingSpecifier> mapListSettingKey(ExpressionConfig value,
								int index, String key) {
							BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
									ExpressionConfig.settings(key + ".", exprServices));
							return Collections.<SettingSpecifier> singletonList(configGroup);
						}
					}));
		}

		return results;
	}

	private String getSampleMessage(ModbusData sample) {
		if ( sample.getDataTimestamp() < 1 ) {
			return "N/A";
		}

		GeneralNodeDatum d = new GeneralNodeDatum();
		populateDatumProperties(sample, d, propConfigs);

		Map<String, ?> data = d.getSampleData();
		if ( data == null || data.isEmpty() ) {
			return "No data.";
		}

		StringBuilder buf = new StringBuilder();
		buf.append(StringUtils.delimitedStringFromMap(data));
		buf.append("; sampled at ")
				.append(DateTimeFormat.forStyle("LS").print(new DateTime(sample.getDataTimestamp())));
		return buf.toString();
	}

	private static short[] shortArrayForBitSet(BitSet set, int start, int count) {
		short[] result = new short[count];
		for ( int i = 0; i < count; i++ ) {
			result[i] = set.get(start + i) ? (short) 1 : (short) 0;
		}
		return result;
	}

	@Override
	public ModbusData doWithConnection(final ModbusConnection conn) throws IOException {
		sample.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				final int maxReadLen = maxReadWordCount;
				Map<ModbusReadFunction, List<ModbusPropertyConfig>> functionMap = getReadFunctionSets(
						propConfigs);
				Map<ModbusReadFunction, IntRangeSet> functionRangeMap = new HashMap<>(
						functionMap.size());
				for ( Map.Entry<ModbusReadFunction, List<ModbusPropertyConfig>> me : functionMap
						.entrySet() ) {
					ModbusReadFunction function = me.getKey();
					List<ModbusPropertyConfig> configs = me.getValue();
					// try to read from device as few times as possible by combining ranges of addresses
					// into single calls, but limited to at most maxReadWordCount addresses at a time
					// because some devices have trouble returning large word counts
					IntRangeSet addressRangeSet = getRegisterAddressSet(configs);
					functionRangeMap.put(function, addressRangeSet);
					log.debug("Reading modbus {} register ranges: {}", getUnitId(), addressRangeSet);
					IntRange[] ranges = addressRangeSet.ranges();
					for ( IntRange range : ranges ) {
						for ( int start = range.first(); start < range.last(); ) {
							int len = Math.min(range.last() - start, maxReadLen);
							switch (function) {
								case ReadCoil:
									m.saveDataArray(shortArrayForBitSet(
											conn.readDiscreetValues(start, len), start, len), start);
									break;

								case ReadDiscreteInput:
									m.saveDataArray(
											shortArrayForBitSet(conn.readInputDiscreteValues(start, len),
													start, len),
											start);
									break;

								case ReadHoldingRegister:
									m.saveDataArray(
											conn.readUnsignedShorts(
													ModbusReadFunction.ReadHoldingRegister, start, len),
											start);
									break;

								case ReadInputRegister:
									m.saveDataArray(conn.readUnsignedShorts(
											ModbusReadFunction.ReadInputRegister, start, len), start);
									break;
							}
							start += len;
						}
					}
				}

				// handle expression references
				ExpressionConfig[] exprConfigs = getExpressionConfigs();
				if ( exprConfigs != null && exprConfigs.length > 0 ) {
					IntRangeSet readHoldingRegSet = functionRangeMap
							.get(ModbusReadFunction.ReadHoldingRegister);
					IntRangeSet exprAddressRangeSet = new IntRangeSet();
					for ( ExpressionConfig config : exprConfigs ) {
						Set<Integer> addrSet = config.registerAddressReferences();
						for ( Integer addr : addrSet ) {
							// don't add addresses already read
							if ( !readHoldingRegSet.contains(addr) ) {
								exprAddressRangeSet.addAll(addr, addr + 1);
							}
						}
					}
					IntRange[] ranges = exprAddressRangeSet.ranges();
					for ( IntRange range : ranges ) {
						for ( int start = range.first(); start < range.last(); ) {
							int len = Math.min(range.last() - start, maxReadLen);
							m.saveDataArray(conn.readUnsignedShorts(
									ModbusReadFunction.ReadHoldingRegister, start, len), start);
							start += len;
						}
					}
				}

				return true;
			}
		});
		return sample.copy();
	}

	private ModbusData getCurrentSample() {
		ModbusData currSample = null;
		if ( isCachedSampleExpired() ) {
			try {
				currSample = performAction(this);
				if ( currSample != null && log.isTraceEnabled() ) {
					log.trace(currSample.dataDebugString());
				}
			} catch ( IOException e ) {
				Throwable t = e;
				while ( t.getCause() != null ) {
					t = t.getCause();
				}
				log.debug("Error reading from Modbus device {}", modbusDeviceName(), t);
				log.warn("Communication problem reading source from Modbus device {}: {}", this.sourceId,
						modbusDeviceName(), t.getMessage());
			}
		} else {
			currSample = sample.copy();
		}
		return currSample;
	}

	private boolean isCachedSampleExpired() {
		final long lastReadDiff = System.currentTimeMillis() - sample.getDataTimestamp();
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	/**
	 * Get the sample cache maximum age, in milliseconds.
	 * 
	 * @return the cache milliseconds
	 */
	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache maximum age, in milliseconds.
	 * 
	 * @param sampleCacheSecondsMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Get the property configurations.
	 * 
	 * @return the property configurations
	 */
	public ModbusPropertyConfig[] getPropConfigs() {
		return propConfigs;
	}

	/**
	 * Set the property configurations to use.
	 * 
	 * @param propConfigs
	 *        the configs to use
	 */
	public void setPropConfigs(ModbusPropertyConfig[] propConfigs) {
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 * 
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropConfigsCount() {
		ModbusPropertyConfig[] confs = this.propConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propConfigs} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link ModbusPropertyConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code propConfigs} elements.
	 */
	public void setPropConfigsCount(int count) {
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, count,
				ModbusPropertyConfig.class, null);
	}

	/**
	 * Set the maximum number of Modbus registers to read in any single read
	 * operation.
	 * 
	 * <p>
	 * Some modbus devices do not handle large read ranges. This setting can be
	 * used to limit the number of registers read at one time.
	 * </p>
	 * 
	 * @param maxReadWordCount
	 *        the maximum word count; defaults to {@literal 64}
	 */
	public void setMaxReadWordCount(int maxReadWordCount) {
		if ( maxReadWordCount < 1 ) {
			return;
		}
		this.maxReadWordCount = maxReadWordCount;
	}

	/**
	 * Set the source ID to use for returned datum.
	 * 
	 * @param soruceId
	 *        the source ID to use; defaults to {@literal modbus}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the word order.
	 * 
	 * @return the word order
	 * @since 1.2
	 */
	public ModbusWordOrder getWordOrder() {
		return sample.getWordOrder();
	}

	/**
	 * Set the word order.
	 * 
	 * @param wordOrder
	 *        the order to set; {@literal null} will be ignored
	 * @since 1.2
	 */
	public void setWordOrder(ModbusWordOrder wordOrder) {
		if ( wordOrder == null ) {
			return;
		}
		sample.setWordOrder(wordOrder);
	}

	/**
	 * Get the word order as a key value.
	 * 
	 * @return the word order as a key; if {@link #getWordOrder()} is
	 *         {@literal null} then
	 *         {@link ModbusWordOrder#MostToLeastSignificant} will be returned
	 * @since 1.2
	 */
	public char getWordOrderKey() {
		ModbusWordOrder order = getWordOrder();
		if ( order == null ) {
			order = ModbusWordOrder.MostToLeastSignificant;
		}
		return order.getKey();
	}

	/**
	 * Set the word order as a key value.
	 * 
	 * @param key
	 *        the word order key to set; if {@code key} is not valid then
	 *        {@link ModbusWordOrder#MostToLeastSignificant} will be set
	 * @since 1.2
	 */
	public void setWordOrderKey(char key) {
		ModbusWordOrder order;
		try {
			order = ModbusWordOrder.forKey(key);
		} catch ( IllegalArgumentException e ) {
			order = ModbusWordOrder.MostToLeastSignificant;
		}
		setWordOrder(order);
	}

	/**
	 * Get the virtual meter configurations.
	 * 
	 * @return the virtual meter configurations
	 * @since 1.4
	 */
	public VirtualMeterConfig[] getVirtualMeterConfigs() {
		return virtualMeterConfigs;
	}

	/**
	 * Set the virtual meter configurations to use.
	 * 
	 * @param virtualMeterConfigs
	 *        the configs to use
	 * @since 1.4
	 */
	public void setVirtualMeterConfigs(VirtualMeterConfig[] virtualMeterConfigs) {
		this.virtualMeterConfigs = virtualMeterConfigs;
	}

	/**
	 * Get the number of configured {@code virtualMeterConfigs} elements.
	 * 
	 * @return the number of {@code virtualMeterConfigs} elements
	 * @since 1.4
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
	 * @since 1.4
	 */
	public void setVirtualMeterConfigsCount(int count) {
		this.virtualMeterConfigs = ArrayUtils.arrayWithLength(this.virtualMeterConfigs, count,
				VirtualMeterConfig.class, null);
	}

	/**
	 * Get the expression configurations.
	 * 
	 * @return the expression configurations
	 * @since 1.5
	 */
	public ExpressionConfig[] getExpressionConfigs() {
		return expressionConfigs;
	}

	/**
	 * Set the expression configurations to use.
	 * 
	 * @param expressionConfigs
	 *        the configs to use
	 * @since 1.5
	 */
	public void setExpressionConfigs(ExpressionConfig[] expressionConfigs) {
		this.expressionConfigs = expressionConfigs;
	}

	/**
	 * Get the number of configured {@code expressionConfigs} elements.
	 * 
	 * @return the number of {@code expressionConfigs} elements
	 * @since 1.5
	 */
	public int getExpressionConfigsCount() {
		ExpressionConfig[] confs = this.expressionConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code ExpressionConfig} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link ExpressionConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code expressionConfigs} elements.
	 * @since 1.5
	 */
	public void setExpressionConfigsCount(int count) {
		this.expressionConfigs = ArrayUtils.arrayWithLength(this.expressionConfigs, count,
				ExpressionConfig.class, null);
	}

	/**
	 * Configure an optional collection of {@link ExpressionService}.
	 * 
	 * <p>
	 * Configuring these services allows expressions to be defined to calculate
	 * dynamic datum property values at runtime.
	 * </p>
	 * 
	 * @param expressionServices
	 *        the optional {@link ExpressionService} collection to use
	 * @since 1.5
	 */
	public void setExpressionServices(OptionalServiceCollection<ExpressionService> expressionServices) {
		this.expressionServices = expressionServices;
	}

}

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

import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.MutableNodeDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.ModbusRegisterData;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;
import net.solarnetwork.node.io.modbus.support.ModbusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Generic Modbus device datum data source.
 *
 * @author matt
 * @version 3.7
 */
public class ModbusDatumDataSource extends ModbusDeviceDatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider, ModbusConnectionAction<Void>,
		SettingsChangeObserver, ServiceLifecycleObserver {

	/**
	 * The setting UID used by this service.
	 *
	 * @since 3.1
	 */
	public static final String SETTING_UID = "net.solarnetwork.node.datum.modbus";

	/** The {@code sampleCacheMs} property default value. */
	public static final long DEFAULT_SAMPLE_CACHE_MS = 5000L;

	/** The {@code maxReadWordCount} property default value. */
	public static final int DEFAULT_MAX_READ_WORD_COUNT = 64;

	/** The {@code wordOrder} property default value. */
	public static final ModbusWordOrder DEFAULT_WORD_ORDER = ModbusWordOrder.MostToLeastSignificant;

	private String sourceId;
	private long sampleCacheMs;
	private int maxReadWordCount;
	private ModbusPropertyConfig[] propConfigs;

	private final AtomicLong sampleDate = new AtomicLong(0);
	private final ModbusRegisterData data;

	/**
	 * Constructor.
	 */
	public ModbusDatumDataSource() {
		super();
		data = new ModbusRegisterData();
		sampleCacheMs = DEFAULT_SAMPLE_CACHE_MS;
		maxReadWordCount = DEFAULT_MAX_READ_WORD_COUNT;
		setWordOrder(DEFAULT_WORD_ORDER);
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		startSubSampling(this);
		saveMetadata(getSourceId());
	}

	@Override
	public void serviceDidStartup() {
		startSubSampling(this);
	}

	@Override
	public void serviceDidShutdown() {
		stopSubSampling();
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		return Collections.emptyMap();
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return NodeDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		return readCurrentDatum(null);
	}

	@Override
	protected void readSubSampleDatum(DatumDataSource dataSource) {
		NodeDatum datum = readCurrentDatum(SUB_SAMPLE_PROPS);
		log.debug("Got sub-sample datum: {}", datum);
	}

	private NodeDatum readCurrentDatum(Map<String, Object> xformProps) {
		boolean refreshed = refreshDeviceData();
		if ( !refreshed ) {
			return null;
		}
		long ts = sampleDate.get();
		if ( ts < 1 ) {
			return null;
		}
		SimpleDatum d = SimpleDatum.nodeDatum(resolvePlaceholders(sourceId), Instant.ofEpochMilli(ts));
		populateDatumProperties(d, propConfigs);
		populateDatumProperties(d, getExpressionConfigs());
		return d;
	}

	private void populateDatumProperties(MutableNodeDatum d, ModbusPropertyConfig[] propConfs) {
		if ( propConfs == null ) {
			return;
		}
		for ( ModbusPropertyConfig conf : propConfs ) {
			// skip configurations without full configuration set
			if ( !conf.isValid() ) {
				continue;
			}
			Object propVal = currentValue(conf);
			if ( propVal != null ) {
				switch (conf.getPropertyType()) {
					case Accumulating:
					case Instantaneous:
						if ( !(propVal instanceof Number) ) {
							log.warn("Cannot set datum {} property {} to non-number value [{}]",
									conf.getPropertyType(), conf.getPropertyKey(), propVal);
							continue;
						}

					default:
						// nothing
				}
				d.asMutableSampleOperations().putSampleValue(conf.getPropertyType(),
						conf.getPropertyKey(), propVal);
			}
		}
	}

	private Object currentValue(ModbusPropertyConfig config) {
		Object propVal = currentRawValue(config);
		if ( propVal instanceof Boolean && (config.getPropertyType() == DatumSamplesType.Accumulating
				|| config.getPropertyType() == DatumSamplesType.Instantaneous) ) {
			// convert Boolean to 0/1
			propVal = ((Boolean) propVal).booleanValue() ? 1 : 0;
		}
		if ( propVal instanceof Number ) {
			if ( config.getUnitMultiplier() != null ) {
				propVal = applyUnitMultiplier((Number) propVal, config.getUnitMultiplier());
			}
			if ( config.getDecimalScale() >= 0 ) {
				propVal = applyDecimalScale((Number) propVal, config.getDecimalScale());
			}
		}
		return propVal;
	}

	private Object currentRawValue(ModbusPropertyConfig config) {
		final ModbusRegisterBlockType blockType = config.getFunction().blockType();
		switch (blockType) {
			case Coil:
			case Discrete:
				return data.readBits(blockType, bits -> bits.get(config.getAddress()));

			default:
				return data.readRegisters(blockType, d -> d.getValue(config.getDataType(),
						config.getAddress(), config.getWordLength()));
		}
	}

	private void populateDatumProperties(MutableNodeDatum d, ExpressionConfig[] expressionConfs) {
		populateExpressionDatumProperties(d, expressionConfs,
				new ExpressionRoot(d, data, service(getDatumService())));
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
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	public String getDisplayName() {
		return "Generic Modbus Device";
	}

	private static Map<ModbusReadFunction, List<ModbusPropertyConfig>> getReadFunctionSets(
			ModbusPropertyConfig[] configs) {
		if ( configs == null ) {
			return Collections.emptyMap();
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
				set.addRange(config.getAddress(), config.getAddress() + len - 1);
			}
		}
		return set;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = getIdentifiableSettingSpecifiers();

		results.add(0, new BasicTitleSettingSpecifier("sample", getSampleMessage(), true));

		results.addAll(getModbusNetworkSettingSpecifiers());

		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));
		results.add(new BasicTextFieldSettingSpecifier("maxReadWordCount",
				String.valueOf(DEFAULT_MAX_READ_WORD_COUNT)));

		// drop-down menu for word order
		BasicMultiValueSettingSpecifier wordOrderSpec = new BasicMultiValueSettingSpecifier(
				"wordOrderKey", String.valueOf(DEFAULT_WORD_ORDER.getKey()));
		Map<String, String> wordOrderTitles = new LinkedHashMap<String, String>(2);
		for ( ModbusWordOrder e : ModbusWordOrder.values() ) {
			wordOrderTitles.put(String.valueOf(e.getKey()), e.toDisplayString());
		}
		wordOrderSpec.setValueTitles(wordOrderTitles);
		results.add(wordOrderSpec);

		results.addAll(getSubSampleSettingSpecifiers());

		results.addAll(basicIdentifiableMetadataSettings(null, getMetadata()));

		ModbusPropertyConfig[] confs = getPropConfigs();
		List<ModbusPropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<ModbusPropertyConfig> emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier("propConfigs", confsList,
				new SettingUtils.KeyedListCallback<ModbusPropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(ModbusPropertyConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								ModbusPropertyConfig.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		Iterable<ExpressionService> exprServices = (getExpressionServices() != null
				? getExpressionServices().services()
				: null);
		if ( exprServices != null ) {
			ExpressionConfig[] exprConfs = getExpressionConfigs();
			List<ExpressionConfig> exprConfsList = (exprConfs != null ? Arrays.asList(exprConfs)
					: Collections.<ExpressionConfig> emptyList());
			results.add(SettingUtils.dynamicListSettingSpecifier("expressionConfigs", exprConfsList,
					new SettingUtils.KeyedListCallback<ExpressionConfig>() {

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

	private String getSampleMessage() {
		final long ts = sampleDate.get();
		if ( ts < 1 ) {
			return "N/A";
		}

		SimpleDatum d = SimpleDatum.nodeDatum(null);
		populateDatumProperties(d, propConfigs);

		Map<String, ?> data = d.getSampleData();
		if ( data == null || data.isEmpty() ) {
			return "No data.";
		}

		StringBuilder buf = new StringBuilder();
		buf.append(StringUtils.delimitedStringFromMap(data, "=", ", "));
		buf.append("; sampled at ").append(Instant.ofEpochMilli(ts));
		return buf.toString();
	}

	@Override
	public Void doWithConnection(final ModbusConnection conn) throws IOException {
		final int maxReadLen = maxReadWordCount;
		Map<ModbusReadFunction, List<ModbusPropertyConfig>> functionMap = getReadFunctionSets(
				propConfigs);
		IntRangeSet expressionRegisterSet = expressionRegisterSet();
		for ( Map.Entry<ModbusReadFunction, List<ModbusPropertyConfig>> me : functionMap.entrySet() ) {
			ModbusReadFunction function = me.getKey();
			ModbusRegisterBlockType blockType = function.blockType();

			List<ModbusPropertyConfig> configs = me.getValue();

			// try to read from device as few times as possible by combining ranges of addresses
			// into single calls, but limited to at most maxReadWordCount addresses at a time
			// because some devices have trouble returning large word counts
			IntRangeSet addressRangeSet = getRegisterAddressSet(configs);
			if ( function == ModbusReadFunction.ReadHoldingRegister ) {
				// add expressions
				expressionRegisterSet.forEachOrdered(new IntConsumer() {

					@Override
					public void accept(int value) {
						addressRangeSet.add(value);
					}
				});
				expressionRegisterSet.clear(); // so don't handle later
			}
			log.debug("Reading modbus {} register ranges: {}", getUnitId(), addressRangeSet);
			Iterable<IntRange> ranges = addressRangeSet.ranges();

			if ( blockType.isBitType() ) {
				data.performBitUpdates(function.blockType(), bits -> {
					boolean updated = false;
					for ( IntRange range : ranges ) {
						for ( int start = range.getMin(),
								stop = start + range.length(); start < stop; ) {
							int len = Math.min(range.length(), maxReadLen);
							int max = start + len;
							@SuppressWarnings("deprecation")
							BitSet updates = (blockType == ModbusRegisterBlockType.Coil
									? conn.readDiscreetValues(start, len)
									: conn.readInputDiscreteValues(start, len));
							for ( int i = start, u = 0; i < max; i++, u++ ) {
								bits.set(i, updates.get(u));
							}
							updated = true;
							start += len;
						}
					}
					return updated;
				});
			} else {
				data.performRegisterUpdates(function.blockType(), new ModbusDataUpdateAction() {

					@Override
					public boolean updateModbusData(MutableModbusData m) throws IOException {
						for ( IntRange range : ranges ) {
							for ( int start = range.getMin(),
									stop = start + range.length(); start < stop; ) {
								int len = Math.min(range.length(), maxReadLen);
								short[] data = conn.readWords(function, start, len);
								m.saveDataArray(data, start);
								start += len;
							}
						}
						return true;
					}
				});
			}
			// handle expression references, if not already handled
			if ( expressionRegisterSet != null && !expressionRegisterSet.isEmpty() ) {
				data.performRegisterUpdates(ModbusRegisterBlockType.Holding,
						new ModbusDataUpdateAction() {

							@Override
							public boolean updateModbusData(MutableModbusData m) throws IOException {
								Iterable<IntRange> exprRanges = expressionRegisterSet.ranges();
								for ( IntRange range : exprRanges ) {
									for ( int start = range.getMin(),
											stop = start + range.length(); start < stop; ) {
										int len = Math.min(range.length(), maxReadLen);
										short[] data = conn.readWords(
												ModbusReadFunction.ReadHoldingRegister, start, len);
										m.saveDataArray(data, start);
										start += len;
									}
								}
								return true;
							}
						});
			}
		}
		sampleDate.set(System.currentTimeMillis());
		return null;
	}

	private IntRangeSet expressionRegisterSet() {
		ExpressionConfig[] exprConfigs = getExpressionConfigs();
		IntRangeSet result = new IntRangeSet();
		if ( exprConfigs != null && exprConfigs.length > 0 ) {
			for ( ExpressionConfig config : exprConfigs ) {
				config.registerAddressReferences().forEachOrdered(new IntConsumer() {

					@Override
					public void accept(int value) {
						result.add(value);
					}
				});
			}
		}
		return result;
	}

	private synchronized boolean refreshDeviceData() {
		if ( !isCachedSampleExpired() ) {
			return true;
		}
		ModbusNetwork network = service(getModbusNetwork());
		if ( network == null ) {
			return false;
		}
		try {
			network.performAction(getUnitId(), this);
			return true;
		} catch ( IOException e ) {
			Throwable t = e;
			while ( t.getCause() != null ) {
				t = t.getCause();
			}
			String message;
			if ( t instanceof TimeoutException ) {
				message = "timeout";
			} else if ( t.getMessage() != null ) {
				message = t.getMessage();
			} else {
				message = t.toString();
			}
			log.debug("Error reading from Modbus device {}", modbusDeviceName(), t);
			log.error("Communication problem reading source {} from Modbus device {}: {}",
					resolvePlaceholders(this.sourceId), modbusDeviceName(), message);
			return false;
		}
	}

	private boolean isCachedSampleExpired() {
		final long ts = sampleDate.get();
		return ts + sampleCacheMs < System.currentTimeMillis();
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
	 * @param sampleCacheMs
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
	 * Get the maximum number of Modbus registers to read in any single read
	 * operation.
	 *
	 * @return the max read word count; defaults to
	 *         {@link #DEFAULT_MAX_READ_WORD_COUNT}
	 */
	public int getMaxReadWordCount() {
		return this.maxReadWordCount;
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
	 *        the maximum word count
	 */
	public void setMaxReadWordCount(int maxReadWordCount) {
		if ( maxReadWordCount < 1 ) {
			return;
		}
		this.maxReadWordCount = maxReadWordCount;
	}

	/**
	 * Get the source ID to use for returned datum.
	 *
	 * @return the source ID to use
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID to use for returned datum.
	 *
	 * @param sourceId
	 *        the source ID to use
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
		return data.getWordOrder();
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
		data.setWordOrder(wordOrder);
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
	 * Get the expression configurations.
	 *
	 * @return the expression configurations
	 * @since 1.5
	 */
	@Override
	public ExpressionConfig[] getExpressionConfigs() {
		return (ExpressionConfig[]) super.getExpressionConfigs();
	}

	/**
	 * Set the expression configurations to use.
	 *
	 * @param expressionConfigs
	 *        the configs to use
	 * @since 1.5
	 */
	public void setExpressionConfigs(ExpressionConfig[] expressionConfigs) {
		super.setExpressionConfigs(expressionConfigs);
	}

	/**
	 * Get the number of configured {@code expressionConfigs} elements.
	 *
	 * @return the number of {@code expressionConfigs} elements
	 * @since 1.5
	 */
	@Override
	public int getExpressionConfigsCount() {
		ExpressionConfig[] confs = getExpressionConfigs();
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
	@Override
	public void setExpressionConfigsCount(int count) {
		setExpressionConfigs(
				ArrayUtils.arrayWithLength(getExpressionConfigs(), count, ExpressionConfig.class, null));
	}

}

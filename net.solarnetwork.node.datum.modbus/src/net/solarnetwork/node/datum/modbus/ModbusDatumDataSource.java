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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import bak.pcj.set.IntRange;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Generic Modbus device datum data source.
 * 
 * @author matt
 * @version 1.1
 */
public class ModbusDatumDataSource extends ModbusDeviceDatumDataSourceSupport implements
		DatumDataSource<GeneralNodeDatum>, SettingSpecifierProvider, ModbusConnectionAction<ModbusData> {

	private String sourceId = "modbus";
	private long sampleCacheMs = 5000;
	private int maxReadWordCount = 64;
	private ModbusPropertyConfig[] propConfigs;

	private final ModbusData sample = new ModbusData();

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
					propVal = sample.getInt16(conf.getAddress());
					break;

				case Int32:
					propVal = sample.getInt32(conf.getAddress());
					break;

				case Int64:
					propVal = sample.getInt64(conf.getAddress());
					break;

				case SignedInt16:
					propVal = sample.getSignedInt16(conf.getAddress());
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

	private static Map<ModbusReadFunction, List<ModbusPropertyConfig>> getRegisterAddressSets(
			ModbusPropertyConfig[] configs) {
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
				Map<ModbusReadFunction, List<ModbusPropertyConfig>> functionMap = getRegisterAddressSets(
						propConfigs);
				for ( Map.Entry<ModbusReadFunction, List<ModbusPropertyConfig>> me : functionMap
						.entrySet() ) {
					ModbusReadFunction function = me.getKey();
					List<ModbusPropertyConfig> configs = me.getValue();
					// try to read from device as few times as possible by combining ranges of addresses
					// into single calls, but limited to at most maxReadWordCount addresses at a time
					// because some devices have trouble returning large word counts
					IntRangeSet addressRangeSet = getRegisterAddressSet(configs);
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
				return true;
			}
		});
		return sample.copy();
	}

	private ModbusData getCurrentSample() {
		ModbusData currSample;
		if ( isCachedSampleExpired() ) {
			try {
				currSample = performAction(this);
				if ( currSample != null && log.isTraceEnabled() ) {
					log.trace(currSample.dataDebugString());
				}
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from Modbus device " + modbusNetwork(), e);
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
	 * Get the property configurations to use.
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

}

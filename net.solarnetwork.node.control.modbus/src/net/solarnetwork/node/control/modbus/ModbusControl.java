/* ==================================================================
 * ModbusControl.java - 15/03/2018 5:34:09 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.modbus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.domain.NodeControlInfoDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.support.ModbusDeviceSupport;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * Read and write a Modbus "coil" or "holding" type register.
 * 
 * @author matt
 * @version 2.0
 */
public class ModbusControl extends ModbusDeviceSupport implements SettingSpecifierProvider,
		NodeControlProvider, InstructionHandler, ModbusConnectionAction<ModbusData> {

	/** The default value for the {@code address} property. */
	public static final int DEFAULT_ADDRESS = 0x0;

	/** The default value for the {@code controlId} property. */
	public static final String DEFAULT_CONTROL_ID = "/thermostat/temp/comfort";

	private long sampleCacheMs = 5000;
	private ModbusWritePropertyConfig[] propConfigs;
	private OptionalService<EventAdmin> eventAdmin;

	private final ModbusData sample = new ModbusData();

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		return null;
	}

	private NodeControlInfoDatum currentValue(ModbusWritePropertyConfig config) throws IOException {
		ModbusData currSample = getCurrentSample();
		Object value = extractControlValue(config, currSample);
		return newNodeControlInfoDatum(config, value);
	}

	private Object extractControlValue(ModbusWritePropertyConfig config, ModbusData currSample) {
		Object propVal = null;
		switch (config.getDataType()) {
			case Boolean:
				propVal = sample.getBoolean(config.getAddress());
				break;

			case Bytes:
				// can't set on control currently
				break;

			case Float32:
				propVal = sample.getFloat32(config.getAddress());
				break;

			case Float64:
				propVal = sample.getFloat64(config.getAddress());
				break;

			case Int16:
				propVal = sample.getInt16(config.getAddress());
				break;

			case UInt16:
				propVal = sample.getUnsignedInt16(config.getAddress());
				break;

			case Int32:
				propVal = sample.getInt32(config.getAddress());
				break;

			case UInt32:
				propVal = sample.getUnsignedInt32(config.getAddress());
				break;

			case Int64:
				propVal = sample.getInt64(config.getAddress());
				break;

			case UInt64:
				propVal = sample.getUnsignedInt64(config.getAddress());
				break;

			case StringAscii:
				propVal = sample.getAsciiString(config.getAddress(), config.getWordLength(), true);
				break;

			case StringUtf8:
				propVal = sample.getUtf8String(config.getAddress(), config.getWordLength(), true);
				break;
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

	private Number applyReverseUnitMultiplier(Number value, BigDecimal multiplier) {
		if ( BigDecimal.ONE.compareTo(multiplier) == 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		return v.divide(multiplier);
	}

	private ModbusData getCurrentSample() {
		ModbusData currSample = null;
		if ( sample.getDataTimestamp() + sampleCacheMs < System.currentTimeMillis() ) {
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
				log.warn("Communication problem reading from Modbus device {}: {}", modbusDeviceName(),
						t.getMessage());
			}
		} else {
			currSample = sample.copy();
		}
		return currSample;
	}

	/**
	 * Set the modbus register to a true/false value.
	 * 
	 * @param desiredValue
	 *        the desired value to set, which should have been returned from
	 *        {@link #controlValueForParameterValue(ModbusWritePropertyConfig, String)}
	 * @return {@literal true} if the write succeeded
	 * @throws IOException
	 *         if an IO error occurs
	 */
	private synchronized boolean setValue(final ModbusWritePropertyConfig config,
			final Object desiredValue) throws IOException {
		log.info("Setting {} value to {}", config.getControlId(), desiredValue);
		final ModbusWriteFunction function = config.getFunction();
		final Integer address = config.getAddress();

		return performAction(new ModbusConnectionAction<Boolean>() {

			@Override
			public Boolean doWithConnection(ModbusConnection conn) throws IOException {
				if ( function == ModbusWriteFunction.WriteCoil
						|| function == ModbusWriteFunction.WriteMultipleCoils ) {
					final BitSet bits = new BitSet(1);
					bits.set(0, desiredValue != null && ((Boolean) desiredValue).booleanValue());
					conn.writeDiscreetValues(new int[] { address }, bits);
					return true;
				}

				short[] dataToWrite = null;
				switch (config.getDataType()) {
					case StringAscii:
						conn.writeString(function, address,
								desiredValue != null ? desiredValue.toString() : "",
								ModbusDataUtils.ASCII_CHARSET);
						break;

					case StringUtf8:
						conn.writeString(function, address,
								desiredValue != null ? desiredValue.toString() : "",
								ModbusDataUtils.UTF8_CHARSET);
						break;

					case Bytes:
						dataToWrite = ModbusDataUtils.encodeBytes((byte[]) desiredValue);
						break;

					case Boolean:
						dataToWrite = new short[] {
								desiredValue != null && ((Boolean) desiredValue).booleanValue()
										? (short) 1
										: (short) 0 };
						break;

					case Float32:
					case Float64:
					case Int16:
					case UInt16:
					case Int32:
					case UInt32:
					case Int64:
					case UInt64: {
						Number normalizedValue = (desiredValue instanceof Number ? (Number) desiredValue
								: 0);
						if ( config.getUnitMultiplier() != null ) {
							normalizedValue = applyReverseUnitMultiplier(normalizedValue,
									config.getUnitMultiplier());
						}
						dataToWrite = ModbusDataUtils.encodeNumber(config.getDataType(),
								normalizedValue);
					}
						break;

				}
				if ( dataToWrite != null && dataToWrite.length > 0 ) {
					conn.writeWords(function, address, dataToWrite);
					return true;
				}
				return false;
			}
		});
	}

	private Object controlValueForParameterValue(ModbusWritePropertyConfig config, String str) {
		switch (config.getControlPropertyType()) {
			case Boolean:
				return StringUtils.parseBoolean(str);

			case Float:
			case Percent:
				if ( config.getDataType() == ModbusDataType.Float64 ) {
					return new BigDecimal(str).doubleValue();
				}
				return new BigDecimal(str).floatValue();

			case Integer:
				BigInteger bigInt = new BigInteger(str);
				switch (config.getDataType()) {
					case UInt64:
						return bigInt;

					case UInt32:
					case Int64:
						return bigInt.longValue();

					case UInt16:
					case Int32:
						return bigInt.intValue();

					default:
						return bigInt.shortValue();
				}

			case String:
				switch (config.getDataType()) {
					case StringAscii:
					case StringUtf8:
						return str;

					default:
						try {
							return str.getBytes("UTF-8");
						} catch ( UnsupportedEncodingException e ) {
							return new byte[0];
						}
				}

			default:
				// nothing to do

		}
		log.info("Unsupported property type {} for control {}); cannot extract value",
				config.getControlPropertyType(), config.getControlId());
		return null;
	}

	@Override
	public synchronized ModbusData doWithConnection(final ModbusConnection conn) throws IOException {
		ModbusWritePropertyConfig[] configs = getPropConfigs();
		if ( configs != null && configs.length > 0 ) {
			sample.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					for ( ModbusWritePropertyConfig config : configs ) {
						if ( !config.isValid() ) {
							continue;
						}
						ModbusReadFunction readFunction = (ModbusReadFunction) config.getFunction()
								.oppositeFunction();
						int start = config.getAddress();
						int len = config.getDataType().getWordLength();
						if ( len == -1 ) {
							len = config.getWordLength();
						}
						switch (readFunction) {
							case ReadCoil:
								m.saveDataArray(shortArrayForBitSet(conn.readDiscreetValues(start, len),
										start, len), start);
								break;

							case ReadDiscreteInput:
								m.saveDataArray(shortArrayForBitSet(
										conn.readInputDiscreteValues(start, len), start, len), start);
								break;

							case ReadHoldingRegister:
								m.saveDataArray(conn.readWordsUnsigned(
										ModbusReadFunction.ReadHoldingRegister, start, len), start);
								break;

							case ReadInputRegister:
								m.saveDataArray(conn.readWordsUnsigned(
										ModbusReadFunction.ReadInputRegister, start, len), start);
								break;
						}
					}
					return false;
				}
			});
		}
		return sample.copy();
	}

	private static short[] shortArrayForBitSet(BitSet set, int start, int count) {
		short[] result = new short[count];
		for ( int i = 0; i < count; i++ ) {
			result[i] = set.get(start + i) ? (short) 1 : (short) 0;
		}
		return result;
	}

	// NodeControlProvider

	@Override
	public List<String> getAvailableControlIds() {
		ModbusWritePropertyConfig[] configs = getPropConfigs();
		if ( configs == null || configs.length < 1 ) {
			return Collections.emptyList();
		}
		return Arrays.stream(configs).filter(ModbusWritePropertyConfig::isValid)
				.map(ModbusWritePropertyConfig::getControlId).collect(Collectors.toList());
	}

	private ModbusWritePropertyConfig configForControlId(String controlId) {
		ModbusWritePropertyConfig[] configs = getPropConfigs();
		if ( controlId == null || configs == null || configs.length < 1 ) {
			return null;
		}
		for ( ModbusWritePropertyConfig config : configs ) {
			if ( controlId.equals(config.getControlId()) ) {
				return config;
			}
		}
		return null;
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		ModbusWritePropertyConfig config = configForControlId(controlId);
		if ( config == null ) {
			return null;
		}
		// read the control's current status
		log.debug("Reading {} status", controlId);
		NodeControlInfoDatum result = null;
		try {
			result = currentValue(config);
		} catch ( Exception e ) {
			log.error("Error reading {} status: {}", controlId, e.getMessage());
		}
		if ( result != null ) {
			postControlEvent(result, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED);
		}
		return result;
	}

	private NodeControlInfoDatum newNodeControlInfoDatum(ModbusWritePropertyConfig config,
			Object value) {
		NodeControlInfoDatum info = new NodeControlInfoDatum();
		info.setCreated(new Date());
		info.setSourceId(config.getControlId());
		info.setType(config.getControlPropertyType());
		info.setReadonly(false);
		if ( value != null ) {
			info.setValue(value.toString());
		}
		return info;
	}

	private void postControlEvent(NodeControlInfoDatum info, String topic) {
		final EventAdmin admin = (eventAdmin != null ? eventAdmin.service() : null);
		if ( admin == null ) {
			return;
		}
		Map<String, ?> props = info.asSimpleMap();
		admin.postEvent(new Event(topic, props));
	}

	// InstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		ModbusWritePropertyConfig[] configs = getPropConfigs();
		if ( !InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(instruction.getTopic())
				|| configs == null || configs.length < 1 ) {
			return null;
		}
		// look for a parameter name that matches a control ID
		for ( String paramName : instruction.getParameterNames() ) {
			log.trace("Got instruction parameter {}", paramName);
			ModbusWritePropertyConfig config = configForControlId(paramName);
			if ( config == null || !config.isValid() ) {
				continue;
			}
			log.debug("Inspecting instruction {} against control {}", instruction.getId(),
					config.getControlId());
			// treat parameter value as a boolean String
			String str = instruction.getParameterValue(paramName);
			Object desiredValue = controlValueForParameterValue(config, str);
			boolean success = false;
			try {
				success = setValue(config, desiredValue);
			} catch ( Exception e ) {
				log.warn("Error handling instruction {} on control {}: {}", instruction.getTopic(),
						config.getControlId(), e.getMessage());
			}
			if ( success ) {
				sample.expire();
				postControlEvent(newNodeControlInfoDatum(config, desiredValue),
						NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
				return InstructionState.Completed;
			} else {
				return InstructionState.Declined;
			}
		}
		return null;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.modbus";
	}

	@Override
	public String getDisplayName() {
		return "Modbus Control";
	}

	private String getSampleMessage(ModbusData sample) {
		ModbusWritePropertyConfig[] configs = getPropConfigs();
		if ( sample.getDataTimestamp() < 1 || configs == null || configs.length < 1 ) {
			return "N/A";
		}

		Map<String, Object> data = new LinkedHashMap<>(configs.length);
		for ( ModbusWritePropertyConfig config : configs ) {
			if ( !config.isValid() ) {
				continue;
			}
			Object value = extractControlValue(config, sample);
			data.put(config.getControlId(), value != null ? value.toString() : "N/A");
		}
		if ( data.isEmpty() ) {
			return "N/A";
		}

		StringBuilder buf = new StringBuilder();
		buf.append(StringUtils.delimitedStringFromMap(data));
		buf.append("; sampled at ")
				.append(DateTimeFormat.forStyle("LS").print(new DateTime(sample.getDataTimestamp())));
		return buf.toString();
	}

	/**
	 * Get setting specifiers for the {@literal unitId} and
	 * {@literal modbusNetwork.propertyFilters['UID']} properties.
	 * 
	 * @return list of setting specifiers
	 * @since 1.1
	 */
	protected List<SettingSpecifier> getModbusNetworkSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(16);
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['UID']",
				"Modbus Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", "1"));
		return results;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		ModbusControl defaults = new ModbusControl();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);

		// get current value
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(sample.copy()), true));

		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.getUid()));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.getGroupUID()));
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['UID']",
				"Modbus Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(defaults.getUnitId())));

		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));

		ModbusWritePropertyConfig[] confs = getPropConfigs();
		List<ModbusWritePropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<ModbusWritePropertyConfig> emptyList());
		results.add(SettingsUtil.dynamicListSettingSpecifier("propConfigs", confsList,
				new SettingsUtil.KeyedListCallback<ModbusWritePropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(
							ModbusWritePropertyConfig value, int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								ModbusWritePropertyConfig.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return results;
	}

	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
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
	public ModbusWritePropertyConfig[] getPropConfigs() {
		return propConfigs;
	}

	/**
	 * Get the property configurations to use.
	 * 
	 * @param propConfigs
	 *        the configs to use
	 */
	public void setPropConfigs(ModbusWritePropertyConfig[] propConfigs) {
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 * 
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropConfigsCount() {
		ModbusWritePropertyConfig[] confs = this.propConfigs;
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
				ModbusWritePropertyConfig.class, null);
	}

}

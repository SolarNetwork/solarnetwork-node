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

import static net.solarnetwork.util.DateUtils.formatForLocalDisplay;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.util.stream.Collectors;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.io.modbus.support.ModbusDataDeviceSupport;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.ByteUtils;
import net.solarnetwork.util.Half;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Read and write a Modbus "coil" or "holding" type register.
 * 
 * @author matt
 * @version 3.2
 */
public class ModbusControl extends ModbusDataDeviceSupport<ModbusData>
		implements SettingSpecifierProvider, NodeControlProvider, InstructionHandler {

	/** The default value for the {@code address} property. */
	public static final int DEFAULT_ADDRESS = 0x0;

	/** The default value for the {@code controlId} property. */
	public static final String DEFAULT_CONTROL_ID = "/thermostat/temp/comfort";

	/**
	 * The setting UID used by this service.
	 * 
	 * @since 3.1
	 */
	public static final String SETTING_UID = "net.solarnetwork.node.control.modbus";

	/**
	 * The {@code maxReadWordCount} property default value.
	 * 
	 * @since 3.2
	 */
	public static final int DEFAULT_MAX_READ_WORD_COUNT = 64;

	private ModbusWritePropertyConfig[] propConfigs;
	private OptionalService<EventAdmin> eventAdmin;
	private int maxReadWordCount;

	/**
	 * Constructor.
	 */
	public ModbusControl() {
		super(new ModbusData());
		maxReadWordCount = DEFAULT_MAX_READ_WORD_COUNT;
	}

	private SimpleNodeControlInfoDatum currentValue(ModbusWritePropertyConfig config)
			throws IOException {
		ModbusData currSample = getCurrentSample();
		Object value = extractControlValue(config, currSample);
		return newSimpleNodeControlInfoDatum(config, value);
	}

	private Object extractControlValue(ModbusWritePropertyConfig config, ModbusData currSample) {
		Object propVal = null;
		switch (config.getDataType()) {
			case Boolean:
				propVal = currSample.getBoolean(config.getAddress());
				break;

			case Bytes:
				// can't set on control currently
				break;

			case Float16:
				propVal = currSample.getFloat16(config.getAddress());
				break;

			case Float32:
				propVal = currSample.getFloat32(config.getAddress());
				break;

			case Float64:
				propVal = currSample.getFloat64(config.getAddress());
				break;

			case Int16:
				propVal = currSample.getInt16(config.getAddress());
				break;

			case UInt16:
				propVal = currSample.getUnsignedInt16(config.getAddress());
				break;

			case Int32:
				propVal = currSample.getInt32(config.getAddress());
				break;

			case UInt32:
				propVal = currSample.getUnsignedInt32(config.getAddress());
				break;

			case Int64:
				propVal = currSample.getInt64(config.getAddress());
				break;

			case UInt64:
				propVal = currSample.getUnsignedInt64(config.getAddress());
				break;

			case StringAscii:
				propVal = currSample.getAsciiString(config.getAddress(), config.getWordLength(), true);
				break;

			case StringUtf8:
				propVal = currSample.getUtf8String(config.getAddress(), config.getWordLength(), true);
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

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, ModbusData sample) throws IOException {
		// nothing to do		
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
								desiredValue != null ? desiredValue.toString() : "", ByteUtils.ASCII);
						break;

					case StringUtf8:
						conn.writeString(function, address,
								desiredValue != null ? desiredValue.toString() : "", ByteUtils.UTF8);
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

					case Float16:
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
				switch (config.getDataType()) {
					case Float16:
						return Half.valueOf(str);

					case Float64:
						return new BigDecimal(str).doubleValue();

					default:
						return new BigDecimal(str).floatValue();
				}

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
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		return null;
	}

	private static Map<ModbusReadFunction, List<ModbusWritePropertyConfig>> getReadFunctionSets(
			ModbusWritePropertyConfig[] configs) {
		if ( configs == null ) {
			return Collections.emptyMap();
		}
		Map<ModbusReadFunction, List<ModbusWritePropertyConfig>> confsByFunction = new LinkedHashMap<>(
				configs.length);
		for ( ModbusWritePropertyConfig config : configs ) {
			if ( !config.isValid() ) {
				continue;
			}
			confsByFunction.computeIfAbsent((ModbusReadFunction) config.getFunction().oppositeFunction(),
					k -> new ArrayList<>(4)).add(config);
		}
		return confsByFunction;
	}

	private static IntRangeSet getRegisterAddressSet(List<ModbusWritePropertyConfig> configs) {
		IntRangeSet set = new IntRangeSet();
		if ( configs != null ) {
			for ( ModbusWritePropertyConfig config : configs ) {
				int len = config.getDataType().getWordLength();
				if ( len == -1 ) {
					len = config.getWordLength();
				}
				set.addRange(config.getAddress(), config.getAddress() + len - 1);
			}
		}
		return set;
	}

	private static short[] shortArrayForBitSet(BitSet set, int start, int count) {
		short[] result = new short[count];
		for ( int i = 0; i < count; i++ ) {
			result[i] = set.get(start + i) ? (short) 1 : (short) 0;
		}
		return result;
	}

	@Override
	protected synchronized void refreshDeviceData(ModbusConnection conn, ModbusData sample)
			throws IOException {
		ModbusWritePropertyConfig[] configs = getPropConfigs();
		if ( configs != null && configs.length > 0 ) {

			sample.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) throws IOException {
					final int maxReadLen = maxReadWordCount;
					Map<ModbusReadFunction, List<ModbusWritePropertyConfig>> functionMap = getReadFunctionSets(
							propConfigs);
					for ( Map.Entry<ModbusReadFunction, List<ModbusWritePropertyConfig>> me : functionMap
							.entrySet() ) {
						ModbusReadFunction function = me.getKey();
						List<ModbusWritePropertyConfig> configs = me.getValue();
						// try to read from device as few times as possible by combining ranges of addresses
						// into single calls, but limited to at most maxReadWordCount addresses at a time
						// because some devices have trouble returning large word counts
						IntRangeSet addressRangeSet = getRegisterAddressSet(configs);
						log.debug("Reading modbus {} register ranges: {}", getUnitId(), addressRangeSet);
						Iterable<IntRange> ranges = addressRangeSet.ranges();
						for ( IntRange range : ranges ) {
							for ( int start = range.getMin(),
									stop = start + range.length(); start < stop; ) {
								int len = Math.min(range.length(), maxReadLen);
								switch (function) {
									case ReadCoil:
										m.saveDataArray(shortArrayForBitSet(
												conn.readDiscreetValues(start, len), start, len), start);
										break;

									case ReadDiscreteInput:
										m.saveDataArray(shortArrayForBitSet(
												conn.readInputDiscreteValues(start, len), start, len),
												start);
										break;

									case ReadHoldingRegister:
										m.saveDataArray(
												conn.readWords(ModbusReadFunction.ReadHoldingRegister,
														start, len),
												start);
										break;

									case ReadInputRegister:
										m.saveDataArray(
												conn.readWords(ModbusReadFunction.ReadInputRegister,
														start, len),
												start);
										break;
								}
								start += len;
							}
						}
					}
					return true;
				}
			});
		}
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
		SimpleNodeControlInfoDatum result = null;
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

	private SimpleNodeControlInfoDatum newSimpleNodeControlInfoDatum(ModbusWritePropertyConfig config,
			Object value) {
		// @formatter:off
		NodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(resolvePlaceholders(config.getControlId()))
				.withType(config.getControlPropertyType())
				.withReadonly(false)
				.withValue(value != null ? value.toString() : null)
				.build();
		// @formatter:on
		return new SimpleNodeControlInfoDatum(info, Instant.now());
	}

	private void postControlEvent(SimpleNodeControlInfoDatum info, String topic) {
		final EventAdmin admin = (eventAdmin != null ? eventAdmin.service() : null);
		if ( admin == null ) {
			return;
		}
		Event event = DatumEvents.datumEvent(topic, info);
		admin.postEvent(event);
	}

	// InstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
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
				getSample().expire();
				postControlEvent(newSimpleNodeControlInfoDatum(config, desiredValue),
						NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
				return InstructionUtils.createStatus(instruction, InstructionState.Completed);
			}
			return InstructionUtils.createStatus(instruction, InstructionState.Declined);
		}
		return null;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	public String getDisplayName() {
		return "Modbus Control";
	}

	private String getSampleMessage(ModbusData sample) {
		ModbusWritePropertyConfig[] configs = getPropConfigs();
		if ( sample.getDataTimestamp() == null || configs == null || configs.length < 1 ) {
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
		buf.append("; sampled at ").append(formatForLocalDisplay(sample.getDataTimestamp()));
		return buf.toString();
	}

	/**
	 * Get setting specifiers for the {@literal unitId} and
	 * {@literal modbusNetwork.propertyFilters['uid']} properties.
	 * 
	 * @return list of setting specifiers
	 * @since 1.1
	 */
	protected List<SettingSpecifier> getModbusNetworkSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(16);
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['uid']",
				"Modbus Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", "1"));
		return results;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		ModbusControl defaults = new ModbusControl();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);

		// get current value
		results.add(
				new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample().copy()), true));

		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.getUid()));
		results.add(new BasicTextFieldSettingSpecifier("groupUid", defaults.getGroupUid()));
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['uid']",
				"Modbus Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(defaults.getUnitId())));

		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("maxReadWordCount",
				String.valueOf(DEFAULT_MAX_READ_WORD_COUNT)));

		ModbusWritePropertyConfig[] confs = getPropConfigs();
		List<ModbusWritePropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<ModbusWritePropertyConfig> emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier("propConfigs", confsList,
				new SettingUtils.KeyedListCallback<ModbusWritePropertyConfig>() {

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

	/**
	 * Get the event admin service.
	 * 
	 * @return the event admin
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set the event admin sevice.
	 * 
	 * @param eventAdmin
	 *        the service to set
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
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
	 * {@link ModbusWritePropertyConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code propConfigs} elements.
	 */
	public void setPropConfigsCount(int count) {
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, count,
				ModbusWritePropertyConfig.class, null);
	}

	/**
	 * Get the maximum number of Modbus registers to read in any single read
	 * operation.
	 * 
	 * @return the max read word count; defaults to
	 *         {@link #DEFAULT_MAX_READ_WORD_COUNT}
	 * @since 3.2
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
	 * @since 3.2
	 */
	public void setMaxReadWordCount(int maxReadWordCount) {
		if ( maxReadWordCount < 1 ) {
			return;
		}
		this.maxReadWordCount = maxReadWordCount;
	}

}

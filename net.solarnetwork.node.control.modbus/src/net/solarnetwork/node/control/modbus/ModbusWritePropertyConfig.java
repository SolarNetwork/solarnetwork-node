/* ==================================================================
 * ModbusWritePropertyConfig.java - 20/12/2017 1:32:48 PM
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

package net.solarnetwork.node.control.modbus;

import static java.lang.String.format;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a single control property to be set via Modbus.
 *
 * @author matt
 * @version 2.2
 */
public class ModbusWritePropertyConfig {

	/** The default value for the {@code address} property. */
	public static final int DEFAULT_ADDRESS = 0x0;

	/** The default value for the {@code controlPropertyType} property. */
	public static final NodeControlPropertyType DEFAULT_CONTROL_PROPERTY_TYPE = NodeControlPropertyType.Boolean;

	/** The {@code dataType} property default value */
	public static final ModbusDataType DEFAULT_DATA_TYPE = ModbusDataType.Boolean;

	/** The {@code function} property default value */
	public static final ModbusWriteFunction DEFAULT_FUNCTION = ModbusWriteFunction.WriteCoil;

	/** The {@code unitMultiplier} property default value. */
	public static final BigDecimal DEFAULT_UNIT_MULTIPLIER = BigDecimal.ONE;

	/** The {@code wordLength} property default value. */
	public static final int DEFAULT_WORD_LENGTH = 0;

	/** The {@code decimalScale} property default value. */
	public static final int DEFAULT_DECIMAL_SCALE = 0;

	/**
	 * A setting type pattern for a property configuration element.
	 *
	 * <p>
	 * The pattern has two capture groups: the property configuration index and
	 * the property setting name.
	 * </p>
	 *
	 * @since 2.1
	 */
	public static final Pattern PROP_SETTING_PATTERN = Pattern
			.compile(Pattern.quote("propConfigs[").concat("(\\d+)\\]\\.(.*)"));

	private Integer address;
	private NodeControlPropertyType controlPropertyType;
	private String controlId;
	private ModbusWriteFunction function;
	private ModbusDataType dataType;
	private Integer wordLength;
	private BigDecimal unitMultiplier;
	private Integer decimalScale;

	/**
	 * Default constructor.
	 */
	public ModbusWritePropertyConfig() {
		this(null, DEFAULT_CONTROL_PROPERTY_TYPE, DEFAULT_DATA_TYPE, DEFAULT_ADDRESS);
	}

	/**
	 * Construct with values.
	 *
	 * @param controlId
	 *        the control ID to use
	 * @param controlPropertyType
	 *        the control property type
	 * @param dataType
	 *        the modbus data type
	 * @param address
	 *        the modbus register address
	 */
	public ModbusWritePropertyConfig(String controlId, NodeControlPropertyType controlPropertyType,
			ModbusDataType dataType, int address) {
		this(controlId, controlPropertyType, dataType, address, DEFAULT_WORD_LENGTH,
				DEFAULT_UNIT_MULTIPLIER, DEFAULT_DECIMAL_SCALE);
	}

	/**
	 * Construct with values.
	 *
	 * @param controlId
	 *        the control ID to use
	 * @param controlPropertyType
	 *        the control property type
	 * @param dataType
	 *        the modbus data type
	 * @param address
	 *        the modbus register address
	 * @param unitMultiplier
	 *        the unit multiplier
	 */
	public ModbusWritePropertyConfig(String controlId, NodeControlPropertyType controlPropertyType,
			ModbusDataType dataType, int address, BigDecimal unitMultiplier) {
		this(controlId, controlPropertyType, dataType, address, DEFAULT_WORD_LENGTH, unitMultiplier,
				DEFAULT_DECIMAL_SCALE);
	}

	/**
	 * Construct with values.
	 *
	 * @param controlId
	 *        the control ID to use
	 * @param controlPropertyType
	 *        the control property type
	 * @param dataType
	 *        the modbus data type
	 * @param address
	 *        the modbus register address
	 * @param unitMultiplier
	 *        the unit multiplier
	 * @param decimalScale
	 *        for numbers, the maximum decimal scale to support, or
	 *        {@literal -1} for no limit
	 */
	public ModbusWritePropertyConfig(String controlId, NodeControlPropertyType controlPropertyType,
			ModbusDataType dataType, int address, BigDecimal unitMultiplier, int decimalScale) {
		this(controlId, controlPropertyType, dataType, address, DEFAULT_WORD_LENGTH, unitMultiplier,
				decimalScale);
	}

	/**
	 * Construct with values.
	 *
	 * @param controlId
	 *        the control ID to use
	 * @param controlPropertyType
	 *        the control property type
	 * @param dataType
	 *        the modbus data type
	 * @param address
	 *        the modbus register address
	 * @param wordLength
	 *        the modbus word length to read
	 * @param unitMultiplier
	 *        the unit multiplier
	 * @param decimalScale
	 *        for numbers, the maximum decimal scale to support, or
	 *        {@literal -1} for no limit
	 */
	public ModbusWritePropertyConfig(String controlId, NodeControlPropertyType controlPropertyType,
			ModbusDataType dataType, int address, int wordLength, BigDecimal unitMultiplier,
			int decimalScale) {
		this.controlId = controlId;
		this.controlPropertyType = controlPropertyType;
		this.address = address;
		this.function = DEFAULT_FUNCTION;
		this.dataType = dataType;
		this.wordLength = wordLength;
		this.unitMultiplier = unitMultiplier;
		this.decimalScale = decimalScale;
	}

	/**
	 * Test if this configuration is empty.
	 *
	 * @return {@literal true} if all properties are null
	 * @since 2.1
	 */
	public boolean isEmpty() {
		return (dataType == null && decimalScale == null && function == null && unitMultiplier == null
				&& wordLength == null && controlPropertyType == null && address == null);
	}

	/**
	 * Test if this instance has a valid configuration.
	 *
	 * <p>
	 * This method simply verifies the minimum level of configuration is
	 * available for the control to be used.
	 * </p>
	 *
	 * @return {@literal true} if this configuration is valid for use
	 */
	public boolean isValid() {
		return this.controlId != null && controlId.trim().length() > 0 && controlPropertyType != null
				&& dataType != null && function != null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ModbusWritePropertyConfig{address=");
		builder.append(address);
		builder.append(", ");
		if ( controlPropertyType != null ) {
			builder.append("controlPropertyType=");
			builder.append(controlPropertyType);
			builder.append(", ");
		}
		if ( controlId != null ) {
			builder.append("controlId=");
			builder.append(controlId);
			builder.append(", ");
		}
		if ( function != null ) {
			builder.append("function=");
			builder.append(function);
			builder.append(", ");
		}
		if ( dataType != null ) {
			builder.append("dataType=");
			builder.append(dataType);
			builder.append(", ");
		}
		builder.append("wordLength=");
		builder.append(wordLength);
		builder.append(", ");
		if ( unitMultiplier != null ) {
			builder.append("unitMultiplier=");
			builder.append(unitMultiplier);
			builder.append(", ");
		}
		builder.append("decimalScale=");
		builder.append(decimalScale);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Generate a list of setting values.
	 *
	 * @param providerId
	 *        the setting provider ID
	 * @param instanceId
	 *        the factory instance ID
	 * @param i
	 *        the property index
	 * @return the settings
	 * @since 2.1
	 */
	public List<SettingValueBean> toSettingValues(String providerId, String instanceId, int i) {
		List<SettingValueBean> settings = new ArrayList<>(8);
		addSetting(settings, providerId, instanceId, i, "controlId", getControlId());
		addSetting(settings, providerId, instanceId, i, "controlPropertyTypeKey",
				getControlPropertyTypeKey());
		addSetting(settings, providerId, instanceId, i, "address", getAddress());
		addSetting(settings, providerId, instanceId, i, "functionCode", getFunctionCode());
		addSetting(settings, providerId, instanceId, i, "dataTypeKey", getDataTypeKey());
		addSetting(settings, providerId, instanceId, i, "wordLength", getWordLength());
		addSetting(settings, providerId, instanceId, i, "unitMultiplier", getUnitMultiplier());
		addSetting(settings, providerId, instanceId, i, "decimalScale", getDecimalScale());
		return settings;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId, String instanceId,
			int i, String key, Object val) {
		if ( val == null ) {
			return;
		}
		settings.add(new SettingValueBean(providerId, instanceId, format("propConfigs[%d].%s", i, key),
				val.toString()));
	}

	/**
	 * Populate a setting as a property configuration value, if possible.
	 *
	 * @param config
	 *        the overall configuration
	 * @param setting
	 *        the setting to try to handle
	 * @return {@literal true} if the setting was handled as a property
	 *         configuration value
	 * @since 2.1
	 */
	public static boolean populateFromSetting(ModbusControlConfig config, Setting setting) {
		Matcher m = PROP_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		int idx = Integer.parseInt(m.group(1));
		String name = m.group(2);
		List<ModbusWritePropertyConfig> propConfigs = config.getPropertyConfigs();
		if ( !(idx < propConfigs.size()) ) {
			propConfigs.add(new ModbusWritePropertyConfig());
		}
		ModbusWritePropertyConfig propConfig = propConfigs.get(idx);
		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (name) {
				case "controlId":
					propConfig.setControlId(val);
					break;
				case "controlPropertyTypeKey":
					propConfig.setControlPropertyTypeKey(val);
					break;
				case "address":
					propConfig.setAddress(Integer.valueOf(val));
					break;
				case "functionCode":
					propConfig.setFunctionCode(val);
					break;
				case "dataTypeKey":
					propConfig.setDataTypeKey(val);
					break;
				case "wordLength":
					propConfig.setWordLength(Integer.valueOf(val));
					break;
				case "unitMultiplier":
					propConfig.setUnitMultiplier(new BigDecimal(val));
					break;
				case "decimalScale":
					propConfig.setDecimalScale(Integer.valueOf(val));
					break;
				default:
					// ignore
			}
		}
		return true;
	}

	/**
	 * Get the settings to configure an instance of this class.
	 *
	 * @param prefix
	 *        the settings prefix to use
	 * @return the settings
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();

		ModbusWritePropertyConfig defaults = new ModbusWritePropertyConfig();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "controlId", ""));

		// drop-down menu for controlPropertyType
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "controlPropertyTypeKey", defaults.getControlPropertyTypeKey());
		Map<String, String> propTypeTitles = new LinkedHashMap<String, String>(3);
		for ( NodeControlPropertyType e : NodeControlPropertyType.values() ) {
			propTypeTitles.put(Character.toString(e.getKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "address",
				String.valueOf(defaults.getAddress())));

		// drop-down menu for function
		BasicMultiValueSettingSpecifier functionSpec = new BasicMultiValueSettingSpecifier(
				prefix + "functionCode", defaults.getFunctionCode());
		Map<String, String> functionTitles = new LinkedHashMap<String, String>(4);
		for ( ModbusWriteFunction e : ModbusWriteFunction.values() ) {
			functionTitles.put(String.valueOf(e.getCode()), e.toDisplayString());
		}
		functionSpec.setValueTitles(functionTitles);
		results.add(functionSpec);

		// drop-down menu for dataType
		BasicMultiValueSettingSpecifier dataTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "dataTypeKey", defaults.getDataTypeKey());
		Map<String, String> dataTypeTitles = new LinkedHashMap<String, String>(3);
		for ( ModbusDataType e : ModbusDataType.values() ) {
			dataTypeTitles.put(e.getKey(), e.toDisplayString());
		}
		dataTypeSpec.setValueTitles(dataTypeTitles);
		results.add(dataTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "wordLength", "1"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "unitMultiplier", "1"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale", "0"));
		return results;
	}

	/**
	 * Get the control ID.
	 *
	 * @return the control ID
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 *
	 * @param controlId
	 *        the control ID to set
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Get the control data type.
	 *
	 * @return the control property type
	 */
	public NodeControlPropertyType getControlPropertyType() {
		return controlPropertyType;
	}

	/**
	 * Set the control property type.
	 *
	 * @param controlPropertyType
	 *        the control property type
	 */
	public void setControlPropertyType(NodeControlPropertyType controlPropertyType) {
		if ( controlPropertyType == null ) {
			controlPropertyType = NodeControlPropertyType.Boolean;
		}
		this.controlPropertyType = controlPropertyType;
	}

	/**
	 * Get the control property type key.
	 *
	 * <p>
	 * This returns the configured {@link #getControlPropertyType()}
	 * {@link NodeControlPropertyType#getKey()} value as a string.
	 * </p>
	 *
	 * @return the property type key
	 */
	public String getControlPropertyTypeKey() {
		NodeControlPropertyType type = getControlPropertyType();
		if ( type == null ) {
			return null;
		}
		return Character.toString(type.getKey());
	}

	/**
	 * Set the datum property type via a key value.
	 *
	 * <p>
	 * This uses the first character of {@code key} as a
	 * {@link NodeControlPropertyType} key value to call
	 * {@link #setControlPropertyType(NodeControlPropertyType)}. If {@code key}
	 * is not recognized, then {@link #DEFAULT_CONTROL_PROPERTY_TYPE} will be
	 * set instead.
	 * </p>
	 *
	 * @param key
	 *        the datum property type key to set
	 */
	public void setControlPropertyTypeKey(String key) {
		if ( key == null || key.length() < 1 ) {
			return;
		}
		NodeControlPropertyType type;
		try {
			type = NodeControlPropertyType.forKey(key.charAt(0));
		} catch ( IllegalArgumentException e ) {
			type = DEFAULT_CONTROL_PROPERTY_TYPE;
		}
		setControlPropertyType(type);
	}

	/**
	 * Get the Modbus function to use.
	 *
	 * @return the Modbus function
	 */
	public ModbusWriteFunction getFunction() {
		return function;
	}

	/**
	 * Set the Modbus function to use.
	 *
	 * @param function
	 *        the Modbus function
	 */
	public void setFunction(ModbusWriteFunction function) {
		if ( function == null ) {
			return;
		}
		this.function = function;
	}

	/**
	 * Get the Modbus function code to use as a string.
	 *
	 * @return the Modbus function code as a string
	 */
	public String getFunctionCode() {
		return String.valueOf(function.getCode());
	}

	/**
	 * Set the Modbus function to use as a string.
	 *
	 * @param function
	 *        the Modbus function
	 */
	public void setFunctionCode(String function) {
		if ( function == null ) {
			return;
		}
		setFunction(ModbusWriteFunction.forCode(Integer.parseInt(function)));
	}

	/**
	 * Get the data type.
	 *
	 * @return the type
	 */
	public ModbusDataType getDataType() {
		return dataType;
	}

	/**
	 * Set the data type.
	 *
	 * @param dataType
	 *        the type to set
	 */
	public void setDataType(ModbusDataType dataType) {
		if ( dataType == null ) {
			return;
		}
		this.dataType = dataType;
	}

	/**
	 * Get the data type as a key value.
	 *
	 * @return the type as a key
	 */
	public String getDataTypeKey() {
		ModbusDataType type = getDataType();
		return (type != null ? type.getKey() : null);
	}

	/**
	 * Set the data type as a string value.
	 *
	 * @param key
	 *        the type to set
	 */
	public void setDataTypeKey(String key) {
		setDataType(ModbusDataType.forKey(key));
	}

	/**
	 * Get the number of Modbus registers to read.
	 *
	 * <p>
	 * This is only used for data types of unknown length, like strings.
	 * </p>
	 *
	 * @return the register count to read
	 */
	public Integer getWordLength() {
		return wordLength;
	}

	/**
	 * Set the number of Modbus registers to read.
	 *
	 * <p>
	 * This is only used for data types of unknown length, like strings.
	 * </p>
	 *
	 * @param wordLength
	 *        the register count to read
	 */
	public void setWordLength(Integer wordLength) {
		this.wordLength = wordLength;
	}

	/**
	 * Get the register address to start writing data to.
	 *
	 * @return the register address
	 */
	public Integer getAddress() {
		return address;
	}

	/**
	 * Set the register address to start writing data to.
	 *
	 * @param address
	 *        the register address to set
	 */
	public void setAddress(Integer address) {
		this.address = address;
	}

	/**
	 * Get the unit multiplier.
	 *
	 * @return the multiplier
	 */
	public BigDecimal getUnitMultiplier() {
		return unitMultiplier;
	}

	/**
	 * Set the unit multiplier.
	 *
	 * <p>
	 * This value represents a multiplication factor to apply to values
	 * collected for this property so that a standardized unit is captured. For
	 * example, a power meter might report power as <i>killowatts</i>, in which
	 * case {@code multiplier} can be configured as {@literal .001} to convert
	 * the value to <i>watts</i>.
	 * </p>
	 *
	 * @param unitMultiplier
	 *        the mutliplier to set
	 */
	public void setUnitMultiplier(BigDecimal unitMultiplier) {
		this.unitMultiplier = unitMultiplier;
	}

	/**
	 * Get the decimal scale to round decimal numbers to.
	 *
	 * @return the decimal scale
	 */
	public Integer getDecimalScale() {
		return decimalScale;
	}

	/**
	 * Set the decimal scale to round decimal numbers to.
	 *
	 * <p>
	 * This is a <i>maximum</i> scale value that decimal values should be
	 * rounded to. This is applied <i>after</i> any {@code unitMultiplier} is
	 * applied. A scale of {@literal 0} would round all decimals to integer
	 * values.
	 * </p>
	 *
	 * @param decimalScale
	 *        the scale to set, or {@literal -1} to disable rounding completely
	 */
	public void setDecimalScale(Integer decimalScale) {
		this.decimalScale = decimalScale;
	}

}

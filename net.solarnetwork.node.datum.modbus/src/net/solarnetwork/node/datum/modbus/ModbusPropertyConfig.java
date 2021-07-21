/* ==================================================================
 * ModbusPropertyConfig.java - 20/12/2017 1:32:48 PM
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.GeneralDatumSamplePropertyConfig;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a single datum property to be set via Modbus.
 * 
 * <p>
 * The {@link #getConfig()} value represents the modbus address to read from.
 * </p>
 * 
 * @author matt
 * @version 1.3
 */
public class ModbusPropertyConfig extends GeneralDatumSamplePropertyConfig<Integer> {

	private ModbusReadFunction function;
	private ModbusDataType dataType;
	private Integer wordLength;
	private BigDecimal unitMultiplier;
	private Integer decimalScale;

	/**
	 * Default constructor.
	 */
	public ModbusPropertyConfig() {
		super(null, GeneralDatumSamplesType.Instantaneous, 0);
		dataType = ModbusDataType.Float32;
		function = ModbusReadFunction.ReadHoldingRegister;
		setWordLength(1);
		setUnitMultiplier(BigDecimal.ONE);
		setDecimalScale(0);
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param datumPropertyType
	 *        the datum property type
	 * @param dataType
	 *        the modbus data type
	 * @param address
	 *        the modbus register address
	 */
	public ModbusPropertyConfig(String name, GeneralDatumSamplesType datumPropertyType,
			ModbusDataType dataType, int address) {
		this(name, datumPropertyType, dataType, address, 0, BigDecimal.ONE, 0);
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param datumPropertyType
	 *        the datum property type
	 * @param dataType
	 *        the modbus data type
	 * @param address
	 *        the modbus register address
	 * @param unitMultiplier
	 *        the unit multiplier
	 */
	public ModbusPropertyConfig(String name, GeneralDatumSamplesType datumPropertyType,
			ModbusDataType dataType, int address, BigDecimal unitMultiplier) {
		this(name, datumPropertyType, dataType, address, 0, unitMultiplier, 0);
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param datumPropertyType
	 *        the datum property type
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
	public ModbusPropertyConfig(String name, GeneralDatumSamplesType datumPropertyType,
			ModbusDataType dataType, int address, BigDecimal unitMultiplier, int decimalScale) {
		this(name, datumPropertyType, dataType, address, 0, unitMultiplier, decimalScale);
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param datumPropertyType
	 *        the datum property type
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
	public ModbusPropertyConfig(String name, GeneralDatumSamplesType datumPropertyType,
			ModbusDataType dataType, int address, int wordLength, BigDecimal unitMultiplier,
			int decimalScale) {
		super(name, datumPropertyType, address);
		this.function = ModbusReadFunction.ReadHoldingRegister;
		this.dataType = dataType;
		setWordLength(wordLength);
		setUnitMultiplier(unitMultiplier);
		setDecimalScale(decimalScale);
	}

	/**
	 * Test if this configuration appears to be vaild.
	 * 
	 * @return {@literal true} if the configuration has all necessary properties
	 *         configured
	 */
	public boolean isValid() {
		Integer address = getAddress();
		String propName = getName();
		return (address != null && address.intValue() >= 0 && propName != null && !propName.isEmpty()
				&& function != null && dataType != null);
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		ModbusPropertyConfig defaults = new ModbusPropertyConfig();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "name", ""));

		// drop-down menu for datumPropertyType
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "datumPropertyTypeKey", defaults.getDatumPropertyTypeValue());
		Map<String, String> propTypeTitles = new LinkedHashMap<String, String>(3);
		for ( GeneralDatumSamplesType e : GeneralDatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "address",
				String.valueOf(defaults.getAddress())));

		// drop-down menu for function
		BasicMultiValueSettingSpecifier functionSpec = new BasicMultiValueSettingSpecifier(
				prefix + "functionCode", defaults.getFunctionCode());
		Map<String, String> functionTitles = new LinkedHashMap<String, String>(4);
		for ( ModbusReadFunction e : ModbusReadFunction.values() ) {
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
	 * Get the datum property name used for this configuration.
	 * 
	 * <p>
	 * This is an alias for {@link #getPropertyKey()}.
	 * </p>
	 * 
	 * @return the property name
	 */
	public String getName() {
		return getPropertyKey();
	}

	/**
	 * Set the datum property name to use.
	 * 
	 * <p>
	 * This is an alias for {@link #setPropertyKey(String)}.
	 * </p>
	 * 
	 * @param name
	 *        the property name
	 */
	public void setName(String name) {
		setPropertyKey(name);
	}

	/**
	 * Get the datum property type key.
	 * 
	 * <p>
	 * This is an alias for {@link #getPropertyTypeKey()}.
	 * </p>
	 * 
	 * @return the property type key
	 */
	public String getDatumPropertyTypeValue() {
		return getPropertyTypeKey();
	}

	/**
	 * Set the datum property type via a key value.
	 * 
	 * <p>
	 * This is an alias for {@link #setPropertyTypeKey(String)}.
	 * </p>
	 * 
	 * @param key
	 *        the datum property type key to set
	 */
	public void setDatumPropertyTypeValue(String key) {
		setPropertyTypeKey(key);
	}

	/**
	 * Get the datum property type.
	 * 
	 * <p>
	 * This is an alias for {@link #getPropertyType()}.
	 * </p>
	 * 
	 * @return the type
	 */
	public GeneralDatumSamplesType getDatumPropertyType() {
		return getPropertyType();
	}

	/**
	 * Set the datum property type.
	 * 
	 * <p>
	 * This is an alias for {@link #setPropertyType(GeneralDatumSamplesType)},
	 * and ignores a {@literal null} argument.
	 * </p>
	 * 
	 * @param datumPropertyType
	 *        the datum property type to set
	 */
	public void setDatumPropertyType(GeneralDatumSamplesType datumPropertyType) {
		if ( datumPropertyType == null ) {
			return;
		}
		setPropertyType(datumPropertyType);
	}

	/**
	 * Get the property type key.
	 * 
	 * <p>
	 * This returns the configured {@link #getPropertyType()}
	 * {@link GeneralDatumSamplesType#toKey()} value as a string. If the type is
	 * not available, {@link GeneralDatumSamplesType#Instantaneous} will be
	 * returned.
	 * </p>
	 * 
	 * @return the property type key
	 */
	public String getDatumPropertyTypeKey() {
		GeneralDatumSamplesType type = getDatumPropertyType();
		if ( type == null ) {
			type = GeneralDatumSamplesType.Instantaneous;
		}
		return Character.toString(type.toKey());
	}

	/**
	 * Set the property type via a key value.
	 * 
	 * <p>
	 * This uses the first character of {@code key} as a
	 * {@link GeneralDatumSamplesType} key value to call
	 * {@link #setPropertyType(GeneralDatumSamplesType)}. If there is any
	 * problem parsing the type, {@link GeneralDatumSamplesType#Instantaneous}
	 * is set.
	 * </p>
	 * 
	 * @param key
	 *        the datum property type key to set
	 */
	public void setDatumPropertyTypeKey(String key) {
		GeneralDatumSamplesType type = null;
		if ( key != null && key.length() > 0 ) {
			try {
				type = GeneralDatumSamplesType.valueOf(key.charAt(0));
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		if ( type == null ) {
			type = GeneralDatumSamplesType.Instantaneous;
		}
		setDatumPropertyType(type);
	}

	/**
	 * Get the Modbus function to use.
	 * 
	 * @return the Modbus function
	 */
	public ModbusReadFunction getFunction() {
		return function;
	}

	/**
	 * Set the Modbus function to use.
	 * 
	 * @param function
	 *        the Modbus function
	 */
	public void setFunction(ModbusReadFunction function) {
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
		setFunction(ModbusReadFunction.forCode(Integer.parseInt(function)));
	}

	/**
	 * Get the Modbus function to use as a string.
	 * 
	 * @return the Modbus function
	 * @deprecated use {@link #getFunctionCode()}
	 */
	@Deprecated
	public String getFunctionValue() {
		return function.toString();
	}

	/**
	 * Set the Modbus function to use as a string.
	 * 
	 * @param function
	 *        the Modbus function
	 * @deprecated use {@link #setFunctionCode(String)}
	 */
	@Deprecated
	public void setFunctionValue(String function) {
		if ( function == null ) {
			return;
		}
		setFunction(ModbusReadFunction.valueOf(function));
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
	 * Get the data type as a string value.
	 * 
	 * @return the type as a string
	 * @deprecated use {@link #getDataTypeKey()}
	 */
	@Deprecated
	public String getDataTypeValue() {
		return dataType.toString();
	}

	/**
	 * Set the data type as a string value.
	 * 
	 * <p>
	 * If {@code this.dataType} already has a value, this method will not do
	 * anything.
	 * </p>
	 * 
	 * @param dataType
	 *        the type to set
	 * @deprecated use {@link #setDataTypeKey(String)}
	 */
	@Deprecated
	public void setDataTypeValue(String dataType) {
		if ( dataType != null ) {
			return;
		}
		setDataType(
				net.solarnetwork.node.datum.modbus.ModbusDataType.valueOf(dataType).toModbusDataType());
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
	 *        the type key to set
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
	 * @return the register count to read, never {@literal null}
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
	 *        the register count to read; if {@literal null} or less than
	 *        {@literal 1} then {@literal 1} will be set
	 */
	public void setWordLength(Integer wordLength) {
		if ( wordLength == null || wordLength < 1 ) {
			wordLength = 1;
		}
		this.wordLength = wordLength;
	}

	/**
	 * Get the register address to start reading data from.
	 * 
	 * <p>
	 * This is an alias for {@link #getConfig()}, returning {@literal 0} if that
	 * returns {@literal null}.
	 * </p>
	 * 
	 * @return the register address, never {@literal null}
	 */
	public Integer getAddress() {
		Integer addr = getConfig();
		return (addr != null ? addr : 0);
	}

	/**
	 * Set the register address to start reading data from.
	 * 
	 * <p>
	 * This is an alias for
	 * {@link GeneralDatumSamplePropertyConfig#setConfig(Object)}.
	 * </p>
	 * 
	 * @param address
	 *        the register address to set
	 */
	public void setAddress(Integer address) {
		setConfig(address);
	}

	/**
	 * Get the unit multiplier.
	 * 
	 * @return the multiplier, never {@literal null}
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
	 *        the mutliplier to set; if {@literal null} then {@literal 1} will
	 *        be set instead
	 */
	public void setUnitMultiplier(BigDecimal unitMultiplier) {
		if ( unitMultiplier == null ) {
			unitMultiplier = BigDecimal.ONE;
		}
		this.unitMultiplier = unitMultiplier;
	}

	/**
	 * Get the decimal scale to round decimal numbers to.
	 * 
	 * @return the decimal scale, never {@literal null}
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
	 *        the scale to set, or {@literal -1} to disable rounding completely;
	 *        if {@literal null} then {@literal 0} will be set
	 */
	public void setDecimalScale(Integer decimalScale) {
		if ( decimalScale == null ) {
			decimalScale = 0;
		}
		this.decimalScale = decimalScale;
	}

}

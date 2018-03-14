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
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a single datum property to be set via Modbus.
 * 
 * @author matt
 * @version 1.0
 */
public class ModbusPropertyConfig {

	private String name;
	private GeneralDatumSamplesType datumPropertyType;
	private ModbusReadFunction function;
	private ModbusDataType dataType;
	private int address;
	private int wordLength;
	private BigDecimal unitMultiplier;
	private int decimalScale;

	/**
	 * Default constructor.
	 */
	public ModbusPropertyConfig() {
		super();
		datumPropertyType = GeneralDatumSamplesType.Instantaneous;
		dataType = ModbusDataType.Float32;
		function = ModbusReadFunction.ReadHoldingRegister;
		address = 0;
		wordLength = 1;
		unitMultiplier = BigDecimal.ONE;
		decimalScale = 0;
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
		super();
		this.name = name;
		this.datumPropertyType = datumPropertyType;
		this.function = ModbusReadFunction.ReadHoldingRegister;
		this.dataType = dataType;
		this.address = address;
		this.wordLength = wordLength;
		this.unitMultiplier = unitMultiplier;
		this.decimalScale = decimalScale;
	}

	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "name", ""));

		// drop-down menu for datumPropertyType
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "datumPropertyTypeValue",
				Character.toString(GeneralDatumSamplesType.Instantaneous.toKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<String, String>(3);
		for ( GeneralDatumSamplesType e : GeneralDatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "address", "0"));

		// drop-down menu for function
		BasicMultiValueSettingSpecifier functionSpec = new BasicMultiValueSettingSpecifier(
				prefix + "functionValue", ModbusReadFunction.ReadHoldingRegister.toString());
		Map<String, String> functionTitles = new LinkedHashMap<String, String>(4);
		for ( ModbusReadFunction e : ModbusReadFunction.values() ) {
			functionTitles.put(e.toString(), e.toDisplayString());
		}
		functionSpec.setValueTitles(functionTitles);
		results.add(functionSpec);

		// drop-down menu for dataType
		BasicMultiValueSettingSpecifier dataTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "dataTypeValue", ModbusDataType.Float32.toString());
		Map<String, String> dataTypeTitles = new LinkedHashMap<String, String>(3);
		for ( ModbusDataType e : ModbusDataType.values() ) {
			dataTypeTitles.put(e.toString(), e.toString());
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
	 * @return the property name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the datum property name to use.
	 * 
	 * @param name
	 *        the property name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the datum property type key.
	 * 
	 * @return the property type key
	 */
	public String getDatumPropertyTypeValue() {
		return Character.toString(datumPropertyType.toKey());
	}

	/**
	 * Set the datum property type via a key value.
	 * 
	 * @param key
	 *        the datum property type key to set
	 */
	public void setDatumPropertyTypeValue(String key) {
		if ( key == null || key.length() < 1 ) {
			return;
		}
		this.datumPropertyType = GeneralDatumSamplesType.valueOf(key.charAt(0));
	}

	/**
	 * Get the datum property type.
	 * 
	 * @return the type
	 */
	public GeneralDatumSamplesType getDatumPropertyType() {
		return datumPropertyType;
	}

	/**
	 * Set the datum property type.
	 * 
	 * @param datumPropertyType
	 *        the datum property type to set
	 */
	public void setDatumPropertyType(GeneralDatumSamplesType datumPropertyType) {
		if ( datumPropertyType == null ) {
			return;
		}
		this.datumPropertyType = datumPropertyType;
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
	 * Get the Modbus function to use as a string.
	 * 
	 * @return the Modbus function
	 */
	public String getFunctionValue() {
		return function.toString();
	}

	/**
	 * Set the Modbus function to use as a string.
	 * 
	 * @param function
	 *        the Modbus function
	 */
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
	 */
	public String getDataTypeValue() {
		return dataType.toString();
	}

	/**
	 * Set the data type as a string value.
	 * 
	 * @param dataType
	 *        the type to set
	 */
	public void setDataTypeValue(String dataType) {
		setDataType(ModbusDataType.valueOf(dataType));
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
	public int getWordLength() {
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
	public void setWordLength(int wordLength) {
		if ( wordLength < 1 ) {
			return;
		}
		this.wordLength = wordLength;
	}

	/**
	 * Get the register address to start reading data from.
	 * 
	 * @return the register address
	 */
	public int getAddress() {
		return address;
	}

	/**
	 * Set the register address to start reading data from.
	 * 
	 * @param address
	 *        the register address to set
	 */
	public void setAddress(int address) {
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
	public int getDecimalScale() {
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
	public void setDecimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
	}

}

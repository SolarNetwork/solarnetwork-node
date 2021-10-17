/* ==================================================================
 * MeasurementConfig.java - 17/09/2020 4:05:43 PM
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

package net.solarnetwork.node.io.modbus.server.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.NumberUtils;

/**
 * Configuration for a Modbus measurement captured from a datum source property.
 * 
 * @author matt
 * @version 2.0
 */
public class MeasurementConfig {

	/** The default value for the {@code dataType} property. */
	public static final ModbusDataType DEFAULT_DATA_TYPE = ModbusDataType.UInt16;

	/** The default value for the {@code unitMultiplier} property. */
	public static final BigDecimal DEFAULT_UNIT_MULTIPLIER = BigDecimal.ONE;

	/** The default value for the {@code decimalScale} property. */
	public static final int DEFAULT_DECIMAL_SCALE = 0;

	/** The default value for the {@code wordLength} property . */
	public static final int DEFAULT_WORD_LENGTH = 1;

	private String sourceId;
	private String propertyName;
	private ModbusDataType dataType = DEFAULT_DATA_TYPE;
	private int wordLength = DEFAULT_WORD_LENGTH;
	private BigDecimal unitMultiplier = DEFAULT_UNIT_MULTIPLIER;
	private int decimalScale = DEFAULT_DECIMAL_SCALE;

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>(6);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "sourceId", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyName", ""));

		// drop-down menu for data type
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "dataTypeKey", DEFAULT_DATA_TYPE.getKey());
		Map<String, String> propTypeTitles = new LinkedHashMap<>(3);
		for ( ModbusDataType e : ModbusDataType.values() ) {
			propTypeTitles.put(e.getKey(), e.toDisplayString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "wordLength",
				String.valueOf(DEFAULT_WORD_LENGTH)));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "unitMultiplier",
				DEFAULT_UNIT_MULTIPLIER.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));

		return results;
	}

	/**
	 * Apply the configured unit multiplier and decimal scale, if appropriate.
	 * 
	 * @param propVal
	 *        the property value to transform; only {@link Number} values will
	 *        be transformed
	 * @return the transformed property value to use
	 */
	public Object applyTransforms(Object propVal) {
		if ( propVal instanceof Number ) {
			if ( unitMultiplier != null ) {
				propVal = applyUnitMultiplier((Number) propVal, unitMultiplier);
			}
			if ( decimalScale >= 0 ) {
				propVal = applyDecimalScale((Number) propVal, decimalScale);
			}
		}
		return propVal;
	}

	private static Number applyDecimalScale(Number value, int decimalScale) {
		if ( decimalScale < 0 ) {
			return value;
		}
		if ( value instanceof Byte || value instanceof Short || value instanceof Integer
				|| value instanceof Long || value instanceof BigInteger ) {
			// no decimal here
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		if ( v.scale() > decimalScale ) {
			v = v.setScale(decimalScale, RoundingMode.HALF_UP);
		}
		return v;
	}

	private static Number applyUnitMultiplier(Number value, BigDecimal multiplier) {
		if ( BigDecimal.ONE.compareTo(multiplier) == 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		return v.multiply(multiplier);
	}

	/**
	 * Get the number of registers used by this measurement.
	 * 
	 * @return the register count
	 */
	public int getSize() {
		int len = dataType.getWordLength();
		if ( len < 1 ) {
			len = getWordLength();
		}
		return len;
	}

	/**
	 * Get the datum source ID.
	 * 
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the datum source ID.
	 * 
	 * @param sourceId
	 *        the source ID to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the sample property name.
	 * 
	 * <p>
	 * This value represents a key in a datum property map.
	 * </p>
	 * 
	 * @return the sample property name
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Set the sample property key.
	 * 
	 * <p>
	 * This value represents a key in a datum map.
	 * </p>
	 * 
	 * @param name
	 *        the sample property name
	 */
	public void setPropertyName(String name) {
		this.propertyName = name;
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

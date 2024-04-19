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

import static java.lang.String.format;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.NumberUtils;

/**
 * Configuration for a Modbus measurement captured from a datum source property.
 *
 * @author matt
 * @version 2.3
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

	/**
	 * A setting type pattern for a unit configuration element.
	 *
	 * <p>
	 * The pattern has two capture groups: the unit configuration index and the
	 * property setting name.
	 * </p>
	 *
	 * @since 2.2
	 */
	public static final Pattern MEASUREMENT_SETTING_PATTERN = Pattern
			.compile(".+".concat(Pattern.quote(".measurementConfigs[")).concat("(\\d+)\\]\\.(.*)"));

	private String sourceId;
	private String propertyName;
	private ModbusDataType dataType = DEFAULT_DATA_TYPE;
	private Integer wordLength = DEFAULT_WORD_LENGTH;
	private BigDecimal unitMultiplier = DEFAULT_UNIT_MULTIPLIER;
	private Integer decimalScale = DEFAULT_DECIMAL_SCALE;

	/**
	 * Constructor.
	 */
	public MeasurementConfig() {
		super();
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
	 * @since 2.2
	 */
	public static boolean populateFromSetting(RegisterBlockConfig config, Setting setting) {
		Matcher m = MEASUREMENT_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		int idx = Integer.parseInt(m.group(1));
		String name = m.group(2);
		if ( idx >= config.getMeasurementConfigsCount() ) {
			config.setMeasurementConfigsCount(idx + 1);
		}
		MeasurementConfig measConfig = config.getMeasurementConfigs()[idx];

		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (name) {
				case "sourceId":
					measConfig.setSourceId(val);
					break;
				case "propertyName":
					measConfig.setPropertyName(val);
					break;
				case "dataTypeKey":
					measConfig.setDataTypeKey(val);
					break;
				case "wordLength":
					measConfig.setWordLength(Integer.valueOf(val));
					break;
				case "unitMultiplier":
					measConfig.setUnitMultiplier(new BigDecimal(val));
					break;
				case "decimalScale":
					measConfig.setDecimalScale(Integer.valueOf(val));
					break;
				default:
					// ignore
			}
		}
		return true;
	}

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
	 * Generate a list of setting values.
	 *
	 * @param providerId
	 *        the setting provider ID
	 * @param instanceId
	 *        the factory instance ID
	 * @param unitIdx
	 *        the unit configuration index
	 * @param blockIdx
	 *        the block configuration index
	 * @param measIdx
	 *        the measurement configuration index
	 * @return the settings
	 * @since 2.2
	 */
	public List<SettingValueBean> toSettingValues(String providerId, String instanceId, int unitIdx,
			int blockIdx, int measIdx) {
		List<SettingValueBean> settings = new ArrayList<>(2);
		addSetting(settings, providerId, instanceId, unitIdx, blockIdx, measIdx, "sourceId",
				getSourceId());
		addSetting(settings, providerId, instanceId, unitIdx, blockIdx, measIdx, "propertyName",
				getPropertyName());
		addSetting(settings, providerId, instanceId, unitIdx, blockIdx, measIdx, "dataTypeKey",
				getDataTypeKey());
		addSetting(settings, providerId, instanceId, unitIdx, blockIdx, measIdx, "wordLength",
				getWordLength());
		addSetting(settings, providerId, instanceId, unitIdx, blockIdx, measIdx, "unitMultiplier",
				getUnitMultiplier());
		addSetting(settings, providerId, instanceId, unitIdx, blockIdx, measIdx, "decimalScale",
				getDecimalScale());
		return settings;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId, String instanceId,
			int unitIdx, int blockIdx, int measIdx, String key, Object val) {
		if ( val == null ) {
			return;
		}
		settings.add(new SettingValueBean(providerId, instanceId,
				format("unitConfigs[%d].registerBlockConfigs[%d].measurementConfigs[%d].%s", unitIdx,
						blockIdx, measIdx, key),
				val.toString()));
	}

	/**
	 * Test if this configuration is empty.
	 *
	 * @return {@literal true} if all properties are null
	 * @since 2.2
	 */
	public boolean isEmpty() {
		return (dataType == null && decimalScale == null && propertyName == null && sourceId == null
				&& unitMultiplier == null);
	}

	/**
	 * Test if this configuration appears to be valid.
	 *
	 * @return {@literal true} if the configuration has all necessary properties
	 *         configured
	 * @since 2.2
	 */
	public boolean isValid() {
		String sourceId = getSourceId();
		String propName = getPropertyName();
		return (sourceId != null && !sourceId.trim().isEmpty() && propName != null
				&& !propName.trim().isEmpty() && dataType != null);
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
			len = (getWordLength() != null ? getWordLength().intValue() : 1);
		}
		return len;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MeasurementConfig{");
		if ( sourceId != null ) {
			builder.append("sourceId=");
			builder.append(sourceId);
			builder.append(", ");
		}
		if ( propertyName != null ) {
			builder.append("propertyName=");
			builder.append(propertyName);
			builder.append(", ");
		}
		if ( dataType != null ) {
			builder.append("dataType=");
			builder.append(dataType);
			builder.append(", ");
		}
		if ( wordLength != null ) {
			builder.append("wordLength=");
			builder.append(wordLength);
			builder.append(", ");
		}
		if ( unitMultiplier != null ) {
			builder.append("unitMultiplier=");
			builder.append(unitMultiplier);
			builder.append(", ");
		}
		if ( decimalScale != null ) {
			builder.append("decimalScale=");
			builder.append(decimalScale);
		}
		builder.append("}");
		return builder.toString();
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
		if ( wordLength != null && wordLength.intValue() < 1 ) {
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

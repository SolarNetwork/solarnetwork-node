/* ==================================================================
 * BacnetWritePropertyConfig.java - 10/11/2022 8:16:51 am
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.bacnet;

import static java.lang.String.format;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.io.bacnet.BacnetDeviceObjectPropertyRef;
import net.solarnetwork.node.io.bacnet.BacnetObjectType;
import net.solarnetwork.node.io.bacnet.BacnetPropertyType;
import net.solarnetwork.node.io.bacnet.BacnetUtils;
import net.solarnetwork.node.io.bacnet.SimpleBacnetDeviceObjectPropertyRef;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a single control property to be set via BACnet.
 *
 * @author matt
 * @version 1.1
 */
public class BacnetWritePropertyConfig {

	/** The {@code controlPropertyType} property default value. */
	public static final NodeControlPropertyType DEFAULT_CONTROL_PROPERTY_TYPE = NodeControlPropertyType.Boolean;

	/** The {@code unitMultiplier} property default value. */
	public static final BigDecimal DEFAULT_UNIT_MULTIPLIER = BigDecimal.ONE;

	/** The {@code decimalScale} property default value. */
	public static final Integer DEFAULT_DECIMAL_SCALE = 5;

	/**
	 * A setting type pattern for a property configuration element.
	 *
	 * <p>
	 * The pattern has two capture groups: the property configuration index and
	 * the property setting name.
	 * </p>
	 */
	public static final Pattern PROP_SETTING_PATTERN = Pattern
			.compile(Pattern.quote("propConfigs[").concat("(\\d+)]\\.(.*)"));

	private static final Logger log = LoggerFactory.getLogger(BacnetWritePropertyConfig.class);

	private String controlId;
	private NodeControlPropertyType controlPropertyType = DEFAULT_CONTROL_PROPERTY_TYPE;
	private Integer deviceId;
	private Integer objectType;
	private Integer objectNumber;
	private Integer propertyId;
	private Integer priority;
	private BigDecimal unitMultiplier = DEFAULT_UNIT_MULTIPLIER;
	private Integer decimalScale = DEFAULT_DECIMAL_SCALE;

	/**
	 * Constructor.
	 */
	public BacnetWritePropertyConfig() {
		super();
	}

	/**
	 * Get the settings to configure an instance of this class.
	 *
	 * @param prefix
	 *        the settings prefix to use
	 * @return the settings
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "controlId", null));

		// drop-down menu for controlPropertyType
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "controlPropertyTypeKey",
				String.valueOf(DEFAULT_CONTROL_PROPERTY_TYPE.getKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<>(3);
		for ( NodeControlPropertyType e : NodeControlPropertyType.values() ) {
			propTypeTitles.put(Character.toString(e.getKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "deviceId", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "objectTypeValue", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "objectNumber", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyIdValue", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "priority", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "unitMultiplier",
				DEFAULT_UNIT_MULTIPLIER.toPlainString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale",
				DEFAULT_DECIMAL_SCALE.toString()));
		return results;
	}

	/**
	 * Test if this configuration is empty.
	 *
	 * @return {@literal true} if all properties are null
	 */
	public boolean isEmpty() {
		return (controlPropertyType == null && deviceId == null && objectType == null
				&& objectNumber == null && propertyId == null && unitMultiplier == null
				&& decimalScale == null);
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
				&& deviceId != null && deviceId.intValue() >= 0 && objectType != null
				&& objectType.intValue() >= 0 && objectNumber != null && objectNumber.intValue() >= 0;
	}

	/**
	 * Get a reference for the configuration of this instance.
	 *
	 * @return the reference, or {@literal null} if any of the required
	 *         configuration is missing
	 */
	public BacnetDeviceObjectPropertyRef toRef() {
		if ( deviceId == null || objectType == null || objectNumber == null ) {
			return null;
		}
		final Integer propId = getPropertyId();
		final int pid = (propId != null ? propId.intValue() : BacnetPropertyType.PresentValue.getCode());
		final Integer prio = getPriority();
		return new SimpleBacnetDeviceObjectPropertyRef(deviceId, objectType, objectNumber, pid,
				BacnetDeviceObjectPropertyRef.NOT_INDEXED,
				prio != null ? prio.intValue() : BacnetDeviceObjectPropertyRef.NO_PRIORITY);
	}

	/**
	 * Populate a setting as a property configuration value, if possible.
	 *
	 * @param config
	 *        the control configuration
	 * @param setting
	 *        the setting to try to handle
	 * @return {@literal true} if the setting was handled as a property
	 *         configuration value
	 */
	public static boolean populateFromSetting(BacnetControlConfig config, Setting setting) {
		Matcher m = PROP_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		int idx = Integer.parseInt(m.group(1));
		String name = m.group(2);
		List<BacnetWritePropertyConfig> propConfigs = config.getPropertyConfigs();
		if ( !(idx < propConfigs.size()) ) {
			propConfigs.add(idx, new BacnetWritePropertyConfig());
		}
		BacnetWritePropertyConfig propConfig = propConfigs.get(idx);
		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (name) {
				case "controlId":
					propConfig.setControlId(val);
					break;
				case "controlPropertyTypeKey":
					propConfig.setControlPropertyTypeKey(val);
					break;
				case "deviceId":
					propConfig.setDeviceId(Integer.valueOf(val));
					break;
				case "objectTypeValue":
					propConfig.setObjectTypeValue(val);
					break;
				case "objectNumber":
					propConfig.setObjectNumber(Integer.valueOf(val));
					break;
				case "propertyIdValue":
					propConfig.setPropertyIdValue(val);
					break;
				case "priority":
					propConfig.setPriority(Integer.valueOf(val));
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
	 * Generate a list of setting values.
	 *
	 * @param providerId
	 *        the setting provider ID
	 * @param instanceId
	 *        the factory instance ID
	 * @param i
	 *        the property index
	 * @return the settings
	 */
	public List<SettingValueBean> toSettingValues(String providerId, String instanceId, int i) {
		List<SettingValueBean> settings = new ArrayList<>(8);
		addSetting(settings, providerId, instanceId, i, "controlId", getControlId());
		addSetting(settings, providerId, instanceId, i, "controlPropertyTypeKey",
				getControlPropertyTypeKey());
		addSetting(settings, providerId, instanceId, i, "deviceId", getDeviceId());
		addSetting(settings, providerId, instanceId, i, "objectTypeValue", getObjectTypeValue());
		addSetting(settings, providerId, instanceId, i, "objectNumber", getObjectNumber());
		addSetting(settings, providerId, instanceId, i, "propertyIdValue", getPropertyIdValue());
		addSetting(settings, providerId, instanceId, i, "priority", getPriority());
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BacnetWritePropertyConfig{");
		if ( controlId != null ) {
			builder.append("controlId=");
			builder.append(controlId);
			builder.append(", ");
		}
		if ( controlPropertyType != null ) {
			builder.append("controlPropertyType=");
			builder.append(controlPropertyType);
			builder.append(", ");
		}
		if ( deviceId != null ) {
			builder.append("deviceId=");
			builder.append(deviceId);
			builder.append(", ");
		}
		if ( objectType != null ) {
			builder.append("objectType=");
			builder.append(objectType);
			builder.append(", ");
		}
		if ( objectNumber != null ) {
			builder.append("objectNumber=");
			builder.append(objectNumber);
			builder.append(", ");
		}
		if ( propertyId != null ) {
			builder.append("propertyId=");
			builder.append(propertyId);
			builder.append(", ");
		}
		if ( priority != null ) {
			builder.append("priority=");
			builder.append(priority);
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
	 * Get the BACnet device (instance) ID.
	 *
	 * @return the deviceId the device ID
	 */
	public Integer getDeviceId() {
		return deviceId;
	}

	/**
	 * Set the BACnet device (instance) ID.
	 *
	 * @param deviceId
	 *        the device ID to set
	 */
	public void setDeviceId(Integer deviceId) {
		this.deviceId = deviceId;
	}

	/**
	 * Get the object type.
	 *
	 * @return the object type
	 */
	public Integer getObjectType() {
		return objectType;
	}

	/**
	 * Set the object type.
	 *
	 * @param objectType
	 *        the object type to set
	 */
	public void setObjectType(Integer objectType) {
		this.objectType = objectType;
	}

	/**
	 * Get the object type as a string value.
	 *
	 * @return the object type enumeration name if supported, else the
	 *         configured object type as a string
	 */
	public String getObjectTypeValue() {
		Integer type = getObjectType();
		if ( type == null ) {
			return null;
		}
		BacnetObjectType e = CodedValue.forCodeValue(type.intValue(), BacnetObjectType.class, null);
		return (e != null ? BacnetUtils.camelToKebabCase(e.name()) : type.toString());
	}

	/**
	 * Set the object type as a string value.
	 *
	 * <p>
	 * The value can be an object type code, enumeration name, or train-case
	 * enumeration name.
	 * </p>
	 *
	 * @param value
	 *        the value to set
	 */
	public void setObjectTypeValue(String value) {
		if ( value == null ) {
			setObjectType(null);
			return;
		}
		try {
			setObjectType(BacnetObjectType.forKey(value).getCode());
		} catch ( IllegalArgumentException e ) {
			log.error("Unsupported BACnet object type [{}]", value);
		}
	}

	/**
	 * Get the object (instance) number.
	 *
	 * @return the object number
	 */
	public Integer getObjectNumber() {
		return objectNumber;
	}

	/**
	 * Set the object (instance) number.
	 *
	 * @param objectNumber
	 *        the object number to set
	 */
	public void setObjectNumber(Integer objectNumber) {
		this.objectNumber = objectNumber;
	}

	/**
	 * Get the BACnet property ID.
	 *
	 * @return the property ID
	 */
	public Integer getPropertyId() {
		return propertyId;
	}

	/**
	 * Set the BACnet property ID.
	 *
	 * @param propertyId
	 *        the property ID to set
	 */
	public void setPropertyId(Integer propertyId) {
		this.propertyId = propertyId;
	}

	/**
	 * Get the property ID as a string value.
	 *
	 * @return the property type enumeration name if supported, else the
	 *         configured object type as a string
	 */
	public String getPropertyIdValue() {
		Integer type = getPropertyId();
		if ( type == null ) {
			return null;
		}
		BacnetPropertyType e = CodedValue.forCodeValue(type.intValue(), BacnetPropertyType.class, null);
		return (e != null ? BacnetUtils.camelToKebabCase(e.name()) : type.toString());
	}

	/**
	 * Set the property ID as a string value.
	 *
	 * <p>
	 * The value can be an property ID code, enumeration name, or train-case
	 * enumeration name.
	 * </p>
	 *
	 * @param value
	 *        the value to set
	 */
	public void setPropertyIdValue(String value) {
		try {
			setPropertyId(BacnetPropertyType.forKey(value).getCode());
		} catch ( IllegalArgumentException e ) {
			log.error("Unsupported BACnet property type [{}]", value);
		}
	}

	/**
	 * Get the write priority.
	 *
	 * @return the priority, a value between 1-16 with 1 being the highest
	 *         priority
	 */
	public Integer getPriority() {
		return priority;
	}

	/**
	 * Set the write priority.
	 *
	 * @param priority
	 *        the priority to set: must be a value between 1-16 with 1 being the
	 *        highest priority, or {@literal null} to use the default priority
	 *        (16)
	 */
	public void setPriority(Integer priority) {
		this.priority = priority;
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

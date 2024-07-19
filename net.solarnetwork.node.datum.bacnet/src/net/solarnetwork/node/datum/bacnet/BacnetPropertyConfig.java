/* ==================================================================
 * BacnetPropertyConfig.java - 4/11/2022 2:00:32 pm
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

package net.solarnetwork.node.datum.bacnet;

import static java.lang.String.format;
import static net.solarnetwork.node.datum.bacnet.BacnetDatumDataSourceConfig.JOB_SERVICE_SETTING_PREFIX;
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
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.NumberDatumSamplePropertyConfig;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.io.bacnet.BacnetDeviceObjectPropertyRef;
import net.solarnetwork.node.io.bacnet.BacnetObjectType;
import net.solarnetwork.node.io.bacnet.BacnetPropertyType;
import net.solarnetwork.node.io.bacnet.BacnetUtils;
import net.solarnetwork.node.io.bacnet.SimpleBacnetDeviceObjectPropertyCovRef;
import net.solarnetwork.node.io.bacnet.SimpleBacnetDeviceObjectPropertyRef;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.ArrayUtils;

/**
 * Configuration for a single datum property to be set via a BACnet property.
 *
 * <p>
 * The {@link #getConfig()} value represents the BACnet property identifier to
 * read.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class BacnetPropertyConfig extends NumberDatumSamplePropertyConfig<Integer> {

	/**
	 * A setting type pattern for a property configuration element.
	 *
	 * <p>
	 * The pattern has two capture groups: the device configuration index and
	 * the property setting name.
	 * </p>
	 */
	public static final Pattern PROP_SETTING_PATTERN = Pattern
			.compile(Pattern.quote(JOB_SERVICE_SETTING_PREFIX.concat("deviceConfigs[")).concat("\\d+")
					.concat(Pattern.quote("].propConfigs[")).concat("(\\d+)]\\.(.*)"));

	private static final Logger log = LoggerFactory.getLogger(BacnetPropertyConfig.class);

	private Integer objectType;
	private Integer objectNumber;
	private Float covIncrement;

	/**
	 * Default constructor.
	 */
	public BacnetPropertyConfig() {
		super();
		setPropertyType(null);
	}

	/**
	 * Construct with values.
	 *
	 * @param propertyKey
	 *        the property key
	 * @param propertyType
	 *        the property type
	 * @param propertyId
	 *        the BACnet property ID
	 */
	public BacnetPropertyConfig(String propertyKey, DatumSamplesType propertyType, Integer propertyId) {
		super(propertyKey, propertyType, propertyId);
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		if ( prefix == null ) {
			prefix = "";
		}
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "objectTypeValue", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "objectNumber", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyIdValue", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "covIncrement", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyKey", null));

		// drop-down menu for propertyTypeKey
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "propertyTypeKey", "");
		Map<String, String> propTypeTitles = new LinkedHashMap<>(4);
		propTypeTitles.put("", "");
		for ( DatumSamplesType e : DatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "slope", DEFAULT_SLOPE.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));

		return results;
	}

	/**
	 * Test if this configuration is empty.
	 *
	 * @return {@literal true} if all properties are null
	 */
	public boolean isEmpty() {
		return (objectType == null && objectNumber == null && getPropertyId() == null
				&& covIncrement == null && getSlope() == null && getPropertyType() == null
				&& getPropertyKey() == null && getConfig() == null);
	}

	/**
	 * Test if this instance represents a valid configuration.
	 *
	 * <p>
	 * This only verifies that the configuration is complete, not that actual
	 * BACnet device properties exist for the configured values.
	 * </p>
	 *
	 * @return {@literal true} if this instance represents a valid configuration
	 */
	public boolean isValid() {
		if ( objectType == null || objectType.intValue() < 0 || objectNumber == null
				|| objectNumber.intValue() < 0 ) {
			return false;
		}
		final Integer propId = getPropertyId();
		final String datumPropName = getPropertyKey();
		if ( (propId != null && propId.intValue() < 0) || datumPropName == null
				|| datumPropName.trim().isEmpty() ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BacnetPropertyConfig{objectType=");
		builder.append(getObjectTypeValue());
		builder.append(", objectNumber=");
		builder.append(objectNumber);
		builder.append(", propertyId=");
		builder.append(getPropertyIdValue());
		builder.append(", covIncrement=");
		builder.append(covIncrement);
		builder.append(", valid=");
		builder.append(isValid());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get a reference for a given device ID and the configuration of this
	 * instance.
	 *
	 * @param deviceId
	 *        the device ID
	 * @return the reference, or {@literal null} if any of the required
	 *         configuration is missing
	 */
	public BacnetDeviceObjectPropertyRef toRef(Integer deviceId) {
		if ( deviceId == null || objectType == null || objectNumber == null ) {
			return null;
		}
		final Integer propId = getPropertyId();
		final int pid = (propId != null ? propId.intValue() : BacnetPropertyType.PresentValue.getCode());
		final Float covInc = getCovIncrement();
		if ( covInc != null && !covInc.isInfinite() && covInc.floatValue() > 0.0f ) {
			return new SimpleBacnetDeviceObjectPropertyCovRef(deviceId, objectType, objectNumber, pid,
					covInc);
		}
		return new SimpleBacnetDeviceObjectPropertyRef(deviceId, objectType, objectNumber, pid);
	}

	/**
	 * Populate a setting as a property configuration value, if possible.
	 *
	 * @param config
	 *        the device configuration
	 * @param setting
	 *        the setting to try to handle
	 * @return {@literal true} if the setting was handled as a property
	 *         configuration value
	 */
	public static boolean populateFromSetting(BacnetDeviceConfig config, Setting setting) {
		Matcher m = PROP_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		int idx = Integer.parseInt(m.group(1));
		String name = m.group(2);
		BacnetPropertyConfig[] propConfigs = config.getPropConfigs();
		if ( propConfigs == null || idx >= propConfigs.length ) {
			propConfigs = ArrayUtils.arrayWithLength(propConfigs, idx + 1, BacnetPropertyConfig.class,
					null);
			config.setPropConfigs(propConfigs);
		}
		BacnetPropertyConfig propConfig = propConfigs[idx];
		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (name) {
				case "objectTypeValue":
					propConfig.setObjectTypeValue(val);
					break;
				case "objectNumber":
					propConfig.setObjectNumber(Integer.valueOf(val));
					break;
				case "propertyIdValue":
					propConfig.setPropertyIdValue(val);
					break;
				case "covIncrement":
					propConfig.setCovIncrement(Float.valueOf(val));
					break;
				case "propertyKey":
					propConfig.setPropertyKey(val);
					break;
				case "propertyTypeKey":
					propConfig.setPropertyTypeKey(val);
					break;
				case "slope":
					propConfig.setSlope(new BigDecimal(val));
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
	 * @param deviceIndex
	 *        the device configuration index
	 * @param i
	 *        the property index
	 * @return the settings
	 */
	public List<SettingValueBean> toSettingValues(String providerId, String instanceId, int deviceIndex,
			int i) {
		List<SettingValueBean> settings = new ArrayList<>(8);
		addSetting(settings, providerId, instanceId, deviceIndex, i, "objectTypeValue",
				getObjectTypeValue());
		addSetting(settings, providerId, instanceId, deviceIndex, i, "objectNumber", getObjectNumber());
		addSetting(settings, providerId, instanceId, deviceIndex, i, "propertyIdValue",
				getPropertyIdValue());
		addSetting(settings, providerId, instanceId, deviceIndex, i, "covIncrement", getCovIncrement());
		addSetting(settings, providerId, instanceId, deviceIndex, i, "propertyKey", getPropertyKey());
		addSetting(settings, providerId, instanceId, deviceIndex, i, "propertyTypeKey",
				getPropertyTypeKey());
		addSetting(settings, providerId, instanceId, deviceIndex, i, "slope", getSlope());
		addSetting(settings, providerId, instanceId, deviceIndex, i, "decimalScale", getDecimalScale());
		return settings;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId, String instanceId,
			int deviceIndex, int i, String key, Object val) {
		if ( val == null ) {
			return;
		}
		settings.add(new SettingValueBean(providerId, instanceId,
				BacnetDatumDataSourceConfig.JOB_SERVICE_SETTING_PREFIX
						.concat(format("deviceConfigs[%d].propConfigs[%d].%s", deviceIndex, i, key)),
				val.toString()));
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
	 * <p>
	 * This is an alias for {@link #getConfig()}.
	 * </p>
	 *
	 * @return the property ID
	 */
	public Integer getPropertyId() {
		return getConfig();
	}

	/**
	 * Set the BACnet property ID.
	 *
	 * <p>
	 * This is an alias for {@link #setConfig(Object)}.
	 * </p>
	 *
	 * @param propertyId
	 *        the property ID to set
	 */
	public void setPropertyId(Integer propertyId) {
		setConfig(propertyId);
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
		if ( value == null ) {
			setPropertyId(null);
			return;
		}
		try {
			setPropertyId(BacnetPropertyType.forKey(value).getCode());
		} catch ( IllegalArgumentException e ) {
			log.error("Unsupported BACnet property type [{}]", value);
		}
	}

	/**
	 * Get the change-of-value increment (notification threshold).
	 *
	 * @return the COV increment
	 */
	public Float getCovIncrement() {
		return covIncrement;
	}

	/**
	 * Set the change-of-value increment (notification threshold).
	 *
	 * @param covIncrement
	 *        the COV increment to set
	 */
	public void setCovIncrement(Float covIncrement) {
		this.covIncrement = covIncrement;
	}

}

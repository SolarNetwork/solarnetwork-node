/* ==================================================================
 * MBusPropertyConfig.java - 09/07/2020 10:43:58 am
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

package net.solarnetwork.node.datum.mbus;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static net.solarnetwork.node.datum.mbus.BaseDatumDataSourceConfig.JOB_SERVICE_SETTING_PREFIX;
import static net.solarnetwork.node.datum.mbus.BaseDatumDataSourceConfig.addJobSetting;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.solarnetwork.domain.datum.DatumSamplePropertyConfig;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.io.mbus.MBusDataDescription;
import net.solarnetwork.node.io.mbus.MBusDataType;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a single datum property to be set via M-Bus.
 * 
 * <p>
 * The {@link #getConfig()} value represents the mbus address to read from.
 * </p>
 * 
 * @author alex
 * @version 2.1
 */
public class MBusPropertyConfig extends DatumSamplePropertyConfig<MBusDataDescription> {

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
	public static final Pattern PROP_SETTING_PATTERN = Pattern.compile(
			Pattern.quote(JOB_SERVICE_SETTING_PREFIX.concat("propConfigs[")).concat("(\\d+)\\]\\.(.*)"));

	private MBusDataType dataType;
	private BigDecimal unitMultiplier;
	private Integer decimalScale;

	/**
	 * Default constructor.
	 */
	public MBusPropertyConfig() {
		super(null, DatumSamplesType.Instantaneous, MBusDataDescription.NotSupported);
		dataType = MBusDataType.None;
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
	 *        the mbus data type
	 * @param dataDescription
	 *        the mbus data description
	 */
	public MBusPropertyConfig(String name, DatumSamplesType datumPropertyType, MBusDataType dataType,
			MBusDataDescription dataDescription) {
		this(name, datumPropertyType, dataType, dataDescription, BigDecimal.ONE, 0);
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param datumPropertyType
	 *        the datum property type
	 * @param dataType
	 *        the mbus data type
	 * @param dataDescription
	 *        the mbus data description
	 * @param unitMultiplier
	 *        the unit multiplier
	 * @param decimalScale
	 *        for numbers, the maximum decimal scale to support, or
	 *        {@literal -1} for no limit
	 */
	public MBusPropertyConfig(String name, DatumSamplesType datumPropertyType, MBusDataType dataType,
			MBusDataDescription dataDescription, BigDecimal unitMultiplier, int decimalScale) {
		super(name, datumPropertyType, dataDescription);
		this.dataType = dataType;
		this.unitMultiplier = unitMultiplier;
		this.decimalScale = decimalScale;
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param datumPropertyType
	 *        the datum property type
	 */
	public MBusPropertyConfig(String name, DatumSamplesType datumPropertyType) {
		super(name, datumPropertyType, null);
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		MBusPropertyConfig defaults = new MBusPropertyConfig();
		List<SettingSpecifier> results = new ArrayList<>();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "name", ""));

		// drop-down menu for datumPropertyType
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "datumPropertyTypeKey", defaults.getDatumPropertyTypeValue());
		Map<String, String> propTypeTitles = new LinkedHashMap<>(3);
		for ( DatumSamplesType e : DatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		// drop-down menu for dataDescription
		BasicMultiValueSettingSpecifier dataDescriptionSpec = new BasicMultiValueSettingSpecifier(
				prefix + "dataDescriptionKey", defaults.getDataDescriptionKey());
		Map<String, String> dataDescriptionTitles = new LinkedHashMap<>(256);
		for ( MBusDataDescription e : stream(MBusDataDescription.values())
				.sorted((l, r) -> l.name().compareTo(r.name())).collect(toList()) ) {
			dataDescriptionTitles.put(e.toString(), e.toString());
		}
		dataDescriptionSpec.setValueTitles(dataDescriptionTitles);
		results.add(dataDescriptionSpec);

		// drop-down menu for dataType
		BasicMultiValueSettingSpecifier dataTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "dataTypeKey", defaults.getDataTypeKey());
		Map<String, String> dataTypeTitles = new LinkedHashMap<>(8);
		for ( MBusDataType e : MBusDataType.values() ) {
			dataTypeTitles.put(e.toString(), e.toString());
		}
		dataTypeSpec.setValueTitles(dataTypeTitles);
		results.add(dataTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "unitMultiplier", "1"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale", "0"));
		return results;
	}

	/**
	 * Test if this configuration is empty.
	 * 
	 * @return {@literal true} if all properties are null
	 * @since 2.1
	 */
	public boolean isEmpty() {
		return (dataType == null && decimalScale == null && unitMultiplier == null && getName() == null
				&& getPropertyType() == null && getConfig() == null);
	}

	/**
	 * Test if this configuration is valid.
	 * 
	 * @return {@literal true} if all required properties are set
	 * @since 2.1
	 */
	public boolean isValid() {
		String propName = getName();
		return (dataType != null && propName != null && !propName.isEmpty() && getPropertyType() != null
				&& getConfig() != null);
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
		addSetting(settings, providerId, instanceId, i, "name", getName());
		addSetting(settings, providerId, instanceId, i, "datumPropertyTypeKey",
				getDatumPropertyTypeKey());
		addSetting(settings, providerId, instanceId, i, "dataTypeKey", getDataTypeKey());
		addSetting(settings, providerId, instanceId, i, "dataDescriptionKey", getDataDescriptionKey());
		addSetting(settings, providerId, instanceId, i, "unitMultiplier", getUnitMultiplier());
		addSetting(settings, providerId, instanceId, i, "decimalScale", getDecimalScale());
		return settings;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId, String instanceId,
			int i, String key, Object val) {
		if ( val == null ) {
			return;
		}
		String pKey = format("propConfigs[%d].%s", i, key);
		addJobSetting(settings, providerId, instanceId, pKey, val);
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
	public static boolean populateFromSetting(BaseDatumDataSourceConfig config, Setting setting) {
		Matcher m = PROP_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		int idx = Integer.parseInt(m.group(1));
		String name = m.group(2);
		List<MBusPropertyConfig> propConfigs = config.getPropertyConfigs();
		if ( !(idx < propConfigs.size()) ) {
			propConfigs.add(idx, new MBusPropertyConfig());
		}
		MBusPropertyConfig propConfig = propConfigs.get(idx);
		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (name) {
				case "name":
					propConfig.setName(val);
					break;
				case "datumPropertyTypeKey":
					propConfig.setDatumPropertyTypeKey(val);
					break;
				case "dataTypeKey":
					propConfig.setDataTypeKey(val);
					break;
				case "dataDescriptionKey":
					propConfig.setDataDescriptionKey(val);
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
	public DatumSamplesType getDatumPropertyType() {
		return getPropertyType();
	}

	/**
	 * Set the datum property type.
	 * 
	 * <p>
	 * This is an alias for {@link #setPropertyType(DatumSamplesType)}, and
	 * ignores a {@literal null} argument.
	 * </p>
	 * 
	 * @param datumPropertyType
	 *        the datum property type to set
	 */
	public void setDatumPropertyType(DatumSamplesType datumPropertyType) {
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
	 * {@link DatumSamplesType#toKey()} value as a string. If the type is not
	 * available, {@link DatumSamplesType#Instantaneous} will be returned.
	 * </p>
	 * 
	 * @return the property type key
	 */
	public String getDatumPropertyTypeKey() {
		DatumSamplesType type = getDatumPropertyType();
		if ( type == null ) {
			type = DatumSamplesType.Instantaneous;
		}
		return Character.toString(type.toKey());
	}

	/**
	 * Set the property type via a key value.
	 * 
	 * <p>
	 * This uses the first character of {@code key} as a
	 * {@link DatumSamplesType} key value to call
	 * {@link #setPropertyType(DatumSamplesType)}. If there is any problem
	 * parsing the type, {@link DatumSamplesType#Instantaneous} is set.
	 * </p>
	 * 
	 * @param key
	 *        the datum property type key to set
	 */
	public void setDatumPropertyTypeKey(String key) {
		DatumSamplesType type = null;
		if ( key != null && key.length() > 0 ) {
			try {
				type = DatumSamplesType.valueOf(key.charAt(0));
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		if ( type == null ) {
			type = DatumSamplesType.Instantaneous;
		}
		setDatumPropertyType(type);
	}

	/**
	 * Get the data type.
	 * 
	 * @return the type
	 */
	public MBusDataType getDataType() {
		return dataType;
	}

	/**
	 * Set the data type.
	 * 
	 * @param dataType
	 *        the type to set
	 */
	public void setDataType(MBusDataType dataType) {
		if ( dataType == null ) {
			return;
		}
		this.dataType = dataType;
	}

	/**
	 * Get the data type as a key value.
	 * 
	 * @return the data type as a key
	 */
	public String getDataTypeKey() {
		MBusDataType type = getDataType();
		return (type != null ? type.toString() : null);
	}

	/**
	 * Set the data type as a string value.
	 * 
	 * @param key
	 *        the data type to set as a key value
	 */
	public void setDataTypeKey(String key) {
		setDataType(MBusDataType.valueOf(key));
	}

	/**
	 * Get the description to read data for.
	 * 
	 * <p>
	 * This is an alias for {@link #getConfig()}, returning
	 * {@literal MBusDataDescription.NotSupported} if that returns
	 * {@literal null}.
	 * </p>
	 * 
	 * @return the data description
	 */
	public MBusDataDescription getDataDescription() {
		MBusDataDescription desc = getConfig();
		return (desc != null ? desc : MBusDataDescription.NotSupported);
	}

	/**
	 * Set the data description to read for.
	 * 
	 * <p>
	 * This is an alias for {@link #setConfig(Object)}.
	 * </p>
	 * 
	 * @param desc
	 *        the data description to set
	 */
	public void setDataDescription(MBusDataDescription desc) {
		setConfig(desc);
	}

	/**
	 * Get the data description as a key value.
	 * 
	 * @return the description as a key
	 */
	public String getDataDescriptionKey() {
		MBusDataDescription desc = getDataDescription();
		return (desc != null ? desc.toString() : null);
	}

	/**
	 * Set the data description as a string value.
	 * 
	 * @param key
	 *        the data description key to set
	 */
	public void setDataDescriptionKey(String key) {
		setDataDescription(MBusDataDescription.valueOf(key));
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

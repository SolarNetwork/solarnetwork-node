/* ==================================================================
 * CsvPropertyConfig.java - 31/03/2023 4:03:21 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.csv;

import static java.lang.String.format;
import static net.solarnetwork.node.datum.csv.CsvDatumDataSourceConfig.JOB_SERVICE_SETTING_PREFIX;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.solarnetwork.codec.CsvUtils;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.NumberDatumSamplePropertyConfig;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a CSV property.
 * 
 * @author matt
 * @version 1.0
 */
public class CsvPropertyConfig extends NumberDatumSamplePropertyConfig<String> {

	private int[] columnIndexes;

	/**
	 * A setting type pattern for a property configuration element.
	 * 
	 * <p>
	 * The pattern has two capture groups: the property configuration index and
	 * the property setting name.
	 * </p>
	 */
	public static final Pattern PROP_SETTING_PATTERN = Pattern.compile(
			Pattern.quote(JOB_SERVICE_SETTING_PREFIX.concat("propConfigs[")).concat("(\\d+)\\]\\.(.*)"));

	/**
	 * Constructor.
	 */
	public CsvPropertyConfig() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param propertyKey
	 *        the property name
	 * @param propertyType
	 *        the property type
	 * @param columnRef
	 *        the CSV column reference
	 */
	public CsvPropertyConfig(String propertyKey, DatumSamplesType propertyType, String columnRef) {
		super(propertyKey, propertyType, columnRef);
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
		results.add(new BasicTextFieldSettingSpecifier(prefix + "column", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyKey", null));

		// drop-down menu for propertyTypeKey
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "propertyTypeKey", String.valueOf(DEFAULT_PROPERTY_TYPE.toKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<>(4);
		for ( DatumSamplesType e : DatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "slope", DEFAULT_SLOPE.toString()));
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "intercept", DEFAULT_INTERCEPT.toString()));
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
		return (getSlope() == null && getIntercept() == null && getColumn() == null
				&& getPropertyKey() == null && getPropertyType() == null);
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
		final String column = getColumn();
		final String datumPropName = getPropertyKey();
		try {
			if ( CsvUtils.parseColumnReference(column) > 0 && datumPropName != null
					&& !datumPropName.trim().isEmpty() ) {
				return true;
			}
		} catch ( IllegalArgumentException e ) {
			// fall through
		}
		return false;
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
	public static boolean populateFromSetting(CsvDatumDataSourceConfig config, Setting setting) {
		Matcher m = PROP_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		int idx = Integer.parseInt(m.group(1));
		String name = m.group(2);
		List<CsvPropertyConfig> propConfigs = config.getPropertyConfigs();
		if ( !(idx < propConfigs.size()) ) {
			propConfigs.add(idx, new CsvPropertyConfig());
		}
		CsvPropertyConfig propConfig = propConfigs.get(idx);
		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (name) {
				case "column":
					propConfig.setColumn(val);
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
				case "intercept":
					propConfig.setIntercept(new BigDecimal(val));
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
		addSetting(settings, providerId, instanceId, i, "column", getColumn());
		addSetting(settings, providerId, instanceId, i, "propertyKey", getPropertyKey());
		addSetting(settings, providerId, instanceId, i, "propertyTypeKey", getPropertyTypeKey());
		addSetting(settings, providerId, instanceId, i, "slope", getSlope());
		addSetting(settings, providerId, instanceId, i, "intercept", getIntercept());
		addSetting(settings, providerId, instanceId, i, "decimalScale", getDecimalScale());
		return settings;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId, String instanceId,
			int i, String key, Object val) {
		if ( val == null ) {
			return;
		}
		settings.add(new SettingValueBean(providerId, instanceId,
				CsvDatumDataSourceConfig.JOB_SERVICE_SETTING_PREFIX
						.concat(format("propConfigs[%d].%s", i, key)),
				val.toString()));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CsvPropertyConfig{");
		if ( getColumn() != null ) {
			builder.append("column=");
			builder.append(getColumn());
			builder.append(", ");
		}
		if ( getPropertyKey() != null ) {
			builder.append("property=");
			builder.append(getPropertyKey());
			builder.append(", ");
		}
		if ( getPropertyType() != null ) {
			builder.append("propertyType=");
			builder.append(getPropertyType());
			builder.append(", ");
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public void setConfig(String config) {
		super.setConfig(config);
		this.columnIndexes = CsvDatumDataSourceUtils.columnIndexes(config);
	}

	/**
	 * Get the CSV column reference.
	 * 
	 * <p>
	 * This is an alias for {@link #getConfig()}.
	 * </p>
	 * 
	 * @return the CSV column reference
	 */
	public String getColumn() {
		return getConfig();
	}

	/**
	 * Set the CSV column reference.
	 * 
	 * <p>
	 * This is an alias for {@link #setConfig(String)}.
	 * </p>
	 * 
	 * @param ref
	 *        the CSV column reference to set
	 */
	public void setColumn(String ref) {
		setConfig(ref);
	}

	/**
	 * Get the CSV column indexes.
	 * 
	 * @return the 0-based CSV column indexes, or {@literal null} if not set
	 */
	public int[] getColumnIndexes() {
		return columnIndexes;
	}

}

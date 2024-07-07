/* ==================================================================
 * BacnetDeviceConfig.java - 4/11/2022 1:58:07 pm
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Datum data source configuration for a single BACnet device.
 *
 * @author matt
 * @version 1.0
 */
public class BacnetDeviceConfig {

	/**
	 * A setting type pattern for a device configuration element.
	 *
	 * <p>
	 * The pattern has two capture groups: the device configuration index and
	 * the device setting name.
	 * </p>
	 */
	public static final Pattern PROP_SETTING_PATTERN = Pattern.compile(Pattern
			.quote(JOB_SERVICE_SETTING_PREFIX.concat("deviceConfigs[")).concat("(\\d+)\\]\\.(.*)"));

	private Integer deviceId;
	private BacnetPropertyConfig[] propConfigs;

	/**
	 * Constructor.
	 */
	public BacnetDeviceConfig() {
		super();
	}

	/**
	 * Get settings suitable for configuring this instance.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public List<SettingSpecifier> settings(String prefix) {
		if ( prefix == null ) {
			prefix = "";
		}
		List<SettingSpecifier> results = new ArrayList<>(8);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "deviceId", null));

		// property config list
		BacnetPropertyConfig[] confs = getPropConfigs();
		List<BacnetPropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier(prefix + "propConfigs", confsList,
				new SettingUtils.KeyedListCallback<BacnetPropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(BacnetPropertyConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								BacnetPropertyConfig.settings(key + "."));
						return Collections.singletonList(configGroup);
					}
				}));

		return results;
	}

	/**
	 * Test if this instance represents a valid configuration.
	 *
	 * <p>
	 * This only verifies that the configuration is complete, not that actual
	 * BACnet device and/or properties exist for the configured values.
	 * </p>
	 *
	 * @return {@literal true} if this instance represents a valid configuration
	 */
	public boolean isValid() {
		if ( deviceId == null || deviceId.intValue() < 1 || getPropConfigsCount() < 1 ) {
			return false;
		}
		// verify at least 1 valid prop config
		for ( BacnetPropertyConfig p : propConfigs ) {
			if ( p != null && p.isValid() ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Populate a setting as a device configuration value, if possible.
	 *
	 * @param config
	 *        the overall configuration
	 * @param setting
	 *        the setting to try to handle
	 * @return {@literal true} if the setting was handled as a device
	 *         configuration value
	 */
	public static boolean populateFromSetting(BacnetDatumDataSourceConfig config, Setting setting) {
		Matcher m = PROP_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		int idx = Integer.parseInt(m.group(1));
		String name = m.group(2);
		List<BacnetDeviceConfig> deviceConfigs = config.getDeviceConfigs();
		if ( !(idx < deviceConfigs.size()) ) {
			deviceConfigs.add(idx, new BacnetDeviceConfig());
		}
		BacnetDeviceConfig deviceConfig = deviceConfigs.get(idx);
		if ( BacnetPropertyConfig.populateFromSetting(deviceConfig, setting) ) {
			return true;
		}
		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (name) {
				case "deviceId":
					deviceConfig.setDeviceId(Integer.valueOf(val));
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
	 *        the device index
	 * @return the settings
	 */
	public List<SettingValueBean> toSettingValues(String providerId, String instanceId, final int i) {
		List<SettingValueBean> settings = new ArrayList<>(8);
		addSetting(settings, providerId, instanceId, i, "deviceId", getDeviceId());
		if ( propConfigs != null ) {
			for ( int propIdx = 0, len = propConfigs.length; propIdx < len; propIdx++ ) {
				settings.addAll(
						propConfigs[propIdx].toSettingValues(providerId, instanceId, i, propIdx));
			}
		}
		return settings;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId, String instanceId,
			int i, String key, Object val) {
		if ( val == null ) {
			return;
		}
		settings.add(new SettingValueBean(providerId, instanceId,
				BacnetDatumDataSourceConfig.JOB_SERVICE_SETTING_PREFIX
						.concat(format("deviceConfigs[%d].%s", i, key)),
				val.toString()));
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
	 * Get the property configurations.
	 *
	 * @return the property configurations; never {@literal null}
	 */
	public BacnetPropertyConfig[] getPropConfigs() {
		return propConfigs;
	}

	/**
	 * Set the property configurations to use.
	 *
	 * @param propConfigs
	 *        the configs to use
	 */
	public void setPropConfigs(BacnetPropertyConfig[] propConfigs) {
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 *
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropConfigsCount() {
		BacnetPropertyConfig[] confs = this.propConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propConfigs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link BacnetPropertyConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        the desired number of {@code propConfigs} elements
	 */
	public void setPropConfigsCount(int count) {
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, count,
				BacnetPropertyConfig.class, null);
	}

	/**
	 * Add a new property configuration.
	 *
	 * @param config
	 *        the configuration to add
	 */
	public void addPropConfig(BacnetPropertyConfig config) {
		BacnetPropertyConfig[] configs = new BacnetPropertyConfig[propConfigs != null
				? propConfigs.length + 1
				: 1];
		if ( propConfigs != null ) {
			System.arraycopy(propConfigs, 0, configs, 0, propConfigs.length);
		}
		configs[configs.length - 1] = config;
		setPropConfigs(configs);
	}

}

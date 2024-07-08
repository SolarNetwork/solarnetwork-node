/* ==================================================================
 * BacnetControlConfig.java - 10/11/2022 8:44:28 am
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

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.settings.SettingValueBean;

/**
 * Overall configuration for a BACnet Control.
 *
 * @author matt
 * @version 1.0
 */
public class BacnetControlConfig {

	private String key;
	private String serviceName;
	private String serviceGroup;
	private String bacnetNetworkName;
	private Long sampleCacheMs;

	private final List<BacnetWritePropertyConfig> propertyConfigs = new ArrayList<>(8);

	/**
	 * Constructor.
	 */
	public BacnetControlConfig() {
		super();
	}

	/**
	 * Generate a list of setting values from this instance.
	 *
	 * @param providerId
	 *        the setting provider key to use
	 * @return the list of setting values, never {@literal null}
	 */
	public List<SettingValueBean> toSettingValues(String providerId) {
		List<SettingValueBean> settings = new ArrayList<>(16);
		addSetting(settings, providerId, key, "serviceName", serviceName);
		addSetting(settings, providerId, key, "serviceGroup", serviceGroup);
		addSetting(settings, providerId, key, "bacnetNetworkUid", bacnetNetworkName);
		addSetting(settings, providerId, key, "sampleCacheMs", sampleCacheMs);

		int i = 0;
		for ( BacnetWritePropertyConfig propConfig : propertyConfigs ) {
			settings.addAll(propConfig.toSettingValues(providerId, key, i++));
		}
		return settings;
	}

	/**
	 * Populate a setting as a configuration value, if possible.
	 *
	 * @param setting
	 *        the setting to try to handle
	 * @return {@literal true} if the setting was handled as a configuration
	 *         value
	 */
	public boolean populateFromSetting(Setting setting) {
		if ( BacnetWritePropertyConfig.populateFromSetting(this, setting) ) {
			return true;
		}
		String type = setting.getType();
		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (type) {
				case "serviceName":
					setServiceName(val);
					break;
				case "serviceGroup":
					setServiceGroup(val);
					break;
				case "bacnetNetworkUid":
					setBacnetNetworkName(val);
					break;
				case "sampleCacheMs":
					setSampleCacheMs(Long.valueOf(val));
					break;
				default:
					return false;
			}
		}
		return false;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId, String instanceId,
			String key, Object val) {
		if ( val == null ) {
			return;
		}
		settings.add(new SettingValueBean(providerId, instanceId, key, val.toString()));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BacnetControlConfig{");
		if ( key != null ) {
			builder.append("key=");
			builder.append(key);
			builder.append(", ");
		}
		if ( serviceName != null ) {
			builder.append("serviceName=");
			builder.append(serviceName);
			builder.append(", ");
		}
		if ( serviceGroup != null ) {
			builder.append("serviceGroup=");
			builder.append(serviceGroup);
			builder.append(", ");
		}
		if ( bacnetNetworkName != null ) {
			builder.append("bacnetNetworkName=");
			builder.append(bacnetNetworkName);
			builder.append(", ");
		}
		if ( sampleCacheMs != null ) {
			builder.append("sampleCacheMs=");
			builder.append(sampleCacheMs);
			builder.append(", ");
		}
		if ( propertyConfigs != null ) {
			builder.append("propertyConfigs=");
			builder.append(propertyConfigs);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the instance key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set the instance ID.
	 *
	 * @param key
	 *        the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Get the service name.
	 *
	 * @return the service name
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * Set the service name.
	 *
	 * @param serviceName
	 *        the service name to set
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Get the service group.
	 *
	 * @return the service group
	 */
	public String getServiceGroup() {
		return serviceGroup;
	}

	/**
	 * Set the service group.
	 *
	 * @param serviceGroup
	 *        the service group to set
	 */
	public void setServiceGroup(String serviceGroup) {
		this.serviceGroup = serviceGroup;
	}

	/**
	 * Get the BACnet network name.
	 *
	 * @return the BACnet network name
	 */
	public String getBacnetNetworkName() {
		return bacnetNetworkName;
	}

	/**
	 * Set the BACnet network name.
	 *
	 * @param bacnetNetworkName
	 *        the BACnet network name to set
	 */
	public void setBacnetNetworkName(String bacnetNetworkName) {
		this.bacnetNetworkName = bacnetNetworkName;
	}

	/**
	 * Get the sample cache milliseconds.
	 *
	 * @return the sampleCacheMs
	 */
	public Long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache milliseconds.
	 *
	 * @param sampleCacheMs
	 *        the sampleCacheMs to set
	 */
	public void setSampleCacheMs(Long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Get the property configurations.
	 *
	 * @return the property configurations, never {@literal null}
	 */
	public List<BacnetWritePropertyConfig> getPropertyConfigs() {
		return propertyConfigs;
	}

}

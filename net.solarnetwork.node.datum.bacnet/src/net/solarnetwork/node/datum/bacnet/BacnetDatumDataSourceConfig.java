/* ==================================================================
 * BacnetDatumDataSourceConfig.java - 9/11/2022 9:27:54 am
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

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.settings.SettingValueBean;

/**
 * Overall configuration for a BACnet data source.
 *
 * @author matt
 * @version 1.0
 */
public class BacnetDatumDataSourceConfig {

	/** The setting prefix for data source settings. */
	public static final String JOB_SERVICE_SETTING_PREFIX = "jobService.datumDataSource.";

	private String key;
	private String serviceName;
	private String serviceGroup;
	private String sourceId;
	private String schedule;
	private String bacnetNetworkName;
	private BacnetDatumMode datumMode;
	private Long sampleCacheMs;
	private final List<BacnetDeviceConfig> deviceConfigs = new ArrayList<>(8);

	/**
	 * Constructor.
	 */
	public BacnetDatumDataSourceConfig() {
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
		if ( schedule != null ) {
			settings.add(new SettingValueBean(providerId, key, "schedule", schedule));
		}
		addSetting(settings, providerId, key, "serviceName", serviceName);
		addSetting(settings, providerId, key, "serviceGroup", serviceGroup);
		addSetting(settings, providerId, key, "sourceId", sourceId);
		addSetting(settings, providerId, key, "bacnetNetworkUid", bacnetNetworkName);
		addSetting(settings, providerId, key, "datumModeValue",
				(datumMode != null ? datumMode.name() : null));
		addSetting(settings, providerId, key, "sampleCacheMs", sampleCacheMs);

		int i = 0;
		for ( BacnetDeviceConfig deviceConfig : deviceConfigs ) {
			settings.addAll(deviceConfig.toSettingValues(providerId, key, i++));
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
		if ( "schedule".equals(setting.getType()) ) {
			setSchedule(setting.getValue());
			return true;
		}
		if ( BacnetDeviceConfig.populateFromSetting(this, setting) ) {
			return true;
		}
		if ( setting.getType().startsWith(JOB_SERVICE_SETTING_PREFIX) ) {
			String type = setting.getType().substring(JOB_SERVICE_SETTING_PREFIX.length());
			String val = setting.getValue();
			if ( val != null && !val.isEmpty() ) {
				switch (type) {
					case "serviceName":
						setServiceName(val);
						break;
					case "serviceGroup":
						setServiceGroup(val);
						break;
					case "sourceId":
						setSourceId(val);
						break;
					case "bacnetNetworkUid":
						setBacnetNetworkName(val);
						break;
					case "datumModeValue":
						setDatumModeValue(val);
						break;
					case "sampleCacheMs":
						setSampleCacheMs(Long.valueOf(val));
						break;
					default:
						// ignore
				}
			}
			return true;
		}
		return false;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId, String instanceId,
			String key, Object val) {
		if ( val == null ) {
			return;
		}
		settings.add(new SettingValueBean(providerId, instanceId, JOB_SERVICE_SETTING_PREFIX.concat(key),
				val.toString()));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BacnetDatumDataSourceConfig{");
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
		if ( sourceId != null ) {
			builder.append("sourceId=");
			builder.append(sourceId);
			builder.append(", ");
		}
		if ( schedule != null ) {
			builder.append("schedule=");
			builder.append(schedule);
			builder.append(", ");
		}
		if ( bacnetNetworkName != null ) {
			builder.append("bacnetNetworkName=");
			builder.append(bacnetNetworkName);
			builder.append(", ");
		}
		if ( datumMode != null ) {
			builder.append("datumMode=");
			builder.append(datumMode);
			builder.append(", ");
		}
		if ( sampleCacheMs != null ) {
			builder.append("sampleCacheMs=");
			builder.append(sampleCacheMs);
			builder.append(", ");
		}
		if ( deviceConfigs != null ) {
			builder.append("deviceConfigs=");
			builder.append(deviceConfigs);
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
	 * Get the source ID.
	 *
	 * @return the sourceId
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID.
	 *
	 * @param sourceId
	 *        the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the schedule.
	 *
	 * @return the schedule
	 */
	public String getSchedule() {
		return schedule;
	}

	/**
	 * Set the schedule.
	 *
	 * @param schedule
	 *        the schedule to set
	 */
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	/**
	 * Get the BACnet network name.
	 *
	 * @return the bacnetNetworkName
	 */
	public String getBacnetNetworkName() {
		return bacnetNetworkName;
	}

	/**
	 * Set the BACnet network name.
	 *
	 * @param bacnetNetworkName
	 *        the bacnetNetworkName to set
	 */
	public void setBacnetNetworkName(String bacnetNetworkName) {
		this.bacnetNetworkName = bacnetNetworkName;
	}

	/**
	 * Get the datum mode.
	 *
	 * @return the datum mode
	 */
	public BacnetDatumMode getDatumMode() {
		return datumMode;
	}

	/**
	 * Set the datum mode.
	 *
	 * @param datumMode
	 *        the datum mode to set
	 */
	public void setDatumMode(BacnetDatumMode datumMode) {
		this.datumMode = datumMode;
	}

	/**
	 * Get the datum mode as a string value.
	 *
	 * @return the datum mode value
	 */
	public String getDatumModeValue() {
		final BacnetDatumMode mode = getDatumMode();
		return (mode != null ? mode.name() : null);
	}

	/**
	 * Set the datum mode as a string value.
	 *
	 * @param value
	 *        the value to set
	 */
	public void setDatumModeValue(String value) {
		BacnetDatumMode mode = null;
		try {
			mode = BacnetDatumMode.valueOf(value);
		} catch ( Exception e ) {
			// ignore
		}
		setDatumMode(mode);
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
	 * Get the device configurations.
	 *
	 * @return the configurations, never {@literal null}
	 */
	public List<BacnetDeviceConfig> getDeviceConfigs() {
		return deviceConfigs;
	}

}

/* ==================================================================
 * BaseDatumDataSourceConfig.java - 30/09/2022 12:05:38 pm
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

package net.solarnetwork.node.datum.mbus;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.settings.SettingValueBean;

/**
 * Abstract base class for M-Bus data source configurations.
 *
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public abstract class BaseDatumDataSourceConfig {

	/** The setting prefix for data source settings. */
	public static final String JOB_SERVICE_SETTING_PREFIX = "jobService.datumDataSource.";

	private String key;
	private String serviceName;
	private String serviceGroup;
	private String sourceId;
	private String schedule;
	private String networkName;
	private final List<MBusPropertyConfig> propertyConfigs = new ArrayList<>(8);

	/**
	 * Constructor.
	 */
	public BaseDatumDataSourceConfig() {
		super();
	}

	/**
	 * Generate a list of setting values from this instance.
	 *
	 * <p>
	 * Property settings are not included by this method. See
	 * {@link #addPropertySettingValues(List, String)} for that.
	 * </p>
	 *
	 * @param providerId
	 *        the setting provider key to use
	 * @return the list of setting values, never {@literal null}
	 */
	public List<SettingValueBean> toSettingValues(String providerId) {
		List<SettingValueBean> settings = new ArrayList<>(16);
		settings.add(new SettingValueBean(providerId, key, "schedule", schedule));
		addJobSetting(settings, providerId, key, "uid", serviceName);
		addJobSetting(settings, providerId, key, "groupUid", serviceGroup);
		addJobSetting(settings, providerId, key, "sourceId", sourceId);
		addJobSetting(settings, providerId, key, "sourceId", sourceId);
		addJobSetting(settings, providerId, key, "mBusNetwork.propertyFilters['uid']", networkName);
		return settings;
	}

	/**
	 * Add all property setting values to a settings list.
	 *
	 * @param settings
	 *        the settings to add to
	 * @param providerId
	 *        the provider ID to use
	 */
	protected void addPropertySettingValues(List<SettingValueBean> settings, String providerId) {
		int i = 0;
		for ( MBusPropertyConfig propConfig : propertyConfigs ) {
			settings.addAll(propConfig.toSettingValues(providerId, key, i++));
		}
	}

	/**
	 * Add a job-service setting.
	 *
	 * @param settings
	 *        the settings to add the new setting to
	 * @param providerId
	 *        the provider ID
	 * @param instanceId
	 *        the instance ID
	 * @param key
	 *        the setting key
	 * @param val
	 *        the setting default value
	 */
	public static void addJobSetting(List<SettingValueBean> settings, String providerId,
			String instanceId, String key, Object val) {
		if ( val == null ) {
			return;
		}
		settings.add(new SettingValueBean(providerId, instanceId, JOB_SERVICE_SETTING_PREFIX.concat(key),
				val != null ? val.toString() : null));
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
	 * Get the network name.
	 *
	 * @return the name
	 */
	public String getNetworkName() {
		return networkName;
	}

	/**
	 * Set the network name.
	 *
	 * @param networkName
	 *        the name to set
	 */
	public void setNetworkName(String networkName) {
		this.networkName = networkName;
	}

	/**
	 * Get the property configurations.
	 *
	 * @return the configurations, never {@literal null}
	 */
	public List<MBusPropertyConfig> getPropertyConfigs() {
		return propertyConfigs;
	}

}

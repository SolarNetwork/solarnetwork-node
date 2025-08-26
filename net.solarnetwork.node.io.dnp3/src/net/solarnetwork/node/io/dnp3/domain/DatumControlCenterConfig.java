/* ==================================================================
 * DatumControlCenterConfig.java - 19/08/2025 1:17:18 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.dnp3.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.io.dnp3.impl.DatumControlCenterService;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.util.StringUtils;

/**
 * Overall configuration for a {@link DatumControlCenterService}.
 *
 * @author matt
 * @version 1.0
 */
public class DatumControlCenterConfig {

	/** The setting prefix for data source settings. */
	public static final String JOB_SERVICE_SETTING_PREFIX = "jobService.multiDatumDataSource.";

	private String key;
	private String serviceName;
	private String serviceGroup;
	private String connectionName;
	private Integer address;
	private Set<ClassType> unsolicitedEventClasses;
	private String schedule;
	private final List<DatumConfig> datumConfigs = new ArrayList<>(8);

	/**
	 * Constructor.
	 */
	public DatumControlCenterConfig() {
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
		addSetting(settings, providerId, key, "uid", serviceName);
		addSetting(settings, providerId, key, "groupUid", serviceGroup);
		addSetting(settings, providerId, key, "dnp3Channel.propertyFilters['uid']", connectionName);
		addSetting(settings, providerId, key, "linkLayerConfig.localAddr", address);
		addSetting(settings, providerId, key, "unsolicitedEventClassesValue",
				getUnsolicitedEventClassesValue());
		addSetting(settings, providerId, key, "datumConfigsCount", getDatumConfigsCount());

		int i = 0;
		for ( DatumConfig datumConfig : datumConfigs ) {
			settings.addAll(datumConfig.toSettingValues(providerId, key, i++));
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
		if ( DatumConfig.populateFromSetting(this, setting) ) {
			return true;
		}
		if ( setting.getType().startsWith(JOB_SERVICE_SETTING_PREFIX) ) {
			String type = setting.getType().substring(JOB_SERVICE_SETTING_PREFIX.length());
			String val = setting.getValue();
			if ( val != null && !val.isEmpty() ) {
				switch (type) {
					case "uid":
						setServiceName(val);
						break;
					case "groupUid":
						setServiceGroup(val);
						break;
					case "dnp3Channel.propertyFilters['uid']":
						setConnectionName(val);
						break;
					case "linkLayerConfig.localAddr":
						setAddress(Integer.valueOf(val));
						break;
					case "unsolicitedEventClassesValue":
						setUnsolicitedEventClassesValue(val);
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
		builder.append("DatumControlCenterConfig{");
		if ( key != null ) {
			builder.append("key=");
			builder.append(key);
			builder.append(", ");
		}
		if ( connectionName != null ) {
			builder.append("connectionName=");
			builder.append(connectionName);
			builder.append(", ");
		}
		if ( address != null ) {
			builder.append("address=");
			builder.append(address);
			builder.append(", ");
		}
		if ( unsolicitedEventClasses != null ) {
			builder.append("unsolicitedEventClasses=");
			builder.append(unsolicitedEventClasses);
			builder.append(", ");
		}
		if ( schedule != null ) {
			builder.append("schedule=");
			builder.append(schedule);
			builder.append(", ");
		}
		if ( datumConfigs != null ) {
			builder.append("datumConfigs=");
			builder.append(datumConfigs);
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
	 * Get the connection name.
	 *
	 * @return the connectionName
	 */
	public String getConnectionName() {
		return connectionName;
	}

	/**
	 * Set the connection name.
	 *
	 * @param connectionName
	 *        the connectionName to set
	 */
	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}

	/**
	 * Get the address.
	 *
	 * @return the address
	 */
	public Integer getAddress() {
		return address;
	}

	/**
	 * Set the address.
	 *
	 * @param address
	 *        the address to set
	 */
	public void setAddress(Integer address) {
		this.address = address;
	}

	/**
	 * Get the event classes.
	 *
	 * @return the event classes
	 */
	public Set<ClassType> getUnsolicitedEventClasses() {
		return unsolicitedEventClasses;
	}

	/**
	 * Set the event classes.
	 *
	 * @param unsolicitedEventClasses
	 *        the event classes to set
	 */
	public void setUnsolicitedEventClasses(Set<ClassType> unsolicitedEventClasses) {
		this.unsolicitedEventClasses = unsolicitedEventClasses;
	}

	/**
	 * Get the unsolicited event classes to support as a comma-delimited list of
	 * class type codes.
	 *
	 * @return the comma-delimited list of class type codes
	 */
	public String getUnsolicitedEventClassesValue() {
		final Set<ClassType> classes = getUnsolicitedEventClasses();
		if ( classes == null || classes.isEmpty() ) {
			return null;
		}
		return StringUtils.commaDelimitedStringFromCollection(
				classes.stream().map(c -> String.valueOf(c.getCode())).toList());
	}

	/**
	 * Set the unsolicited event classes to support as a comma-delimited list of
	 * class type codes.
	 *
	 * @param value
	 *        the classes as a comma-delimited list of class type codes, or
	 *        {@code null} or empty set to disable unsolicited events
	 */
	public void setUnsolicitedEventClassesValue(String value) {
		final Set<String> set = StringUtils.commaDelimitedStringToSet(value);
		final Set<ClassType> classes = new TreeSet<>();
		if ( set != null ) {
			for ( String classTypeValue : set ) {
				try {
					ClassType classType = ClassType.forValue(classTypeValue);
					if ( classType != ClassType.Static ) {
						classes.add(classType);
					}
				} catch ( IllegalArgumentException e ) {
					// ignore and continue
				}
			}
		}
		setUnsolicitedEventClasses(classes.isEmpty() ? null : classes);
	}

	/**
	 * Get the datum configurations.
	 *
	 * @return the datum configurations
	 */
	public List<DatumConfig> getDatumConfigs() {
		return datumConfigs;
	}

	/**
	 * Get the number of configured {@code datumConfigs} elements.
	 *
	 * @return the number of {@code datumConfigs} elements
	 */
	public int getDatumConfigsCount() {
		return datumConfigs.size();
	}

}

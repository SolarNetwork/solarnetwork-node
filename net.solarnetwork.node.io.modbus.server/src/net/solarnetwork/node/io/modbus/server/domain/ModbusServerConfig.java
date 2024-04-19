/* ==================================================================
 * ModbusServerConfig.java - 9/03/2022 1:48:03 PM
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

package net.solarnetwork.node.io.modbus.server.domain;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.settings.SettingValueBean;

/**
 * Overall configuration for a Modbus data source.
 *
 * @author matt
 * @version 1.1
 * @since 2.2
 */
public class ModbusServerConfig {

	private String key;
	private String bindAddress;
	private Integer port;
	private Long requestThrottle;
	private final List<UnitConfig> unitConfigs = new ArrayList<>(8);

	/**
	 * Constructor.
	 */
	public ModbusServerConfig() {
		super();
	}

	/**
	 * Get the unit configuration for a given unit ID.
	 *
	 * @param unitId
	 *        the ID to look for
	 * @return the unit configuration, or {@literal null} if not found
	 * @since 2.2
	 */
	public UnitConfig unitConfig(int unitId) {
		return unitConfigs.stream().filter(c -> c.getUnitId() == unitId).findAny().orElse(null);
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
		addSetting(settings, providerId, key, "bindAddress", bindAddress);
		addSetting(settings, providerId, key, "port", port);
		addSetting(settings, providerId, key, "requestThrottle", requestThrottle);
		addSetting(settings, providerId, key, "unitConfigsCount", getUnitConfigsCount());

		int i = 0;
		for ( UnitConfig propConfig : unitConfigs ) {
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
		if ( UnitConfig.populateFromSetting(this, setting) ) {
			return true;
		}
		String type = setting.getType();
		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (type) {
				case "bindAddress":
					setBindAddress(val);
					break;
				case "port":
					setPort(Integer.valueOf(val));
					break;
				case "requestThrottle":
					setRequestThrottle(Long.valueOf(val));
					break;
				default:
					// ignore
			}
		}
		return true;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId, String instanceId,
			String key, Object val) {
		if ( val == null ) {
			return;
		}
		settings.add(
				new SettingValueBean(providerId, instanceId, key, val != null ? val.toString() : ""));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ModbusServerConfig{");
		if ( key != null ) {
			builder.append("key=");
			builder.append(key);
			builder.append(", ");
		}
		if ( bindAddress != null ) {
			builder.append("bindAddress=");
			builder.append(bindAddress);
			builder.append(", ");
		}
		if ( port != null ) {
			builder.append("port=");
			builder.append(port);
			builder.append(", ");
		}
		if ( requestThrottle != null ) {
			builder.append("requestThrottle=");
			builder.append(requestThrottle);
			builder.append(", ");
		}
		if ( unitConfigs != null ) {
			builder.append("unitConfigs=");
			builder.append(unitConfigs);
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
	 * Get the bind address.
	 *
	 * @return the bind address
	 */
	public String getBindAddress() {
		return bindAddress;
	}

	/**
	 * Set the bind address.
	 *
	 * @param bindAddress
	 *        the bind address to set
	 */
	public void setBindAddress(String bindAddress) {
		this.bindAddress = bindAddress;
	}

	/**
	 * Get the listen port.
	 *
	 * @return the port
	 */
	public Integer getPort() {
		return port;
	}

	/**
	 * Set the listen port.
	 *
	 * @param port
	 *        the port to set
	 */
	public void setPort(Integer port) {
		this.port = port;
	}

	/**
	 * Get the request throttle.
	 *
	 * @return the throttle, in milliseconds
	 */
	public Long getRequestThrottle() {
		return requestThrottle;
	}

	/**
	 * Set the request throttle.
	 *
	 * @param requestThrottle
	 *        the throttle to set, in milliseconds
	 */
	public void setRequestThrottle(Long requestThrottle) {
		this.requestThrottle = requestThrottle;
	}

	/**
	 * Get the unit configurations.
	 *
	 * @return the configurations, never {@literal null}
	 */
	public List<UnitConfig> getUnitConfigs() {
		return unitConfigs;
	}

	/**
	 * Get the number of configured {@code unitConfigs} elements.
	 *
	 * @return the number of {@code unitConfigs} elements
	 */
	public int getUnitConfigsCount() {
		return unitConfigs.size();
	}
}

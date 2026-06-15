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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.settings.SettingValueBean;

/**
 * Overall configuration for a Modbus data source.
 *
 * @author matt
 * @version 1.3
 * @since 2.2
 */
public class ModbusServerConfig {

	private @Nullable String key;
	private @Nullable String bindAddress;
	private @Nullable Integer port;
	private @Nullable Long requestThrottle;
	private final Map<String, String> meta = new LinkedHashMap<>(8);
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
	 * @return the unit configuration, or {@code null} if not found
	 * @since 2.2
	 */
	public @Nullable UnitConfig unitConfig(int unitId) {
		return unitConfigs.stream().filter(c -> c.getUnitId() == unitId).findAny().orElse(null);
	}

	/**
	 * Generate a list of setting values from this instance.
	 *
	 * @param providerId
	 *        the setting provider key to use
	 * @return the list of setting values, never {@code null}
	 */
	public List<SettingValueBean> toSettingValues(String providerId) {
		List<SettingValueBean> settings = new ArrayList<>(16);
		addSetting(settings, providerId, key, "bindAddress", bindAddress);
		addSetting(settings, providerId, key, "port", port);
		addSetting(settings, providerId, key, "requestThrottle", requestThrottle);
		addSetting(settings, providerId, key, "unitConfigsCount", getUnitConfigsCount());

		for ( Entry<String, String> e : meta.entrySet() ) {
			addSetting(settings, providerId, key, e.getKey(), e.getValue());
		}

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
					meta.put(type, val);
					break;
			}
		}
		return true;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId,
			@Nullable String instanceId, String key, @Nullable Object val) {
		if ( val == null ) {
			return;
		}
		settings.add(new SettingValueBean(providerId, instanceId, key, val.toString()));
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
		if ( !meta.isEmpty() ) {
			builder.append("meta=");
			builder.append(meta);
			builder.append(", ");
		}
		if ( !unitConfigs.isEmpty() ) {
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
	public final @Nullable String getKey() {
		return key;
	}

	/**
	 * Set the instance ID.
	 *
	 * @param key
	 *        the key to set
	 */
	public final void setKey(@Nullable String key) {
		this.key = key;
	}

	/**
	 * Get the bind address.
	 *
	 * @return the bind address
	 */
	public final @Nullable String getBindAddress() {
		return bindAddress;
	}

	/**
	 * Set the bind address.
	 *
	 * @param bindAddress
	 *        the bind address to set
	 */
	public final void setBindAddress(@Nullable String bindAddress) {
		this.bindAddress = bindAddress;
	}

	/**
	 * Get the listen port.
	 *
	 * @return the port
	 */
	public final @Nullable Integer getPort() {
		return port;
	}

	/**
	 * Set the listen port.
	 *
	 * @param port
	 *        the port to set
	 */
	public final void setPort(@Nullable Integer port) {
		this.port = port;
	}

	/**
	 * Get the request throttle.
	 *
	 * @return the throttle, in milliseconds
	 */
	public final @Nullable Long getRequestThrottle() {
		return requestThrottle;
	}

	/**
	 * Set the request throttle.
	 *
	 * @param requestThrottle
	 *        the throttle to set, in milliseconds
	 */
	public final void setRequestThrottle(@Nullable Long requestThrottle) {
		this.requestThrottle = requestThrottle;
	}

	/**
	 * Get the metadata.
	 *
	 * @return the metadata; never {@code null}
	 * @since 1.3
	 */
	public final Map<String, String> getMeta() {
		return meta;
	}

	/**
	 * Get the unit configurations.
	 *
	 * @return the configurations, never {@code null}
	 */
	public final List<UnitConfig> getUnitConfigs() {
		return unitConfigs;
	}

	/**
	 * Get the number of configured {@code unitConfigs} elements.
	 *
	 * @return the number of {@code unitConfigs} elements
	 */
	public final int getUnitConfigsCount() {
		return unitConfigs.size();
	}
}

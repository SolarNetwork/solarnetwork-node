/* ==================================================================
 * ModbusControlConfig.java - 19/09/2022 5:32:52 pm
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

package net.solarnetwork.node.control.modbus;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;
import net.solarnetwork.node.settings.SettingValueBean;

/**
 * Overall configuration for a Modbus Control.
 *
 * @author matt
 * @version 1.1
 * @since 3.1
 */
public class ModbusControlConfig {

	private @Nullable String key;
	private @Nullable String modbusNetworkName;
	private @Nullable Integer unitId;
	private @Nullable Long sampleCacheMs;
	private @Nullable ModbusWordOrder wordOrder;

	private final List<ModbusWritePropertyConfig> propertyConfigs = new ArrayList<>(8);

	/**
	 * Constructor.
	 */
	public ModbusControlConfig() {
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
		addSetting(settings, providerId, key, "modbusNetwork.propertyFilters['uid']", modbusNetworkName);
		addSetting(settings, providerId, key, "unitId", unitId);
		addSetting(settings, providerId, key, "sampleCacheMs", sampleCacheMs);
		addSetting(settings, providerId, key, "wordOrderKey", getWordOrderKey());

		int i = 0;
		for ( ModbusWritePropertyConfig propConfig : propertyConfigs ) {
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
		if ( ModbusWritePropertyConfig.populateFromSetting(this, setting) ) {
			return true;
		}
		String type = setting.getType();
		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (type) {
				case "modbusNetwork.propertyFilters['uid']":
					setModbusNetworkName(val);
					break;
				case "unitId":
					setUnitId(Integer.valueOf(val));
					break;
				case "sampleCacheMs":
					setSampleCacheMs(Long.valueOf(val));
					break;
				case "wordOrderKey":
					setWordOrderKey(val.charAt(0));
					break;
				default:
					return false;
			}
		}
		return false;
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
		builder.append("ModbusControlConfig{");
		if ( key != null ) {
			builder.append("key=");
			builder.append(key);
			builder.append(", ");
		}
		if ( modbusNetworkName != null ) {
			builder.append("modbusNetworkName=");
			builder.append(modbusNetworkName);
			builder.append(", ");
		}
		if ( unitId != null ) {
			builder.append("unitId=");
			builder.append(unitId);
			builder.append(", ");
		}
		if ( sampleCacheMs != null ) {
			builder.append("sampleCacheMs=");
			builder.append(sampleCacheMs);
			builder.append(", ");
		}
		if ( wordOrder != null ) {
			builder.append("wordOrder=");
			builder.append(wordOrder);
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
	 * Get the modbus network name.
	 *
	 * @return the modbusNetworkName
	 */
	public final @Nullable String getModbusNetworkName() {
		return modbusNetworkName;
	}

	/**
	 * Set the modbus network name.
	 *
	 * @param modbusNetworkName
	 *        the modbusNetworkName to set
	 */
	public final void setModbusNetworkName(@Nullable String modbusNetworkName) {
		this.modbusNetworkName = modbusNetworkName;
	}

	/**
	 * Get the unit ID.
	 *
	 * @return the unitId
	 */
	public final @Nullable Integer getUnitId() {
		return unitId;
	}

	/**
	 * Set the unit ID.
	 *
	 * @param unitId
	 *        the unitId to set
	 */
	public final void setUnitId(@Nullable Integer unitId) {
		this.unitId = unitId;
	}

	/**
	 * Get the sample cache milliseconds.
	 *
	 * @return the sampleCacheMs
	 */
	public final @Nullable Long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache milliseconds.
	 *
	 * @param sampleCacheMs
	 *        the sampleCacheMs to set
	 */
	public final void setSampleCacheMs(@Nullable Long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Get the word order as a key value.
	 *
	 * @return the word order as a key; if {@link #getWordOrder()} is
	 *         {@literal null} then
	 *         {@link ModbusWordOrder#MostToLeastSignificant} will be returned
	 */
	public final char getWordOrderKey() {
		ModbusWordOrder order = getWordOrder();
		if ( order == null ) {
			order = ModbusWordOrder.MostToLeastSignificant;
		}
		return order.getKey();
	}

	/**
	 * Set the word order as a key value.
	 *
	 * @param key
	 *        the word order key to set; if {@code key} is not valid then
	 *        {@link ModbusWordOrder#MostToLeastSignificant} will be set
	 */
	public final void setWordOrderKey(char key) {
		ModbusWordOrder order;
		try {
			order = ModbusWordOrder.forKey(key);
		} catch ( IllegalArgumentException e ) {
			order = ModbusWordOrder.MostToLeastSignificant;
		}
		setWordOrder(order);
	}

	/**
	 * Get the word order.
	 *
	 * @return the wordOrder
	 */
	public final @Nullable ModbusWordOrder getWordOrder() {
		return wordOrder;
	}

	/**
	 * Set the word order.
	 *
	 * @param wordOrder
	 *        the wordOrder to set
	 */
	public final void setWordOrder(@Nullable ModbusWordOrder wordOrder) {
		this.wordOrder = wordOrder;
	}

	/**
	 * Get the property configurations.
	 *
	 * @return the configurations, never {@literal null}
	 */
	public final List<ModbusWritePropertyConfig> getPropertyConfigs() {
		return propertyConfigs;
	}

}

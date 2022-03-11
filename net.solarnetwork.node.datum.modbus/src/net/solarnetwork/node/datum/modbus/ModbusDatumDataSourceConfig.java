/* ==================================================================
 * ModbusDatumDataSourceConfig.java - 9/03/2022 1:48:03 PM
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

package net.solarnetwork.node.datum.modbus;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;
import net.solarnetwork.node.settings.SettingValueBean;

/**
 * Overall configuration for a Modbus data source.
 * 
 * @author matt
 * @version 1.0
 * @since 3.1
 */
public class ModbusDatumDataSourceConfig {

	/** The setting prefix for data source settings. */
	public static final String JOB_SERVICE_SETTING_PREFIX = "jobService.datumDataSource.";

	private String key;
	private String sourceId;
	private String schedule;
	private String modbusNetworkName;
	private Integer unitId;
	private Long sampleCacheMs;
	private Integer maxReadWordCount;
	private ModbusWordOrder wordOrder;
	private final List<ModbusPropertyConfig> propertyConfigs = new ArrayList<>(8);
	private final List<ExpressionConfig> expressionConfigs = new ArrayList<>(8);

	/**
	 * Generate a list of setting values from this instance.
	 * 
	 * @param providerId
	 *        the setting provider key to use
	 * @return the list of setting values, never {@literal null}
	 */
	public List<SettingValueBean> toSettingValues(String providerId) {
		List<SettingValueBean> settings = new ArrayList<>(16);
		settings.add(new SettingValueBean(providerId, key, "schedule", schedule));
		addSetting(settings, providerId, key, "sourceId", sourceId);
		addSetting(settings, providerId, key, "modbusNetwork.propertyFilters['uid']", modbusNetworkName);
		addSetting(settings, providerId, key, "unitId", unitId);
		addSetting(settings, providerId, key, "sampleCacheMs", sampleCacheMs);
		addSetting(settings, providerId, key, "maxReadWordCount", maxReadWordCount);
		addSetting(settings, providerId, key, "wordOrderKey", getWordOrderKey());

		int i = 0;
		for ( ModbusPropertyConfig propConfig : propertyConfigs ) {
			settings.addAll(propConfig.toSettingValues(providerId, key, i++));
		}
		i = 0;
		for ( ExpressionConfig exprConfig : expressionConfigs ) {
			settings.addAll(exprConfig.toSettingValues(providerId, key, i++));
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
		if ( ExpressionConfig.populateFromSetting(this, setting) ) {
			return true;
		} else if ( ModbusPropertyConfig.populateFromSetting(this, setting) ) {
			return true;
		}
		if ( setting.getType().startsWith(JOB_SERVICE_SETTING_PREFIX) ) {
			String type = setting.getType().substring(JOB_SERVICE_SETTING_PREFIX.length());
			String val = setting.getValue();
			if ( val != null && !val.isEmpty() ) {
				switch (type) {
					case "sourceId":
						setSourceId(val);
						break;
					case "modbusNetwork.propertyFilters['uid']":
						setModbusNetworkName(val);
						break;
					case "unitId":
						setUnitId(Integer.valueOf(val));
						break;
					case "sampleCacheMs":
						setSampleCacheMs(Long.valueOf(val));
						break;
					case "maxReadWordCount":
						setMaxReadWordCount(Integer.valueOf(val));
						break;
					case "wordOrderKey":
						setWordOrderKey(val.charAt(0));
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
		builder.append("ModbusDatumDataSourceConfig{");
		if ( key != null ) {
			builder.append("key=");
			builder.append(key);
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
		if ( maxReadWordCount != null ) {
			builder.append("maxReadWordCount=");
			builder.append(maxReadWordCount);
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
	 * Get the modbus network name.
	 * 
	 * @return the modbusNetworkName
	 */
	public String getModbusNetworkName() {
		return modbusNetworkName;
	}

	/**
	 * Set the modbus network name.
	 * 
	 * @param modbusNetworkName
	 *        the modbusNetworkName to set
	 */
	public void setModbusNetworkName(String modbusNetworkName) {
		this.modbusNetworkName = modbusNetworkName;
	}

	/**
	 * Get the unit ID.
	 * 
	 * @return the unitId
	 */
	public Integer getUnitId() {
		return unitId;
	}

	/**
	 * Set the unit ID.
	 * 
	 * @param unitId
	 *        the unitId to set
	 */
	public void setUnitId(Integer unitId) {
		this.unitId = unitId;
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
	 * Get the max read word count.
	 * 
	 * @return the maxReadWordCount
	 */
	public Integer getMaxReadWordCount() {
		return maxReadWordCount;
	}

	/**
	 * Set the max read word count.
	 * 
	 * @param maxReadWordCount
	 *        the maxReadWordCount to set
	 */
	public void setMaxReadWordCount(Integer maxReadWordCount) {
		this.maxReadWordCount = maxReadWordCount;
	}

	/**
	 * Get the word order as a key value.
	 * 
	 * @return the word order as a key; if {@link #getWordOrder()} is
	 *         {@literal null} then
	 *         {@link ModbusWordOrder#MostToLeastSignificant} will be returned
	 */
	public char getWordOrderKey() {
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
	public void setWordOrderKey(char key) {
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
	public ModbusWordOrder getWordOrder() {
		return wordOrder;
	}

	/**
	 * Set the word order.
	 * 
	 * @param wordOrder
	 *        the wordOrder to set
	 */
	public void setWordOrder(ModbusWordOrder wordOrder) {
		this.wordOrder = wordOrder;
	}

	/**
	 * Get the property configurations.
	 * 
	 * @return the configurations, never {@literal null}
	 */
	public List<ModbusPropertyConfig> getPropertyConfigs() {
		return propertyConfigs;
	}

	/**
	 * Get the expression configurations.
	 * 
	 * @return the configurations, never {@literal null}
	 */
	public List<ExpressionConfig> getExpressionConfigs() {
		return expressionConfigs;
	}

}

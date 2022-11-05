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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

	private Integer deviceId;
	private BacnetPropertyConfig[] propConfigs;

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

}

/* ==================================================================
 * MBusDatumDataSourceConfig.java - 30/09/2022 12:03:15 pm
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

import java.util.List;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.settings.SettingValueBean;

/**
 * Overall configuration for a M-Bus data source.
 *
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public class MBusDatumDataSourceConfig extends BaseDatumDataSourceConfig {

	private Integer address;
	private Long sampleCacheMs;

	/**
	 * Constructor.
	 */
	public MBusDatumDataSourceConfig() {
		super();
	}

	@Override
	public List<SettingValueBean> toSettingValues(String providerId) {
		List<SettingValueBean> settings = super.toSettingValues(providerId);
		final String key = getKey();
		addJobSetting(settings, providerId, key, "address", address);
		addJobSetting(settings, providerId, key, "sampleCacheMs", sampleCacheMs);
		addPropertySettingValues(settings, providerId);
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
		if ( MBusPropertyConfig.populateFromSetting(this, setting) ) {
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
					case "uid":
						setServiceName(val);
						break;
					case "groupUid":
						setServiceGroup(val);
						break;
					case "mBusNetwork.propertyFilters['uid']":
						setNetworkName(val);
						break;
					case "address":
						setAddress(Integer.valueOf(val));
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

	/**
	 * Get the M-Bus primary address.
	 *
	 * @return the address
	 */
	public Integer getAddress() {
		return address;
	}

	/**
	 * Set the M-Bus primary address.
	 *
	 * @param address
	 *        the address to set (1-255)
	 */
	public void setAddress(Integer address) {
		this.address = address;
	}

	/**
	 * Get the sample cache milliseconds.
	 *
	 * @return the cache milliseconds
	 */
	public Long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache milliseconds.
	 *
	 * @param sampleCacheMs
	 *        the cache milliseconds to set
	 */
	public void setSampleCacheMs(Long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

}

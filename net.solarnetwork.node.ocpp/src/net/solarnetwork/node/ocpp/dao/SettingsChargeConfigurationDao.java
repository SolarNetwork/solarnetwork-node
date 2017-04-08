/* ==================================================================
 * SettingsChargeConfigurationDao.java - 25/03/2017 11:53:17 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.ocpp.ChargeConfiguration;
import net.solarnetwork.node.ocpp.ChargeConfigurationDao;
import net.solarnetwork.node.ocpp.support.SimpleChargeConfiguration;
import net.solarnetwork.node.support.KeyValuePair;
import net.solarnetwork.util.OptionalService;
import ocpp.v15.support.ConfigurationKeys;

/**
 * Implementation of {@link ChargeConfigurationDao} that uses {@link SettingDao}
 * for persistence.
 * 
 * All configuration properties are stored using a single {@code key} value of
 * {@link #SETTING_KEY} and the {@code type} values are derived from the
 * {@link ConfigurationKeys#getKey()} values.
 * 
 * @author matt
 * @version 1.0
 * @since 0.6
 */
public class SettingsChargeConfigurationDao implements ChargeConfigurationDao {

	/** A setting key constant used for all configuration properties. */
	public static final String SETTING_KEY = "ocpp-conf";

	private final SettingDao dao;
	private final OptionalService<EventAdmin> eventAdmin;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param dao
	 *        The DAO to persist all configuration in.
	 */
	public SettingsChargeConfigurationDao(SettingDao dao) {
		this(dao, null);
	}

	/**
	 * Constructor with {@code EventAmdin}.
	 * 
	 * @param dao
	 *        The DAO to persist all configuration in.
	 * @param eventAdmin
	 *        An optional {@code EventAdmin} service (may be {@code null}).
	 */
	public SettingsChargeConfigurationDao(SettingDao dao, OptionalService<EventAdmin> eventAdmin) {
		super();
		this.dao = dao;
		this.eventAdmin = eventAdmin;
	}

	@Override
	public void storeChargeConfiguration(ChargeConfiguration config) {
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(ConfigurationKeys.HeartBeatInterval.getKey(), config.getHeartBeatInterval());
		dao.storeSetting(SETTING_KEY, ConfigurationKeys.HeartBeatInterval.getKey(),
				String.valueOf(config.getHeartBeatInterval()));
		props.put(ConfigurationKeys.MeterValueSampleInterval.getKey(),
				config.getMeterValueSampleInterval());
		dao.storeSetting(SETTING_KEY, ConfigurationKeys.MeterValueSampleInterval.getKey(),
				String.valueOf(config.getMeterValueSampleInterval()));
		postEvent(EVENT_TOPIC_CHARGE_CONFIGURATION_UPDATED, props);
	}

	private void postEvent(String topic, Map<String, Object> props) {
		final EventAdmin admin = (eventAdmin != null ? eventAdmin.service() : null);
		if ( admin == null ) {
			return;
		}
		admin.postEvent(new Event(topic, props));
	}

	@Override
	public ChargeConfiguration getChargeConfiguration() {
		SimpleChargeConfiguration config = new SimpleChargeConfiguration();
		List<KeyValuePair> settings = dao.getSettings(SETTING_KEY);
		if ( settings != null ) {
			for ( KeyValuePair kv : settings ) {
				if ( kv.getValue() == null || kv.getValue().isEmpty() ) {
					continue;
				}
				try {
					if ( ConfigurationKeys.HeartBeatInterval.getKey().equals(kv.getKey()) ) {
						config.setHeartBeatInterval(Integer.parseInt(kv.getValue()));
					} else if ( ConfigurationKeys.MeterValueSampleInterval.getKey()
							.equals(kv.getKey()) ) {
						config.setMeterValueSampleInterval(Integer.parseInt(kv.getValue()));
					}
				} catch ( NumberFormatException e ) {
					log.warn("Unexpected number value for OCPP configuration {}: {}", kv.getKey(),
							e.getMessage());
				}
			}
		}
		return config;
	}

}

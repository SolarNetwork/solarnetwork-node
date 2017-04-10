/* ==================================================================
 * SettingsChargeConfigurationDaoTests.java - 25/03/2017 12:14:57 PM
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

package net.solarnetwork.node.ocpp.dao.test;

import javax.annotation.Resource;
import javax.sql.DataSource;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.JdbcSettingDao;
import net.solarnetwork.node.ocpp.ChargeConfiguration;
import net.solarnetwork.node.ocpp.dao.SettingsChargeConfigurationDao;
import net.solarnetwork.node.ocpp.support.SimpleChargeConfiguration;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.util.StaticOptionalService;
import ocpp.v15.support.ConfigurationKeys;

/**
 * Unit tests for the {@link SettingsChargeConfigurationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SettingsChargeConfigurationDaoTests extends AbstractNodeTransactionalTest {

	private static final int TEST_HEART_BEAT_INTERVAL = 600;
	private static final int TEST_METER_VALUE_SAMPLE_INTERVAL = 30;

	@Resource(name = "dataSource")
	private DataSource dataSource;

	private JdbcSettingDao settingDao;

	private SettingsChargeConfigurationDao dao;

	private EventAdmin eventAdmin;
	private ChargeConfiguration lastConfig;

	@Before
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		settingDao = new JdbcSettingDao();
		settingDao.setDataSource(dataSource);
		settingDao.afterPropertiesSet();

		eventAdmin = EasyMock.createMock(EventAdmin.class);

		dao = new SettingsChargeConfigurationDao(settingDao,
				new StaticOptionalService<EventAdmin>(eventAdmin));
	}

	@After
	public void finish() {
		EasyMock.verify(eventAdmin);
	}

	private void replayAll() {
		EasyMock.replay(eventAdmin);
	}

	@Test
	public void insert() {
		Capture<Event> eventCapture = new Capture<Event>();
		eventAdmin.postEvent(EasyMock.capture(eventCapture));
		replayAll();

		SimpleChargeConfiguration config = new SimpleChargeConfiguration();
		config.setHeartBeatInterval(TEST_HEART_BEAT_INTERVAL);
		config.setMeterValueSampleInterval(TEST_METER_VALUE_SAMPLE_INTERVAL);
		dao.storeChargeConfiguration(config);

		Event event = eventCapture.getValue();
		Assert.assertNotNull("Updated event posted", event);
		Assert.assertEquals(Integer.valueOf(TEST_HEART_BEAT_INTERVAL),
				event.getProperty(ConfigurationKeys.HeartBeatInterval.getKey()));
		Assert.assertEquals(Integer.valueOf(TEST_METER_VALUE_SAMPLE_INTERVAL),
				event.getProperty(ConfigurationKeys.MeterValueSampleInterval.getKey()));

		lastConfig = config;
	}

	@Test
	public void get() {
		insert();
		ChargeConfiguration config = dao.getChargeConfiguration();
		Assert.assertNotNull("ChargeConfiguration inserted", config);
		Assert.assertEquals("HeartBeatInterval", TEST_HEART_BEAT_INTERVAL,
				config.getHeartBeatInterval());
		Assert.assertEquals("MeterValueSampleInterval", TEST_METER_VALUE_SAMPLE_INTERVAL,
				config.getMeterValueSampleInterval());
	}

	@Test
	public void update() {
		insert();

		EasyMock.reset(eventAdmin);
		Capture<Event> eventCapture = new Capture<Event>();
		eventAdmin.postEvent(EasyMock.capture(eventCapture));
		replayAll();

		SimpleChargeConfiguration update = new SimpleChargeConfiguration(lastConfig);
		update.setHeartBeatInterval(1);
		update.setMeterValueSampleInterval(2);
		dao.storeChargeConfiguration(update);
		ChargeConfiguration updated = dao.getChargeConfiguration();
		Assert.assertEquals("Updated heart beat interval", update.getHeartBeatInterval(),
				updated.getHeartBeatInterval());
		Assert.assertEquals("Updated meter value sample interval", update.getMeterValueSampleInterval(),
				updated.getMeterValueSampleInterval());

		Event event = eventCapture.getValue();
		Assert.assertNotNull("Updated event posted", event);
		Assert.assertEquals(Integer.valueOf(1),
				event.getProperty(ConfigurationKeys.HeartBeatInterval.getKey()));
		Assert.assertEquals(Integer.valueOf(2),
				event.getProperty(ConfigurationKeys.MeterValueSampleInterval.getKey()));

	}

}

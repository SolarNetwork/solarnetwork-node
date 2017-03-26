/* ==================================================================
 * ChargePointService_v15Tests.java - 26/03/2017 8:19:32 AM
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

package net.solarnetwork.node.ocpp.web.test;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.ocpp.ChargeConfigurationDao;
import net.solarnetwork.node.ocpp.ChargeSessionManager;
import net.solarnetwork.node.ocpp.support.SimpleChargeConfiguration;
import net.solarnetwork.node.ocpp.web.ChargePointService_v15;
import ocpp.v15.cp.AvailabilityStatus;
import ocpp.v15.cp.AvailabilityType;
import ocpp.v15.cp.ChangeAvailabilityRequest;
import ocpp.v15.cp.ChangeAvailabilityResponse;
import ocpp.v15.cp.ChangeConfigurationRequest;
import ocpp.v15.cp.ChangeConfigurationResponse;
import ocpp.v15.cp.ConfigurationStatus;
import ocpp.v15.cp.GetConfigurationRequest;
import ocpp.v15.cp.GetConfigurationResponse;
import ocpp.v15.cp.KeyValue;
import ocpp.v15.support.ConfigurationKeys;

/**
 * Unit tests for the {@link ChargePointService_v15} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ChargePointService_v15Tests {

	private static final String TEST_CHARGE_BOX_ID = "test.id";
	private static final String TEST_SOCKET_ID = "/test/socket";

	private ChargePointService_v15 service;

	private ChargeConfigurationDao chargeConfigurationDao;
	private ChargeSessionManager chargeSessionManager;
	private MessageSource messageSource;

	@Before
	public void setup() {
		chargeConfigurationDao = EasyMock.createMock(ChargeConfigurationDao.class);
		chargeSessionManager = EasyMock.createMock(ChargeSessionManager.class);
		messageSource = EasyMock.createMock(MessageSource.class);

		service = new ChargePointService_v15();
		service.setChargeConfigurationDao(chargeConfigurationDao);
		service.setChargeSessionManager(chargeSessionManager);
		service.setChargeSessionManager(chargeSessionManager);
		service.setMessageSource(messageSource);
	}

	@After
	public void finished() {
		EasyMock.verify(chargeConfigurationDao, chargeSessionManager, messageSource);
	}

	private void replayAll() {
		EasyMock.replay(chargeConfigurationDao, chargeSessionManager, messageSource);
	}

	@Test
	public void getAllConfigurationValues() {
		SimpleChargeConfiguration config = new SimpleChargeConfiguration();
		config.setHeartBeatInterval(1);
		config.setMeterValueSampleInterval(2);
		expect(chargeConfigurationDao.getChargeConfiguration()).andReturn(config);
		replayAll();

		GetConfigurationRequest req = new GetConfigurationRequest();
		GetConfigurationResponse resp = service.getConfiguration(req, TEST_CHARGE_BOX_ID);
		assertNotNull("Response available", resp);
		assertEquals("All keys returned", 2, resp.getConfigurationKey().size());
		assertEquals("No unknown keys returned", 0, resp.getUnknownKey().size());
		Map<String, String> props = getConfigurationProperties(resp);
		assertEquals("HeartBeatInterval", "1", props.get(ConfigurationKeys.HeartBeatInterval.getKey()));
		assertEquals("MeterValueSampleInterval", "2",
				props.get(ConfigurationKeys.MeterValueSampleInterval.getKey()));
	}

	private Map<String, String> getConfigurationProperties(GetConfigurationResponse resp) {
		Map<String, String> props = new LinkedHashMap<String, String>(resp.getConfigurationKey().size());
		for ( KeyValue kv : resp.getConfigurationKey() ) {
			props.put(kv.getKey(), kv.getValue());
		}
		return props;
	}

	@Test
	public void getOneConfigurationValue() {
		SimpleChargeConfiguration config = new SimpleChargeConfiguration();
		config.setHeartBeatInterval(1);
		config.setMeterValueSampleInterval(2);
		expect(chargeConfigurationDao.getChargeConfiguration()).andReturn(config);
		replayAll();

		GetConfigurationRequest req = new GetConfigurationRequest();
		req.getKey().add(ConfigurationKeys.MeterValueSampleInterval.getKey());
		GetConfigurationResponse resp = service.getConfiguration(req, TEST_CHARGE_BOX_ID);
		assertNotNull("Response available", resp);
		assertEquals("All keys returned", 1, resp.getConfigurationKey().size());
		assertEquals("No unknown keys returned", 0, resp.getUnknownKey().size());
		Map<String, String> props = getConfigurationProperties(resp);
		assertEquals("MeterValueSampleInterval", "2",
				props.get(ConfigurationKeys.MeterValueSampleInterval.getKey()));
	}

	@Test
	public void changeMeterValueSampleInterval() {
		SimpleChargeConfiguration config = new SimpleChargeConfiguration();
		config.setHeartBeatInterval(0);
		config.setMeterValueSampleInterval(0);
		expect(chargeConfigurationDao.getChargeConfiguration()).andReturn(config);

		// update
		Capture<SimpleChargeConfiguration> configCapture = new Capture<SimpleChargeConfiguration>();
		chargeConfigurationDao.storeChargeConfiguration(EasyMock.capture(configCapture));

		replayAll();

		ChangeConfigurationRequest req = new ChangeConfigurationRequest();
		req.setKey(ConfigurationKeys.MeterValueSampleInterval.getKey());
		req.setValue("100");
		ChangeConfigurationResponse resp = service.changeConfiguration(req, TEST_CHARGE_BOX_ID);
		assertNotNull("Response available", resp);
		assertEquals("Status", ConfigurationStatus.ACCEPTED, resp.getStatus());

		// verify updated config
		assertNotNull("Updated config", configCapture.getValue());
		SimpleChargeConfiguration updatedConfig = configCapture.getValue();
		assertEquals("HeartBeatInterval unchanged", 0, updatedConfig.getHeartBeatInterval());
		assertEquals("MeterValueSampleInterval", 100, updatedConfig.getMeterValueSampleInterval());
	}

	@Test
	public void changeMeterValueSampleIntervalInvalid() {
		SimpleChargeConfiguration config = new SimpleChargeConfiguration();
		config.setHeartBeatInterval(0);
		config.setMeterValueSampleInterval(0);
		expect(chargeConfigurationDao.getChargeConfiguration()).andReturn(config);

		replayAll();

		ChangeConfigurationRequest req = new ChangeConfigurationRequest();
		req.setKey(ConfigurationKeys.MeterValueSampleInterval.getKey());
		req.setValue("ABC");
		ChangeConfigurationResponse resp = service.changeConfiguration(req, TEST_CHARGE_BOX_ID);
		assertNotNull("Response available", resp);
		assertEquals("Status", ConfigurationStatus.REJECTED, resp.getStatus());
	}

	@Test
	public void changeHeartBeatInterval() {
		SimpleChargeConfiguration config = new SimpleChargeConfiguration();
		config.setHeartBeatInterval(0);
		config.setMeterValueSampleInterval(0);
		expect(chargeConfigurationDao.getChargeConfiguration()).andReturn(config);

		// update
		Capture<SimpleChargeConfiguration> configCapture = new Capture<SimpleChargeConfiguration>();
		chargeConfigurationDao.storeChargeConfiguration(EasyMock.capture(configCapture));

		replayAll();

		ChangeConfigurationRequest req = new ChangeConfigurationRequest();
		req.setKey(ConfigurationKeys.HeartBeatInterval.getKey());
		req.setValue("100");
		ChangeConfigurationResponse resp = service.changeConfiguration(req, TEST_CHARGE_BOX_ID);
		assertNotNull("Response available", resp);
		assertEquals("Status", ConfigurationStatus.ACCEPTED, resp.getStatus());

		// verify updated config
		assertNotNull("Updated config", configCapture.getValue());
		SimpleChargeConfiguration updatedConfig = configCapture.getValue();
		assertEquals("HeartBeatInterval", 100, updatedConfig.getHeartBeatInterval());
		assertEquals("MeterValueSampleInterval unchanged", 0,
				updatedConfig.getMeterValueSampleInterval());
	}

	@Test
	public void changeHeartBeatIntervalInvalid() {
		SimpleChargeConfiguration config = new SimpleChargeConfiguration();
		config.setHeartBeatInterval(0);
		config.setMeterValueSampleInterval(0);
		expect(chargeConfigurationDao.getChargeConfiguration()).andReturn(config);

		replayAll();

		ChangeConfigurationRequest req = new ChangeConfigurationRequest();
		req.setKey(ConfigurationKeys.HeartBeatInterval.getKey());
		req.setValue("ABC");
		ChangeConfigurationResponse resp = service.changeConfiguration(req, TEST_CHARGE_BOX_ID);
		assertNotNull("Response available", resp);
		assertEquals("Status", ConfigurationStatus.REJECTED, resp.getStatus());
	}

	@Test
	public void changeAvailabilityAllDisabled() {
		// because passing connector ID 0, must change all sockets
		Set<String> availableSocketIds = new LinkedHashSet<String>(Arrays.asList(TEST_SOCKET_ID));
		expect(chargeSessionManager.availableSocketIds()).andReturn(availableSocketIds);

		// disable the socket
		chargeSessionManager.configureSocketEnabledState(availableSocketIds, false);

		replayAll();
		ChangeAvailabilityRequest req = new ChangeAvailabilityRequest();
		req.setConnectorId(0);
		req.setType(AvailabilityType.INOPERATIVE);
		ChangeAvailabilityResponse resp = service.changeAvailability(req, TEST_CHARGE_BOX_ID);

		assertNotNull("Response available", resp);
		assertEquals("Status", AvailabilityStatus.ACCEPTED, resp.getStatus());
	}

	@Test
	public void changeAvailabilityAllEnabled() {
		// because passing connector ID 0, must change all sockets
		Set<String> availableSocketIds = new LinkedHashSet<String>(Arrays.asList(TEST_SOCKET_ID));
		expect(chargeSessionManager.availableSocketIds()).andReturn(availableSocketIds);

		// disable the socket
		chargeSessionManager.configureSocketEnabledState(availableSocketIds, true);

		replayAll();
		ChangeAvailabilityRequest req = new ChangeAvailabilityRequest();
		req.setConnectorId(0);
		req.setType(AvailabilityType.OPERATIVE);
		ChangeAvailabilityResponse resp = service.changeAvailability(req, TEST_CHARGE_BOX_ID);

		assertNotNull("Response available", resp);
		assertEquals("Status", AvailabilityStatus.ACCEPTED, resp.getStatus());
	}

	@Test
	public void changeAvailabilitySingleConnectorDisabled() {
		expect(chargeSessionManager.socketIdForConnectorId(1)).andReturn(TEST_SOCKET_ID);

		Capture<Collection<String>> socketIdsCapture = new Capture<Collection<String>>();
		chargeSessionManager.configureSocketEnabledState(EasyMock.capture(socketIdsCapture),
				EasyMock.eq(false));

		replayAll();
		ChangeAvailabilityRequest req = new ChangeAvailabilityRequest();
		req.setConnectorId(1);
		req.setType(AvailabilityType.INOPERATIVE);
		ChangeAvailabilityResponse resp = service.changeAvailability(req, TEST_CHARGE_BOX_ID);

		assertNotNull("Response available", resp);
		assertEquals("Status", AvailabilityStatus.ACCEPTED, resp.getStatus());

		// verify captured socket
		Collection<String> socketIds = socketIdsCapture.getValue();
		assertNotNull("Changed socket IDs", socketIds);
		assertEquals("Changed socket IDs count", 1, socketIds.size());
		assertEquals("Changed socket ID", TEST_SOCKET_ID, socketIds.iterator().next());
	}

	@Test
	public void changeAvailabilityUnknownConnectorDisabled() {
		expect(chargeSessionManager.socketIdForConnectorId(1)).andReturn(null);

		replayAll();
		ChangeAvailabilityRequest req = new ChangeAvailabilityRequest();
		req.setConnectorId(1);
		req.setType(AvailabilityType.INOPERATIVE);
		ChangeAvailabilityResponse resp = service.changeAvailability(req, TEST_CHARGE_BOX_ID);

		assertNotNull("Response available", resp);
		assertEquals("Status", AvailabilityStatus.REJECTED, resp.getStatus());
	}

}

/* ==================================================================
 * ChargeSessionManager_v15Tests.java - 11/06/2015 6:47:01 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.charge.test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.EnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.ocpp.AuthorizationManager;
import net.solarnetwork.node.ocpp.CentralSystemServiceFactory;
import net.solarnetwork.node.ocpp.ChargeSession;
import net.solarnetwork.node.ocpp.ChargeSessionDao;
import net.solarnetwork.node.ocpp.charge.ChargeSessionManager_v15;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.node.util.ClassUtils;
import net.solarnetwork.util.StaticOptionalServiceCollection;
import ocpp.v15.AuthorizationStatus;
import ocpp.v15.CentralSystemService;
import ocpp.v15.IdTagInfo;
import ocpp.v15.Measurand;
import ocpp.v15.MeterValue.Value;
import ocpp.v15.ReadingContext;
import ocpp.v15.StartTransactionRequest;
import ocpp.v15.StartTransactionResponse;
import ocpp.v15.UnitOfMeasure;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;

/**
 * Test cases for the {@link ChargeSessionManager_v15} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ChargeSessionManager_v15Tests extends AbstractNodeTest {

	private static final String TEST_CHARGE_BOX_IDENTITY = "test.ident";
	private static final String TEST_METER_SOURCE_ID = "test.meter";
	private static final String TEST_SOCKET_ID = "/socket/test";
	private static final String TEST_ID_TAG = "test.tag";
	private static final Integer TEST_CONNECTOR_ID = 11;
	private static final Integer TEST_TRANSACTION_ID = 22;
	private static final String TEST_SESSION_ID = UUID.randomUUID().toString();

	private AuthorizationManager authManager;
	private CentralSystemServiceFactory centralSystem;
	private CentralSystemService client;
	private ChargeSessionDao chargeSessionDao;
	private DatumDataSource<EnergyDatum> meterDataSource;

	private ChargeSessionManager_v15 manager;

	@SuppressWarnings("unchecked")
	private DatumDataSource<EnergyDatum> newMeterDataSource() {
		return EasyMock.createMock(DatumDataSource.class);
	}

	@Before
	public void setup() {
		authManager = EasyMock.createMock(AuthorizationManager.class);
		client = EasyMock.createMock(CentralSystemService.class);
		centralSystem = new MockCentralSystemServiceFactory("OCPP Central System", null, client,
				TEST_CHARGE_BOX_IDENTITY);
		chargeSessionDao = EasyMock.createMock(ChargeSessionDao.class);

		meterDataSource = newMeterDataSource();

		manager = new ChargeSessionManager_v15();
		manager.setAuthManager(authManager);
		manager.setCentralSystem(centralSystem);
		manager.setChargeSessionDao(chargeSessionDao);
		manager.setMeterDataSource(new StaticOptionalServiceCollection<DatumDataSource<EnergyDatum>>(
				Collections.singletonList(meterDataSource)));
		manager.setSocketConnectorMapping(Collections.singletonMap(TEST_SOCKET_ID, TEST_CONNECTOR_ID));
		manager.setSocketMeterSourceMapping(Collections.singletonMap(TEST_SOCKET_ID,
				TEST_METER_SOURCE_ID));
	}

	@After
	public void finish() {
		EasyMock.verify(authManager, client, chargeSessionDao, meterDataSource);
	}

	private void replayAll() {
		EasyMock.replay(authManager, client, chargeSessionDao, meterDataSource);
	}

	@Test
	public void activeSessionNone() {
		expect(chargeSessionDao.getIncompleteChargeSessionForSocket(TEST_SOCKET_ID)).andReturn(null);
		replayAll();
		ChargeSession session = manager.activeChargeSession(TEST_SOCKET_ID);
		Assert.assertNull("No active session", session);
	}

	@Test
	public void activeSessionMatch() {
		final ChargeSession active = new ChargeSession();
		expect(chargeSessionDao.getIncompleteChargeSessionForSocket(TEST_SOCKET_ID)).andReturn(active);
		replayAll();
		ChargeSession session = manager.activeChargeSession(TEST_SOCKET_ID);
		Assert.assertSame("Active session", active, session);
	}

	private Map<String, Object> datumCapturedEventProperties(EnergyDatum datum) {
		Map<String, Object> m = ClassUtils.getSimpleBeanProperties(datum, null);
		m.put(DatumDataSource.EVENT_DATUM_CAPTURED_DATUM_TYPE, datum.getClass().getSimpleName());
		return m;
	}

	@Test
	public void handleDatumCapturedEventNoSession() {
		final GeneralNodeACEnergyDatum datum = new GeneralNodeACEnergyDatum();
		datum.setCreated(new Date());
		datum.setSourceId(TEST_METER_SOURCE_ID);
		datum.setWatts(100);
		datum.setWattHourReading(1000L);
		final Event captured = new Event(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED,
				datumCapturedEventProperties(datum));

		// first look for active session associated with socket
		expect(chargeSessionDao.getIncompleteChargeSessionForSocket(TEST_SOCKET_ID)).andReturn(null);

		replayAll();
		manager.handleEvent(captured);
	}

	@Test
	public void handleDatumCapturedEventMatchingSession() {
		final GeneralNodeACEnergyDatum datum = new GeneralNodeACEnergyDatum();
		datum.setCreated(new Date());
		datum.setSourceId(TEST_METER_SOURCE_ID);
		datum.setWatts(100);
		datum.setWattHourReading(1000L);
		final Event captured = new Event(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED,
				datumCapturedEventProperties(datum));
		final ChargeSession active = new ChargeSession();
		active.setCreated(new Date());
		active.setSessionId(TEST_SESSION_ID);
		active.setSocketId(TEST_SOCKET_ID);
		active.setStatus(AuthorizationStatus.ACCEPTED);
		active.setTransactionId(TEST_TRANSACTION_ID);

		// first look for active session associated with socket
		expect(chargeSessionDao.getIncompleteChargeSessionForSocket(TEST_SOCKET_ID)).andReturn(active);

		// then store meter reading
		final Capture<Iterable<Value>> readingCapture = new Capture<Iterable<Value>>();
		chargeSessionDao.addMeterReadings(eq(TEST_SESSION_ID), anyObject(Date.class),
				capture(readingCapture));

		replayAll();
		manager.handleEvent(captured);

		Iterable<Value> inserted = readingCapture.getValue();
		Assert.assertNotNull("Inserted readings", inserted);
		Iterator<Value> itr = inserted.iterator();
		Value v = itr.next();
		Assert.assertNotNull("Inserted reading", v);
		Assert.assertEquals("ReadingContext", ReadingContext.SAMPLE_PERIODIC, v.getContext());
		Assert.assertEquals("Measurand", Measurand.ENERGY_ACTIVE_IMPORT_REGISTER, v.getMeasurand());
		Assert.assertEquals("Unit", UnitOfMeasure.WH, v.getUnit());
		Assert.assertEquals("Value", String.valueOf(datum.getWattHourReading()), v.getValue());

		Assert.assertTrue("More readings", itr.hasNext());
		v = itr.next();
		Assert.assertNotNull("Inserted reading", v);
		Assert.assertEquals("ReadingContext", ReadingContext.SAMPLE_PERIODIC, v.getContext());
		Assert.assertEquals("Measurand", Measurand.POWER_ACTIVE_IMPORT, v.getMeasurand());
		Assert.assertEquals("Unit", UnitOfMeasure.W, v.getUnit());
		Assert.assertEquals("Value", String.valueOf(datum.getWatts()), v.getValue());

		Assert.assertFalse("No more readings", itr.hasNext());
	}

	@Test
	public void initiateChargeSession() {
		// verify active session does not exist
		expect(chargeSessionDao.getIncompleteChargeSessionForSocket(TEST_SOCKET_ID)).andReturn(null);

		// request authorization for IdTag
		expect(authManager.authorize(TEST_ID_TAG)).andReturn(true);

		// get meter reading
		final GeneralNodeACEnergyDatum datum = new GeneralNodeACEnergyDatum();
		datum.setWattHourReading(111L);
		expect(meterDataSource.readCurrentDatum()).andReturn(datum);

		// start transaction
		Capture<StartTransactionRequest> startTransactionReqCapture = new Capture<StartTransactionRequest>();
		final StartTransactionResponse startTransactionResp = new StartTransactionResponse();
		startTransactionResp.setIdTagInfo(new IdTagInfo());
		startTransactionResp.getIdTagInfo().setStatus(AuthorizationStatus.ACCEPTED);
		startTransactionResp.setTransactionId(123);
		expect(
				client.startTransaction(capture(startTransactionReqCapture),
						eq(TEST_CHARGE_BOX_IDENTITY))).andReturn(startTransactionResp);

		// store the session
		Capture<ChargeSession> sessionCapture = new Capture<ChargeSession>();
		expect(chargeSessionDao.storeChargeSession(capture(sessionCapture))).andReturn(TEST_SESSION_ID);

		replayAll();

		String sessionId = manager.initiateChargeSession(TEST_ID_TAG, TEST_SOCKET_ID, null);
		Assert.assertEquals("Session ID", TEST_SESSION_ID, sessionId);

		// verify StartTransactionRequest
		StartTransactionRequest req = startTransactionReqCapture.getValue();
		Assert.assertNotNull("Request", req);
		Assert.assertEquals("StartTransactionRequest connectorId", TEST_CONNECTOR_ID.intValue(),
				req.getConnectorId());
		Assert.assertEquals("StartTransactionRequest idTag", TEST_ID_TAG, req.getIdTag());
		Assert.assertEquals("StartTransactionRequest meterStart", datum.getWattHourReading().intValue(),
				req.getMeterStart());
		Assert.assertNull("StartTransactionRequest reservationId", req.getReservationId());
		Assert.assertNotNull("StartTransactionRequest timestamp", req.getTimestamp());

		// verify ChargeSession entity
		ChargeSession session = sessionCapture.getValue();
		Assert.assertNotNull("ChargeSession", session);
		Assert.assertNotNull("ChargeSession created", session.getCreated());
		Assert.assertNull("ChargeSession ended", session.getEnded());
		Assert.assertNull("ChargeSession expiryDate", session.getExpiryDate());
		Assert.assertEquals("ChargeSession idTag", TEST_ID_TAG, session.getIdTag());
		Assert.assertNull("ChargeSession parentIdTag", session.getParentIdTag());
		Assert.assertEquals("ChargeSession socketId", TEST_SOCKET_ID, session.getSocketId());
		Assert.assertEquals("ChargeSession status", AuthorizationStatus.ACCEPTED, session.getStatus());
		Assert.assertNotNull("ChargeSession transactionId", session.getTransactionId());
		Assert.assertEquals("ChargeSession transactionId", startTransactionResp.getTransactionId(),
				session.getTransactionId().intValue());
	}
}

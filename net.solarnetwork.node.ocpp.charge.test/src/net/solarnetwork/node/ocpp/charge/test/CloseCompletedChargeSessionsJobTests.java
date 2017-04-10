/* ==================================================================
 * CloseCompletedChargeSessionsJobTests.java - 24/03/2017 11:09:33 AM
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

package net.solarnetwork.node.ocpp.charge.test;

import static org.easymock.EasyMock.expect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobExecutionException;
import net.solarnetwork.node.ocpp.ChargeSession;
import net.solarnetwork.node.ocpp.ChargeSessionManager;
import net.solarnetwork.node.ocpp.ChargeSessionMeterReading;
import net.solarnetwork.node.ocpp.charge.CloseCompletedChargeSessionsJob;
import net.solarnetwork.node.test.AbstractNodeTest;
import ocpp.v15.cs.AuthorizationStatus;
import ocpp.v15.cs.Measurand;

/**
 * Unit tests for the {@link CloseCompletedChargeSessionsJob} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CloseCompletedChargeSessionsJobTests extends AbstractNodeTest {

	private static final String TEST_SOCKET_ID = "/socket/test";
	private static final List<String> TEST_SOCKET_IDS = Arrays.asList(TEST_SOCKET_ID);
	private static final String TEST_ID_TAG = "test.tag";
	private static final Integer TEST_TRANSACTION_ID = 22;
	private static final String TEST_SESSION_ID = UUID.randomUUID().toString();

	private CloseCompletedChargeSessionsJob job;

	private ChargeSessionManager manager;

	@Before
	public void setup() {
		job = new CloseCompletedChargeSessionsJob();
		job.setThrowExceptions(true);
		manager = EasyMock.createMock(ChargeSessionManager.class);
		job.setService(manager);
	}

	@After
	public void finish() {
		EasyMock.verify(manager);
	}

	private void replayAll() {
		EasyMock.replay(manager);
	}

	@Test
	public void activeSessionNone() throws JobExecutionException {
		expect(manager.availableSocketIds()).andReturn(TEST_SOCKET_IDS);
		expect(manager.activeChargeSession(TEST_SOCKET_ID)).andReturn(null);
		replayAll();
		job.execute(null);
	}

	private ChargeSession createChargeSession(Date date) {
		ChargeSession session = new ChargeSession();
		session.setCreated(date);
		session.setIdTag(TEST_ID_TAG);
		session.setSessionId(TEST_SESSION_ID);
		session.setSocketId(TEST_SOCKET_ID);
		session.setStatus(AuthorizationStatus.ACCEPTED);
		session.setTransactionId(TEST_TRANSACTION_ID);
		return session;
	}

	@Test
	public void activeSessionNoReadingsNotStale() throws JobExecutionException {
		ChargeSession session = createChargeSession(new Date(System.currentTimeMillis() - 60000L));
		List<ChargeSessionMeterReading> readings = new ArrayList<ChargeSessionMeterReading>();
		expect(manager.availableSocketIds()).andReturn(TEST_SOCKET_IDS);
		expect(manager.activeChargeSession(TEST_SOCKET_ID)).andReturn(session);
		expect(manager.meterReadingsForChargeSession(TEST_SESSION_ID)).andReturn(readings);
		replayAll();
		job.execute(null);
	}

	@Test
	public void activeSessionNoReadingsStale() throws JobExecutionException {
		ChargeSession session = createChargeSession(
				new Date(System.currentTimeMillis() - (30 * 60 * 1000L)));
		List<ChargeSessionMeterReading> readings = new ArrayList<ChargeSessionMeterReading>();
		expect(manager.availableSocketIds()).andReturn(TEST_SOCKET_IDS);
		expect(manager.activeChargeSession(TEST_SOCKET_ID)).andReturn(session);
		expect(manager.meterReadingsForChargeSession(TEST_SESSION_ID)).andReturn(readings);
		manager.completeChargeSession(TEST_ID_TAG, TEST_SESSION_ID);
		replayAll();
		job.execute(null);
	}

	private void addReadings(List<ChargeSessionMeterReading> readings, Date date, Long wH,
			Double watts) {
		ChargeSessionMeterReading reading = new ChargeSessionMeterReading();
		reading.setMeasurand(Measurand.ENERGY_ACTIVE_IMPORT_REGISTER);
		reading.setValue(wH.toString());
		reading.setTs(date);
		readings.add(reading);

		reading = new ChargeSessionMeterReading();
		reading.setMeasurand(Measurand.POWER_ACTIVE_IMPORT);
		reading.setValue(watts.toString());
		reading.setTs(date);
		readings.add(reading);
	}

	@Test
	public void activeSessionTooFewReadings() throws JobExecutionException {
		ChargeSession session = createChargeSession(new Date(System.currentTimeMillis() - 60000L));
		List<ChargeSessionMeterReading> readings = new ArrayList<ChargeSessionMeterReading>();
		addReadings(readings, new Date(), 0L, 5.0);
		expect(manager.availableSocketIds()).andReturn(TEST_SOCKET_IDS);
		expect(manager.activeChargeSession(TEST_SOCKET_ID)).andReturn(session);
		expect(manager.meterReadingsForChargeSession(TEST_SESSION_ID)).andReturn(readings);
		replayAll();
		job.execute(null);
	}

	@Test
	public void activeSessionReadingAboveMaxEnergy() throws JobExecutionException {
		final long now = System.currentTimeMillis();
		ChargeSession session = createChargeSession(new Date(now - (30 * 60 * 1000L)));
		List<ChargeSessionMeterReading> readings = new ArrayList<ChargeSessionMeterReading>();
		long t = now - (10 * 60 * 1000L);
		long wh = 0;
		for ( int i = 0; i < 10; i++, t += 60000L, wh += 100 ) {
			addReadings(readings, new Date(t), wh, 500.0);
		}
		expect(manager.availableSocketIds()).andReturn(TEST_SOCKET_IDS);
		expect(manager.activeChargeSession(TEST_SOCKET_ID)).andReturn(session);
		expect(manager.meterReadingsForChargeSession(TEST_SESSION_ID)).andReturn(readings);
		replayAll();
		job.execute(null);
	}

	@Test
	public void activeSessionReadingBelowMaxEnergy() throws JobExecutionException {
		final long now = System.currentTimeMillis();
		ChargeSession session = createChargeSession(new Date(now - (30 * 60 * 1000L)));
		List<ChargeSessionMeterReading> readings = new ArrayList<ChargeSessionMeterReading>();
		long t = now - (10 * 60 * 1000L);
		long wh = 0;
		for ( int i = 0; i < 10; i++, t += 60000L, wh += (i % 2 == 0 ? 0 : 1) ) {
			addReadings(readings, new Date(t), wh, 5.0);
		}
		expect(manager.availableSocketIds()).andReturn(TEST_SOCKET_IDS);
		expect(manager.activeChargeSession(TEST_SOCKET_ID)).andReturn(session);
		expect(manager.meterReadingsForChargeSession(TEST_SESSION_ID)).andReturn(readings);
		manager.completeChargeSession(TEST_ID_TAG, TEST_SESSION_ID);
		replayAll();
		job.execute(null);
	}

}

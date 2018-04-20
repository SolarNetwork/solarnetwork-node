/* ==================================================================
 * JdbcChargeSessionDaoTests.java - 8/06/2015 10:46:45 am
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

package net.solarnetwork.node.ocpp.dao.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.ocpp.ChargeSession;
import net.solarnetwork.node.ocpp.ChargeSessionMeterReading;
import net.solarnetwork.node.ocpp.dao.JdbcChargeSessionDao;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import ocpp.v15.cs.AuthorizationStatus;
import ocpp.v15.cs.Measurand;
import ocpp.v15.cs.MeterValue.Value;
import ocpp.v15.cs.ReadingContext;
import ocpp.v15.cs.UnitOfMeasure;

/**
 * Test cases for the {@link JdbcChargeSessionDao} class.
 * 
 * @author matt
 * @version 1.1
 */
public class JdbcChargeSessionDaoTests extends AbstractNodeTransactionalTest {

	private static final String TEST_ID_TAG = "test.tag";
	private static final String TEST_SOCKET_ID = "test.socket";
	private static final int TEST_TRANSACTION_ID = 789;

	@Resource(name = "dataSource")
	private DataSource dataSource;

	private JdbcChargeSessionDao dao;
	private ChargeSession lastSession;

	@Before
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		dao = new JdbcChargeSessionDao();
		dao.setDataSource(dataSource);
		dao.init();
	}

	@Test
	public void insert() {
		ChargeSession session = new ChargeSession();
		session.setCreated(new Date());
		session.setIdTag(TEST_ID_TAG);
		session.setSocketId(TEST_SOCKET_ID);
		dao.storeChargeSession(session);
		Assert.assertNotNull("Session ID created", session.getSessionId());
		lastSession = session;
	}

	@Test
	public void getByPK() {
		insert();
		ChargeSession session = dao.getChargeSession(lastSession.getSessionId());
		Assert.assertNotNull("ChargeSession inserted", session);
		Assert.assertEquals("Created", lastSession.getCreated(), session.getCreated());
		Assert.assertEquals("IdTag", lastSession.getIdTag(), session.getIdTag());
		Assert.assertEquals("SocketID", lastSession.getSocketId(), session.getSocketId());
		Assert.assertNull("No expires", session.getExpiryDate());
		Assert.assertNull("No status", session.getStatus());
	}

	@Test
	public void update() {
		insert();
		ChargeSession session = dao.getChargeSession(lastSession.getSessionId());
		session.setStatus(AuthorizationStatus.ACCEPTED);
		session.setTransactionId(TEST_TRANSACTION_ID);
		session.setEnded(new Date());
		dao.storeChargeSession(session);
		ChargeSession updated = dao.getChargeSession(lastSession.getSessionId());
		Assert.assertEquals("Updated status", session.getStatus(), updated.getStatus());
		Assert.assertEquals("Updated xid", session.getTransactionId(), updated.getTransactionId());
		Assert.assertEquals("Updated ended", session.getEnded(), updated.getEnded());
	}

	@Test
	public void deleteCompletedNoneComplete() {
		insert();
		int result = dao.deleteCompletedChargeSessions(null);
		Assert.assertEquals("Deleted count", 0, result);
	}

	@Test
	public void deleteCompleted() {
		insert();
		ChargeSession session = dao.getChargeSession(lastSession.getSessionId());
		session.setEnded(new Date());
		dao.storeChargeSession(session);
		int result = dao.deleteCompletedChargeSessions(null);
		Assert.assertEquals("Deleted count", 1, result);
		ChargeSession notThere = dao.getChargeSession(session.getSessionId());
		Assert.assertNull("Session deleted", notThere);
	}

	@Test
	public void deleteCompletedOlder() {
		insert();
		ChargeSession session = dao.getChargeSession(lastSession.getSessionId());
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		session.setEnded(cal.getTime());
		dao.storeChargeSession(session);

		// insert a 2nd, with ended date now
		insert();
		ChargeSession session2 = dao.getChargeSession(lastSession.getSessionId());
		session2.setEnded(new Date());
		dao.storeChargeSession(session2);

		cal = Calendar.getInstance();
		cal.add(Calendar.HOUR, -1);
		int result = dao.deleteCompletedChargeSessions(cal.getTime());
		Assert.assertEquals("Deleted count", 1, result);

		ChargeSession notThere = dao.getChargeSession(session.getSessionId());
		Assert.assertNull("Session deleted", notThere);

		ChargeSession stillThere = dao.getChargeSession(session2.getSessionId());
		Assert.assertNotNull("Session not deleted", stillThere);
	}

	@Test
	public void deletePostedOlder() {
		insert();
		ChargeSession session = dao.getChargeSession(lastSession.getSessionId());
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		session.setPosted(cal.getTime());
		dao.storeChargeSession(session);

		// insert a 2nd, with ended date now
		insert();
		ChargeSession session2 = dao.getChargeSession(lastSession.getSessionId());
		session2.setPosted(new Date());
		dao.storeChargeSession(session2);

		cal = Calendar.getInstance();
		cal.add(Calendar.HOUR, -1);
		int result = dao.deletePostedChargeSessions(cal.getTime());
		Assert.assertEquals("Deleted count", 1, result);

		ChargeSession notThere = dao.getChargeSession(session.getSessionId());
		Assert.assertNull("Session deleted", notThere);

		ChargeSession stillThere = dao.getChargeSession(session2.getSessionId());
		Assert.assertNotNull("Session not deleted", stillThere);
	}

	@Test
	public void insertReadingsNone() {
		final String sessionId = UUID.randomUUID().toString();
		dao.addMeterReadings(sessionId, new Date(), Collections.<Value> emptyList());
	}

	@Test
	public void insertReadings() {
		insert();
		List<Value> readings = new ArrayList<Value>();
		for ( int i = 0; i < Measurand.values().length; i++ ) {
			Value v = new Value();
			v.setMeasurand(Measurand.values()[i]);
			v.setValue("Value " + i);
			v.setUnit(UnitOfMeasure.W);
			if ( i == 0 ) {
				v.setContext(ReadingContext.TRANSACTION_BEGIN);
			} else if ( i + 1 == Measurand.values().length ) {
				v.setContext(ReadingContext.TRANSACTION_END);
			} else {
				v.setContext(ReadingContext.SAMPLE_PERIODIC);
			}
			readings.add(v);
		}
		dao.addMeterReadings(lastSession.getSessionId(), null, readings);
	}

	@Test
	public void listReadings() {
		insertReadings();
		List<ChargeSessionMeterReading> results = dao
				.findMeterReadingsForSession(lastSession.getSessionId());
		Assert.assertNotNull("Readings", results);
		Assert.assertEquals("Readings count", Measurand.values().length, results.size());
		Set<Measurand> mSet = EnumSet.allOf(Measurand.class);
		for ( ChargeSessionMeterReading r : results ) {
			Assert.assertTrue("Non duplicate Measurand", mSet.remove(r.getMeasurand()));
			Assert.assertEquals("Reading value", "Value " + r.getMeasurand().ordinal(), r.getValue());
			Assert.assertNull("Reading format not stored", r.getFormat());
			Assert.assertEquals("Reading unit", UnitOfMeasure.W, r.getUnit());
			if ( r.getMeasurand().ordinal() == 0 ) {
				Assert.assertEquals("Reading context", ReadingContext.TRANSACTION_BEGIN, r.getContext());
			} else if ( r.getMeasurand().ordinal() + 1 == Measurand.values().length ) {
				Assert.assertEquals("Reading context", ReadingContext.TRANSACTION_END, r.getContext());
			} else {
				Assert.assertEquals("Reading context", ReadingContext.SAMPLE_PERIODIC, r.getContext());
			}
		}
	}

	@Test
	public void findIncompleteForSocketNone() {
		ChargeSession result = dao.getIncompleteChargeSessionForSocket(TEST_SOCKET_ID);
		Assert.assertNull("Not found", result);
	}

	@Test
	public void findIncompleteForSocket() {
		insert();
		ChargeSession result = dao.getIncompleteChargeSessionForSocket(TEST_SOCKET_ID);
		Assert.assertNotNull("Found", result);
		Assert.assertEquals("Socket ID", TEST_SOCKET_ID, result.getSocketId());
		Assert.assertEquals("Session ID", lastSession.getSessionId(), result.getSessionId());
	}

	@Test
	public void findIncompleteForTransactionNone() {
		insert();
		ChargeSession result = dao.getIncompleteChargeSessionForTransaction(TEST_TRANSACTION_ID);
		Assert.assertNull("Not found", result);
	}

	@Test
	public void findIncompleteForTransaction() {
		insert();
		ChargeSession session = dao.getChargeSession(lastSession.getSessionId());
		session.setTransactionId(TEST_TRANSACTION_ID);
		dao.storeChargeSession(session);
		ChargeSession result = dao.getIncompleteChargeSessionForTransaction(TEST_TRANSACTION_ID);
		Assert.assertNotNull("Found", result);
		Assert.assertEquals("Socket ID", TEST_SOCKET_ID, result.getSocketId());
		Assert.assertEquals("Session ID", lastSession.getSessionId(), result.getSessionId());
		Assert.assertEquals("Transaction ID", session.getTransactionId(), result.getTransactionId());
	}

	@Test
	public void findIncompleteNone() {
		List<ChargeSession> results = dao.getIncompleteChargeSessions();
		Assert.assertNotNull("Results list", results);
		Assert.assertEquals("Results count", 0, results.size());
	}

	@Test
	public void findIncomplete() {
		ChargeSession session = new ChargeSession();
		session.setCreated(new Date(System.currentTimeMillis() - 1000));
		session.setIdTag(TEST_ID_TAG);
		session.setSocketId("test.socket.2");
		dao.storeChargeSession(session);

		insert();

		List<ChargeSession> results = dao.getIncompleteChargeSessions();
		Assert.assertNotNull("Results list", results);
		Assert.assertEquals("Results count", 2, results.size());
		Assert.assertEquals("Results ordered by date", TEST_SOCKET_ID, results.get(0).getSocketId());
		Assert.assertEquals("Results ordered by date", session.getSocketId(),
				results.get(1).getSocketId());
	}

	@Test
	public void findNeedsPostingNone() {
		List<ChargeSession> results = dao.getChargeSessionsNeedingPosting(Integer.MAX_VALUE);
		Assert.assertNotNull("Results list", results);
		Assert.assertEquals("Results count", 0, results.size());
	}

	@Test
	public void findNeedsPostingAllPosted() {
		ChargeSession session = new ChargeSession();
		session.setCreated(new Date());
		session.setIdTag(TEST_ID_TAG);
		session.setSocketId("test.socket.2");
		session.setTransactionId(1);
		session.setEnded(new Date());
		session.setPosted(new Date());
		dao.storeChargeSession(session);

		List<ChargeSession> results = dao.getChargeSessionsNeedingPosting(Integer.MAX_VALUE);
		Assert.assertNotNull("Results list", results);
		Assert.assertEquals("Results count", 0, results.size());
	}

	@Test
	public void findNeedsPostingNoTransactionId() {
		insert();

		List<ChargeSession> results = dao.getChargeSessionsNeedingPosting(Integer.MAX_VALUE);
		Assert.assertNotNull("Results list", results);
		Assert.assertEquals("Results count", 1, results.size());
		Assert.assertEquals("Results ordered by date", lastSession.getSessionId(),
				results.get(0).getSessionId());
	}

	@Test
	public void findNeedsPostingNotPostedNotEnded() {
		ChargeSession session = new ChargeSession();
		session.setCreated(new Date());
		session.setIdTag(TEST_ID_TAG);
		session.setSocketId("test.socket.2");
		session.setTransactionId(1);
		dao.storeChargeSession(session);

		List<ChargeSession> results = dao.getChargeSessionsNeedingPosting(Integer.MAX_VALUE);
		Assert.assertNotNull("Results list", results);
		Assert.assertEquals("Results count", 0, results.size());
	}

	@Test
	public void findNeedsPostingNotPosted() {
		ChargeSession session = new ChargeSession();
		session.setCreated(new Date());
		session.setIdTag(TEST_ID_TAG);
		session.setSocketId("test.socket.2");
		session.setTransactionId(1);
		session.setEnded(new Date());
		dao.storeChargeSession(session);

		List<ChargeSession> results = dao.getChargeSessionsNeedingPosting(Integer.MAX_VALUE);
		Assert.assertNotNull("Results list", results);
		Assert.assertEquals("Results count", 1, results.size());
		Assert.assertEquals("Results ordered by date", session.getSessionId(),
				results.get(0).getSessionId());
	}
}

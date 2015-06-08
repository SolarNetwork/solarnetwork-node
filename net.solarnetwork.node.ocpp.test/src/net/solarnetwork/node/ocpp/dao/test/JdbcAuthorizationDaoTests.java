/* ==================================================================
 * JdbcAuthorizationDaoTests.java - 8/06/2015 10:46:45 am
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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import javax.annotation.Resource;
import javax.sql.DataSource;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.ocpp.Authorization;
import net.solarnetwork.node.ocpp.dao.JdbcAuthorizationDao;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import ocpp.v15.AuthorizationStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link JdbcAuthorizationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcAuthorizationDaoTests extends AbstractNodeTransactionalTest {

	private static final String TEST_ID_TAG = "test.tag";
	private static final String TEST_PARENT_ID_TAG = "parent.tag";

	@Resource(name = "dataSource")
	private DataSource dataSource;

	private JdbcAuthorizationDao dao;
	private Authorization lastAuth;

	@Before
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		dao = new JdbcAuthorizationDao();
		dao.setDataSource(dataSource);
		dao.init();
	}

	@Test
	public void insert() {
		Authorization auth = new Authorization();
		auth.setCreated(new Date());
		auth.setIdTag(TEST_ID_TAG);
		auth.setParentIdTag(TEST_PARENT_ID_TAG);
		auth.setStatus(AuthorizationStatus.ACCEPTED);
		dao.storeAuthorization(auth);
		lastAuth = auth;
	}

	@Test
	public void insertWithExpires() throws Exception {
		Authorization auth = new Authorization();
		auth.setCreated(new Date());
		auth.setIdTag(TEST_ID_TAG);
		auth.setParentIdTag(TEST_PARENT_ID_TAG);
		auth.setStatus(AuthorizationStatus.ACCEPTED);

		DatatypeFactory dtFactory = DatatypeFactory.newInstance();
		XMLGregorianCalendar expires = dtFactory.newXMLGregorianCalendar(new GregorianCalendar());
		auth.setExpiryDate(expires);

		dao.storeAuthorization(auth);
		lastAuth = auth;
	}

	@Test
	public void getByPK() {
		insert();
		Authorization auth = dao.getAuthorization(TEST_ID_TAG);
		Assert.assertNotNull("Authorization inserted", auth);
		Assert.assertEquals("Created", lastAuth.getCreated(), auth.getCreated());
		Assert.assertEquals("IdTag", lastAuth.getIdTag(), auth.getIdTag());
		Assert.assertEquals("ParentIdTag", lastAuth.getParentIdTag(), auth.getParentIdTag());
		Assert.assertEquals("Status", lastAuth.getStatus(), auth.getStatus());
		Assert.assertNull("No expires", auth.getExpiryDate());
	}

	@Test
	public void getByPKWithExpires() throws Exception {
		insertWithExpires();
		Authorization auth = dao.getAuthorization(TEST_ID_TAG);
		Assert.assertNotNull("Authorization inserted", auth);
		Assert.assertEquals("Created", lastAuth.getCreated(), auth.getCreated());
		Assert.assertEquals("IdTag", lastAuth.getIdTag(), auth.getIdTag());
		Assert.assertEquals("ParentIdTag", lastAuth.getParentIdTag(), auth.getParentIdTag());
		Assert.assertEquals("Status", lastAuth.getStatus(), auth.getStatus());
		Assert.assertEquals("ExpiryDate", lastAuth.getExpiryDate(), auth.getExpiryDate());
	}

	@Test
	public void update() {
		insert();
		Authorization auth = dao.getAuthorization(TEST_ID_TAG);
		auth.setStatus(AuthorizationStatus.INVALID);
		dao.storeAuthorization(auth);
		Authorization updated = dao.getAuthorization(TEST_ID_TAG);
		Assert.assertEquals("Updated status", auth.getStatus(), updated.getStatus());
	}

	@Test
	public void updateAddExpires() throws Exception {
		insert();
		Authorization auth = dao.getAuthorization(TEST_ID_TAG);

		DatatypeFactory dtFactory = DatatypeFactory.newInstance();
		XMLGregorianCalendar expires = dtFactory.newXMLGregorianCalendar(new GregorianCalendar());
		auth.setExpiryDate(expires);

		dao.storeAuthorization(auth);
		Authorization updated = dao.getAuthorization(TEST_ID_TAG);
		Assert.assertEquals("Updated expires", auth.getExpiryDate(), updated.getExpiryDate());
	}

	@Test
	public void deleteExpired() throws Exception {
		insertWithExpires();
		int result = dao.deleteExpiredAuthorizations(null);
		Assert.assertEquals("Deleted count", 1, result);
		Assert.assertNull("No longer exists", dao.getAuthorization(lastAuth.getIdTag()));
	}

	@Test
	public void deleteExpiredSpecificDate() throws Exception {
		insertWithExpires();
		int result = dao
				.deleteExpiredAuthorizations(new Date(System.currentTimeMillis() - 10000000000L));
		Assert.assertEquals("Deleted count", 0, result);
		Assert.assertNotNull("Still exists", dao.getAuthorization(lastAuth.getIdTag()));

		result = dao.deleteExpiredAuthorizations(new Date(System.currentTimeMillis() + 1000L));
		Assert.assertEquals("Deleted count", 1, result);
		Assert.assertNull("Still exists", dao.getAuthorization(lastAuth.getIdTag()));
	}

	@Test
	public void statusCountsEmpty() {
		Map<AuthorizationStatus, Integer> counts = dao.statusCounts();
		Assert.assertNotNull("Counts should not be null", counts);
		Assert.assertEquals("Counts count", 0, counts.size());
	}

	@Test
	public void statusCountsSingle() {
		insert();
		Map<AuthorizationStatus, Integer> counts = dao.statusCounts();
		Assert.assertNotNull("Counts should not be null", counts);
		Assert.assertEquals("Counts count", 1, counts.size());
		Assert.assertEquals("Count", Integer.valueOf(1), counts.get(AuthorizationStatus.ACCEPTED));
	}

	@Test
	public void statusCountsMulti() throws Exception {
		insert();

		lastAuth.setIdTag("accepted.2");
		dao.storeAuthorization(lastAuth);

		lastAuth.setIdTag("blocked");
		lastAuth.setStatus(AuthorizationStatus.BLOCKED);
		dao.storeAuthorization(lastAuth);

		lastAuth.setIdTag("invalid");
		lastAuth.setStatus(AuthorizationStatus.INVALID);
		dao.storeAuthorization(lastAuth);

		lastAuth.setIdTag("invalid.2");
		dao.storeAuthorization(lastAuth);

		Map<AuthorizationStatus, Integer> counts = dao.statusCounts();
		Assert.assertNotNull("Counts should not be null", counts);
		Assert.assertEquals("Counts count", 3, counts.size());
		Assert.assertEquals("Count", Integer.valueOf(2), counts.get(AuthorizationStatus.ACCEPTED));
		Assert.assertEquals("Count", Integer.valueOf(1), counts.get(AuthorizationStatus.BLOCKED));
		Assert.assertEquals("Count", Integer.valueOf(2), counts.get(AuthorizationStatus.INVALID));
	}

	@Test
	public void statusCountsExpired() throws Exception {
		insert();

		// add a 2nd, expired auth
		DatatypeFactory dtFactory = DatatypeFactory.newInstance();
		GregorianCalendar cal = new GregorianCalendar();
		cal.add(Calendar.YEAR, -1);
		XMLGregorianCalendar expires = dtFactory.newXMLGregorianCalendar(cal);
		lastAuth.setIdTag("expired");
		lastAuth.setExpiryDate(expires);
		lastAuth.setStatus(AuthorizationStatus.BLOCKED);
		dao.storeAuthorization(lastAuth);

		Map<AuthorizationStatus, Integer> counts = dao.statusCounts();
		Assert.assertNotNull("Counts should not be null", counts);
		Assert.assertEquals("Counts count", 1, counts.size());
		Assert.assertEquals("Count", Integer.valueOf(1), counts.get(AuthorizationStatus.ACCEPTED));
	}
}

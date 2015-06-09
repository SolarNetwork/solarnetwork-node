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

import java.util.Date;
import javax.annotation.Resource;
import javax.sql.DataSource;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.ocpp.ChargeSession;
import net.solarnetwork.node.ocpp.dao.JdbcChargeSessionDao;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link JdbcChargeSessionDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargeSessionDaoTests extends AbstractNodeTransactionalTest {

	private static final String TEST_ID_TAG = "test.tag";

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
		Assert.assertNull("No expires", session.getExpiryDate());
	}

	@Test
	public void update() {
		insert();
		ChargeSession session = dao.getChargeSession(lastSession.getSessionId());
		session.setTransactionId(-1);
		session.setEnded(new Date());
		dao.storeChargeSession(session);
		ChargeSession updated = dao.getChargeSession(lastSession.getSessionId());
		Assert.assertEquals("Updated xid", session.getTransactionId(), updated.getTransactionId());
		Assert.assertEquals("Updated ended", session.getEnded(), updated.getEnded());
	}

}

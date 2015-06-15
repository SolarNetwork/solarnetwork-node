/* ==================================================================
 * JdbcSocketDaoTests.java - 15/06/2015 3:33:33 pm
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
import net.solarnetwork.node.ocpp.Socket;
import net.solarnetwork.node.ocpp.dao.JdbcSocketDao;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * test cases for the {@link JdbcSocketDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcSocketDaoTests extends AbstractNodeTransactionalTest {

	private static final String TEST_SOCKET_ID = "test.socket";

	@Resource(name = "dataSource")
	private DataSource dataSource;

	private JdbcSocketDao dao;
	private Socket lastSocket;

	@Before
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		dao = new JdbcSocketDao();
		dao.setDataSource(dataSource);
		dao.init();
	}

	@Test
	public void insert() {
		Socket socket = new Socket();
		socket.setCreated(new Date());
		socket.setSocketId(TEST_SOCKET_ID);
		socket.setEnabled(true);
		dao.storeSocket(socket);
		lastSocket = socket;
	}

	@Test
	public void getByPK() {
		insert();
		Socket socket = dao.getSocket(lastSocket.getSocketId());
		Assert.assertNotNull("Socket inserted", socket);
		Assert.assertEquals("Created", lastSocket.getCreated(), socket.getCreated());
		Assert.assertEquals("SocketID", lastSocket.getSocketId(), socket.getSocketId());
		Assert.assertEquals("Enabled", lastSocket.isEnabled(), socket.isEnabled());
	}

	@Test
	public void update() {
		insert();
		Socket socket = dao.getSocket(TEST_SOCKET_ID);
		socket.setEnabled(false);
		dao.storeSocket(socket);
		Socket updated = dao.getSocket(lastSocket.getSocketId());
		Assert.assertEquals("Updated enabled", socket.isEnabled(), updated.isEnabled());
	}

	@Test
	public void isEnabledNoEntity() {
		boolean enabled = dao.isEnabled(TEST_SOCKET_ID);
		Assert.assertEquals("Enabled by default", true, enabled);
	}

	@Test
	public void isEnabledTrue() {
		insert();
		boolean enabled = dao.isEnabled(TEST_SOCKET_ID);
		Assert.assertEquals("Enabled by default", true, enabled);
	}

	@Test
	public void isEnabledFalse() {
		update();
		boolean enabled = dao.isEnabled(TEST_SOCKET_ID);
		Assert.assertEquals("Disabled", false, enabled);
	}
}

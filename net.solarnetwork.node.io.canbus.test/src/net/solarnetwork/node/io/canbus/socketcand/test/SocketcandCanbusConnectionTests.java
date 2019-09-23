/* ==================================================================
 * SocketcandCanbusConnectionTests.java - 23/09/2019 5:21:56 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.canbus.socketcand.test;

import static java.util.Collections.singletonList;
import static net.solarnetwork.node.io.canbus.CanbusConnection.DATA_FILTER_NONE;
import static org.easymock.EasyMock.expect;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.io.canbus.CanbusFrameListener;
import net.solarnetwork.node.io.canbus.socketcand.CanbusSocket;
import net.solarnetwork.node.io.canbus.socketcand.CanbusSocketProvider;
import net.solarnetwork.node.io.canbus.socketcand.MessageType;
import net.solarnetwork.node.io.canbus.socketcand.SocketcandCanbusConnection;
import net.solarnetwork.node.io.canbus.socketcand.msg.BasicMessage;
import net.solarnetwork.node.io.canbus.socketcand.msg.SubscribeMessageImpl;

/**
 * Test cases for the {@link SocketcandCanbusConnection} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SocketcandCanbusConnectionTests {

	private static final String TEST_HOST = "localhost";
	private static final int TEST_PORT = 4321;
	private static final String TEST_BUS_NAME = "Test_Bus";

	private CanbusSocketProvider socketProvider;
	private CanbusSocket socket;

	private List<Object> mocks;

	@Before
	public void setup() {
		socketProvider = EasyMock.createMock(CanbusSocketProvider.class);
		socket = EasyMock.createMock(CanbusSocket.class);

		mocks = new ArrayList<>(8);
		mocks.add(socketProvider);
		mocks.add(socket);
	}

	public void replayAll() {
		EasyMock.replay(mocks.toArray());
	}

	@After
	public void teardown() {
		EasyMock.verify(mocks.toArray());
	}

	@Test
	public void openAndClose() throws IOException {
		// GIVEN
		expect(socketProvider.createCanbusSocket()).andReturn(socket);
		socket.open(TEST_HOST, TEST_PORT);

		expect(socket.nextMessage()).andReturn(new BasicMessage(MessageType.Hi));
		socket.writeMessage(new BasicMessage(MessageType.Open, null, singletonList(TEST_BUS_NAME)));
		expect(socket.nextMessage()).andReturn(new BasicMessage(MessageType.Ok));
		socket.connectionConfirmed();

		socket.close();

		// WHEN
		replayAll();
		try (SocketcandCanbusConnection conn = new SocketcandCanbusConnection(socketProvider, TEST_HOST,
				TEST_PORT, TEST_BUS_NAME)) {
			conn.open();
		}

		// THEN
	}

	@Test
	public void subscribe_noFilter() throws IOException {
		// GIVEN
		expect(socketProvider.createCanbusSocket()).andReturn(socket);
		socket.open(TEST_HOST, TEST_PORT);

		expect(socket.nextMessage()).andReturn(new BasicMessage(MessageType.Hi));
		socket.writeMessage(new BasicMessage(MessageType.Open, null, singletonList(TEST_BUS_NAME)));
		expect(socket.nextMessage()).andReturn(new BasicMessage(MessageType.Ok));
		socket.connectionConfirmed();

		socket.writeMessage(new SubscribeMessageImpl(1, false, 0, 0));

		socket.close();

		CanbusFrameListener listener = EasyMock.createMock(CanbusFrameListener.class);
		mocks.add(listener);

		// WHEN
		replayAll();
		try (SocketcandCanbusConnection conn = new SocketcandCanbusConnection(socketProvider, TEST_HOST,
				TEST_PORT, TEST_BUS_NAME)) {
			conn.open();
			conn.subscribe(1, false, Duration.ZERO, DATA_FILTER_NONE, listener);
		}

		// THEN
	}

}

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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.node.io.canbus.CanbusConnection.DATA_FILTER_NONE;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.io.canbus.CanbusFrame;
import net.solarnetwork.node.io.canbus.CanbusFrameListener;
import net.solarnetwork.node.io.canbus.socketcand.CanbusSocketProvider;
import net.solarnetwork.node.io.canbus.socketcand.Message;
import net.solarnetwork.node.io.canbus.socketcand.MessageType;
import net.solarnetwork.node.io.canbus.socketcand.SocketcandCanbusConnection;
import net.solarnetwork.node.io.canbus.socketcand.msg.BasicMessage;
import net.solarnetwork.node.io.canbus.socketcand.msg.FrameMessageImpl;
import net.solarnetwork.node.io.canbus.socketcand.msg.SubscribeMessageImpl;
import net.solarnetwork.node.io.canbus.socketcand.msg.UnsubscribeMessageImpl;

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
	private Executor executor;
	private TestCanbusSocket socket;

	private List<Object> mocks;

	@Before
	public void setup() {
		socketProvider = EasyMock.createMock(CanbusSocketProvider.class);
		socket = new TestCanbusSocket();

		mocks = new ArrayList<>(8);
		mocks.add(socketProvider);

		executor = Executors.newCachedThreadPool();
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

		// WHEN
		replayAll();
		try (SocketcandCanbusConnection conn = new SocketcandCanbusConnection(socketProvider, executor,
				TEST_HOST, TEST_PORT, TEST_BUS_NAME)) {
			conn.open();
			assertThat("Socket established", conn.isEstablished(), equalTo(true));
		}

		// THEN
		assertThat("Socket closed", socket.isClosed(), equalTo(true));
		assertThat("Sent messages", socket.getWrittenMessages(),
				contains(new BasicMessage(MessageType.Open, null, singletonList(TEST_BUS_NAME))));
	}

	@Test
	public void subscribe_noFilter() throws IOException {
		// GIVEN
		expect(socketProvider.createCanbusSocket()).andReturn(socket);

		CanbusFrameListener listener = EasyMock.createMock(CanbusFrameListener.class);
		mocks.add(listener);

		// WHEN
		replayAll();
		try (SocketcandCanbusConnection conn = new SocketcandCanbusConnection(socketProvider, executor,
				TEST_HOST, TEST_PORT, TEST_BUS_NAME)) {
			conn.open();
			conn.subscribe(1, false, Duration.ZERO, DATA_FILTER_NONE, listener);
		}

		// THEN
		assertThat("Socket closed", socket.isClosed(), equalTo(true));
		assertThat("Sent messages", socket.getWrittenMessages(),
				contains(new BasicMessage(MessageType.Open, null, singletonList(TEST_BUS_NAME)),
						new SubscribeMessageImpl(1, false, 0, 0)));
	}

	@Test
	public void subscribe_receive() throws IOException {
		// GIVEN
		expect(socketProvider.createCanbusSocket()).andReturn(socket);

		CanbusFrameListener listener = EasyMock.createMock(CanbusFrameListener.class);
		mocks.add(listener);

		Capture<CanbusFrame> frameCaptor = Capture.newInstance(CaptureType.ALL);
		listener.canbusFrameReceived(capture(frameCaptor));

		// WHEN
		replayAll();
		try (SocketcandCanbusConnection conn = new SocketcandCanbusConnection(socketProvider, executor,
				TEST_HOST, TEST_PORT, TEST_BUS_NAME)) {
			conn.open();
			conn.subscribe(1, false, Duration.ZERO, DATA_FILTER_NONE, listener);

			// mock a frame message from the server
			socket.respondMessage(new FrameMessageImpl(asList("1 23.424242 11 22 33 44".split(" "))));
		}

		// THEN
		assertThat("Socket closed", socket.isClosed(), equalTo(true));
		assertThat("Sent messages", socket.getWrittenMessages(),
				contains(new BasicMessage(MessageType.Open, null, singletonList(TEST_BUS_NAME)),
						new SubscribeMessageImpl(1, false, 0, 0)));

		CanbusFrame f = frameCaptor.getValue();
		assertThat("Frame captured", f, notNullValue());
		assertThat("Frame address", f.getAddress(), equalTo(1));
		assertThat("Frame data",
				Arrays.equals(f.getData(),
						new byte[] { (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44 }),
				equalTo(true));
	}

	@Test
	public void subscribe_noReceiveOtherAddress() throws IOException {
		// GIVEN
		expect(socketProvider.createCanbusSocket()).andReturn(socket);

		CanbusFrameListener listener = EasyMock.createMock(CanbusFrameListener.class);
		mocks.add(listener);

		// WHEN
		replayAll();
		try (SocketcandCanbusConnection conn = new SocketcandCanbusConnection(socketProvider, executor,
				TEST_HOST, TEST_PORT, TEST_BUS_NAME)) {
			conn.open();
			conn.subscribe(1, false, Duration.ZERO, DATA_FILTER_NONE, listener);

			// mock a frame message from the server
			socket.respondMessage(new FrameMessageImpl(asList("2 23.424242 11 22 33 44".split(" "))));
		}

		// THEN
		assertThat("Socket closed", socket.isClosed(), equalTo(true));
		assertThat("Sent messages", socket.getWrittenMessages(),
				contains(new BasicMessage(MessageType.Open, null, singletonList(TEST_BUS_NAME)),
						new SubscribeMessageImpl(1, false, 0, 0)));
	}

	@Test
	public void subscribe_receive_unsubscribe_noReceive() throws Exception {
		// GIVEN
		expect(socketProvider.createCanbusSocket()).andReturn(socket);

		CanbusFrameListener listener = EasyMock.createMock(CanbusFrameListener.class);
		mocks.add(listener);

		Capture<CanbusFrame> frameCaptor = Capture.newInstance(CaptureType.ALL);
		listener.canbusFrameReceived(capture(frameCaptor));

		// WHEN
		replayAll();
		try (SocketcandCanbusConnection conn = new SocketcandCanbusConnection(socketProvider, executor,
				TEST_HOST, TEST_PORT, TEST_BUS_NAME)) {
			conn.open();
			conn.subscribe(1, false, Duration.ZERO, DATA_FILTER_NONE, listener);

			// mock a frame message from the server
			socket.respondMessage(new FrameMessageImpl(asList("1 23.424242 11 22 33 44".split(" "))));

			Thread.sleep(400L);

			conn.unsubscribe(1, false);

			// mock another frame message; should not get a callback on this one
			socket.respondMessage(new FrameMessageImpl(asList("1 32.0 22 33 44 55".split(" "))));
		}

		// THEN
		assertThat("Socket closed", socket.isClosed(), equalTo(true));
		assertThat("Sent messages", socket.getWrittenMessages(),
				contains(new BasicMessage(MessageType.Open, null, singletonList(TEST_BUS_NAME)),
						new SubscribeMessageImpl(1, false, 0, 0), new UnsubscribeMessageImpl(1, false)));

		assertThat("Only 1 frame captured", frameCaptor.getValues(), hasSize(1));
		CanbusFrame f = frameCaptor.getValue();
		assertThat("Frame captured", f, notNullValue());
		assertThat("Frame address", f.getAddress(), equalTo(1));
		assertThat("Frame data",
				Arrays.equals(f.getData(),
						new byte[] { (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44 }),
				equalTo(true));
	}

	@Test
	public void monitor_receive_unmonnitor_noReceive() throws IOException, InterruptedException {
		// GIVEN
		expect(socketProvider.createCanbusSocket()).andReturn(socket);

		CanbusFrameListener listener = EasyMock.createMock(CanbusFrameListener.class);
		mocks.add(listener);

		Capture<CanbusFrame> frameCaptor = Capture.newInstance(CaptureType.ALL);
		listener.canbusFrameReceived(capture(frameCaptor));

		// WHEN
		replayAll();
		try (SocketcandCanbusConnection conn = new SocketcandCanbusConnection(socketProvider, executor,
				TEST_HOST, TEST_PORT, TEST_BUS_NAME)) {
			conn.open();
			conn.monitor(listener);

			// mock a frame message from the server
			socket.respondMessage(new FrameMessageImpl(asList("1 23.424242 11 22 33 44".split(" "))));

			Thread.sleep(400);

			conn.unmonitor();

			// mock another frame message; should not get a callback on this one
			socket.respondMessage(new FrameMessageImpl(asList("1 32.0 22 33 44 55".split(" "))));
		}

		// THEN
		assertThat("Socket closed", socket.isClosed(), equalTo(true));
		assertThat("Sent messages", socket.getWrittenMessages(),
				contains(new BasicMessage(MessageType.Open, null, singletonList(TEST_BUS_NAME)),
						new BasicMessage(MessageType.Rawmode), new BasicMessage(MessageType.Bcmmode)));

		assertThat("Only 1 frame captured", frameCaptor.getValues(), hasSize(1));
		CanbusFrame f = frameCaptor.getValue();
		assertThat("Frame captured", f, notNullValue());
		assertThat("Frame address", f.getAddress(), equalTo(1));
		assertThat("Frame data",
				Arrays.equals(f.getData(),
						new byte[] { (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44 }),
				equalTo(true));
	}

	@Test
	public void verifyConnectivity_ok() throws Exception {
		// GIVEN
		expect(socketProvider.createCanbusSocket()).andReturn(socket);

		// WHEN
		replayAll();
		Boolean verified = false;
		try (SocketcandCanbusConnection conn = new SocketcandCanbusConnection(socketProvider, executor,
				TEST_HOST, TEST_PORT, TEST_BUS_NAME)) {
			conn.open();

			Future<Boolean> future = conn.verifyConnectivity();

			List<Future<Message>> msgFutures = stream(conn.messageFutures().spliterator(), false)
					.collect(toList());
			assertThat("Message future available", msgFutures, hasSize(1));

			// mock a frame message from the server
			socket.respondMessage(new BasicMessage(MessageType.Echo));

			Thread.sleep(200);

			verified = future.get(1, TimeUnit.SECONDS);

			msgFutures = stream(conn.messageFutures().spliterator(), false).collect(toList());
			assertThat("Message future removed", msgFutures, hasSize(0));
		}

		// THEN
		assertThat("Socket closed", socket.isClosed(), equalTo(true));
		assertThat("Sent messages", socket.getWrittenMessages(),
				contains(new BasicMessage(MessageType.Open, null, singletonList(TEST_BUS_NAME)),
						new BasicMessage(MessageType.Echo)));
		assertThat("Connectivity verified", verified, equalTo(true));
	}

	@Test
	public void verifyConnectivity_timeout() throws Exception {
		// GIVEN
		expect(socketProvider.createCanbusSocket()).andReturn(socket);

		// WHEN
		replayAll();
		try (SocketcandCanbusConnection conn = new SocketcandCanbusConnection(socketProvider, executor,
				TEST_HOST, TEST_PORT, TEST_BUS_NAME)) {
			conn.setVerifyConnectivityTimeout(100);

			conn.open();

			Future<Boolean> future = conn.verifyConnectivity();

			// no response message...

			try {
				future.get(200, TimeUnit.MILLISECONDS);
				fail("Expected a TimeoutException when connection not verified with Echo command.");
			} catch ( TimeoutException e ) {
				// perfect!
			}

			List<Future<Message>> msgFutures = stream(conn.messageFutures().spliterator(), false)
					.collect(toList());
			assertThat("Message future removed", msgFutures, hasSize(0));
		}

		// THEN
		assertThat("Socket closed", socket.isClosed(), equalTo(true));
		assertThat("Sent messages", socket.getWrittenMessages(),
				contains(new BasicMessage(MessageType.Open, null, singletonList(TEST_BUS_NAME)),
						new BasicMessage(MessageType.Echo)));
	}

}

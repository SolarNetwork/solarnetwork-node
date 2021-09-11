/* ==================================================================
 * RfidSocketReaderServiceTests.java - 29/07/2016 6:39:58 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.rfid.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.rfid.RfidSocketReaderService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Unit tests for the {@link RfidSocketReaderService} class.
 * 
 * @author matt
 * @version 2.0
 */
public class RfidSocketReaderServiceTests {

	private Logger log = LoggerFactory.getLogger(getClass());

	private ServerSocket server;
	private Thread serverThread;
	private EventAdmin eventAdmin;

	private RfidSocketReaderService service;

	@Before
	public void setup() throws Exception {
		server = new ServerSocket(0);
		server.setReuseAddress(true);
	}

	@After
	public void shutdown() throws Exception {
		server.close();
	}

	private void startServerThread(Runnable runnable) throws Exception {
		serverThread = new Thread(runnable);
		serverThread.setDaemon(true);
		serverThread.start();
	}

	private void startServerThread(final Collection<MockServerMessage> messages) throws Exception {
		startServerThread(new Runnable() {

			@Override
			public void run() {
				log.debug("Server thread starting for messages {}", messages);
				PrintWriter out = null;
				try {
					Socket socket = server.accept();
					out = new PrintWriter(socket.getOutputStream(), true);
					out.println("HELLO");
					log.debug("RFID server sent HELLO, processing messages {}", messages);
					for ( MockServerMessage msg : messages ) {
						Thread.sleep(msg.getPause());
						log.debug("Server thread sending message {}", msg.getMessage());
						out.println(msg.getMessage());
					}
				} catch ( IOException e ) {
					log.warn("RFID server IOException: {}", e.getMessage());
				} catch ( InterruptedException e ) {
					log.warn("RFID server InterruptedException: {}", e.getMessage());
				} finally {
					if ( out != null ) {
						out.close();
					}
				}
				log.debug("Server thread exiting for messages {}", messages);
			}
		});
	}

	private static final String TEST_UID = "test";
	private static final String TEST_GROUP_UID = "test group";

	@Test
	public void testConnectToServer() throws Exception {
		List<MockServerMessage> msgs = Arrays.asList(new MockServerMessage(10, "Message 1"));
		startServerThread(msgs);
		int port = server.getLocalPort();

		eventAdmin = EasyMock.createMock(EventAdmin.class);

		Capture<Event> eventCapt = new Capture<Event>();
		eventAdmin.postEvent(capture(eventCapt));
		replay(eventAdmin);

		service = new RfidSocketReaderService();
		service.setPort(port);
		service.setConnectRetryMinutes(0);
		service.setEventAdmin(new StaticOptionalService<EventAdmin>(eventAdmin));
		service.setUid(TEST_UID);
		service.setGroupUID(TEST_GROUP_UID);
		service.init();

		serverThread.join(2000);
		Thread.sleep(500); // give time for service to post event

		verify(eventAdmin);

		Event event = eventCapt.getValue();
		Assert.assertEquals(RfidSocketReaderService.TOPIC_RFID_MESSAGE_RECEIVED, event.getTopic());
		Assert.assertEquals(msgs.get(0).getMessage(),
				event.getProperty(RfidSocketReaderService.EVENT_PARAM_MESSAGE));
		Assert.assertEquals(TEST_UID, event.getProperty(RfidSocketReaderService.EVENT_PARAM_UID));
		Assert.assertEquals(TEST_GROUP_UID,
				event.getProperty(RfidSocketReaderService.EVENT_PARAM_GROUP_UID));
		Assert.assertEquals(Long.valueOf(1),
				event.getProperty(RfidSocketReaderService.EVENT_PARAM_COUNT));
		Assert.assertNotNull(event.getProperty(RfidSocketReaderService.EVENT_PARAM_DATE));
	}

	@Test
	public void testReconnectToServer() throws Exception {
		testConnectToServer();

		EasyMock.reset(eventAdmin);

		Capture<Event> eventCapt = new Capture<Event>();
		eventAdmin.postEvent(capture(eventCapt));
		replay(eventAdmin);

		List<MockServerMessage> msgs = Arrays.asList(new MockServerMessage(10, "Message 2"));
		startServerThread(msgs);
		serverThread.join(2000);
		Thread.sleep(500); // give time for service to post event

		verify(eventAdmin);

		Event event = eventCapt.getValue();
		Assert.assertEquals(RfidSocketReaderService.TOPIC_RFID_MESSAGE_RECEIVED, event.getTopic());
		Assert.assertEquals(msgs.get(0).getMessage(),
				event.getProperty(RfidSocketReaderService.EVENT_PARAM_MESSAGE));
		Assert.assertEquals(TEST_UID, event.getProperty(RfidSocketReaderService.EVENT_PARAM_UID));
		Assert.assertEquals(TEST_GROUP_UID,
				event.getProperty(RfidSocketReaderService.EVENT_PARAM_GROUP_UID));
		Assert.assertEquals(Long.valueOf(2),
				event.getProperty(RfidSocketReaderService.EVENT_PARAM_COUNT));
		Assert.assertNotNull(event.getProperty(RfidSocketReaderService.EVENT_PARAM_DATE));
	}

	private AtomicInteger startKeepGoingServerThread(final List<MockServerMessage> messages)
			throws Exception {
		final AtomicInteger count = new AtomicInteger(0);
		startServerThread(new Runnable() {

			@Override
			public void run() {
				log.debug("Server thread starting for messages {}", messages);
				PrintWriter out = null;
				while ( count.get() < messages.size() ) {
					log.debug("Server thread waiting for client @; next message #{}", count.get() + 1);
					try {
						Socket socket = server.accept();
						out = new PrintWriter(socket.getOutputStream(), true);
						out.println("HELLO");
						log.debug("RFID server sent HELLO, processing messages {}", messages);
						while ( count.get() < messages.size() ) {
							MockServerMessage msg = messages.get(count.get());
							Thread.sleep(msg.getPause());
							log.debug("Server thread sending message {} [{}]", count.get() + 1,
									msg.getMessage());
							out.println(msg.getMessage());
							count.incrementAndGet();
						}
					} catch ( IOException e ) {
						log.warn("RFID server IOException: {}", e.getMessage());
					} catch ( InterruptedException e ) {
						log.warn("RFID server InterruptedException: {}", e.getMessage());
					} finally {
						if ( out != null ) {
							out.close();
						}
					}
				}
				log.debug("Server thread exiting for messages {}, last index {}", messages,
						count.get() + 1);
			}
		});
		return count;
	}

	@Test
	public void watchdogTimeoutReconnect() throws Exception {
		List<MockServerMessage> msgs = Arrays.asList(new MockServerMessage(2100, "Message 1"));
		AtomicInteger count = startKeepGoingServerThread(msgs);

		int port = server.getLocalPort();

		eventAdmin = EasyMock.createMock(EventAdmin.class);

		Capture<Event> eventCapt = new Capture<Event>();
		eventAdmin.postEvent(capture(eventCapt));
		replay(eventAdmin);

		service = new RfidSocketReaderService();
		service.setPort(port);
		service.setConnectRetryMinutes(0);
		service.setEventAdmin(new StaticOptionalService<EventAdmin>(eventAdmin));
		service.setUid(TEST_UID);
		service.setGroupUID(TEST_GROUP_UID);
		service.setWatchdogSeconds(1);
		service.init();

		serverThread.join(2000);
		Thread.sleep(500); // sleep; watchdog timer should activate

		verify(eventAdmin);

		Assert.assertEquals("Messages served", 1, count.get());
	}
}

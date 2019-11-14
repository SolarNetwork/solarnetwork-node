/* ==================================================================
 * GpsdClientServiceTests.java - 13/11/2019 9:07:38 am
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

package net.solarnetwork.node.hw.gpsd.service.impl.test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import net.solarnetwork.node.hw.gpsd.domain.GpsdMessage;
import net.solarnetwork.node.hw.gpsd.domain.VersionMessage;
import net.solarnetwork.node.hw.gpsd.domain.WatchMessage;
import net.solarnetwork.node.hw.gpsd.service.GpsdMessageHandler;
import net.solarnetwork.node.hw.gpsd.service.impl.GpsdClientService;
import net.solarnetwork.node.hw.gpsd.test.GpsdMessageHandlerLatch;
import net.solarnetwork.node.hw.gpsd.test.GpsdServerTestSupport;

/**
 * Test cases for the {@link GpsdClientService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GpsdClientServiceTests extends GpsdServerTestSupport {

	private TaskScheduler taskScheduler;
	private GpsdClientService client;

	@Override
	@Before
	public void setup() {
		super.setup();
		setupGpsdServer();
		taskScheduler = new ThreadPoolTaskScheduler();
		client = new GpsdClientService(getMapper(), taskScheduler);
		client.setHost("127.0.0.1");
		client.setPort(gpsdServerPort());
	}

	@Override
	@After
	public void teardown() {
		client.shutdown();
		super.teardown();
	}

	@Test
	public void connectAndGetVersion() throws Exception {
		// GIVEN
		GpsdMessageHandlerLatch handler = new GpsdMessageHandlerLatch(new CountDownLatch(1));
		client.setMessageHandler(handler);

		// WHEN
		client.startup();
		boolean finished = handler.await(5, TimeUnit.SECONDS);

		// THEN
		assertThat("Finished with expected message count", finished, equalTo(true));
		List<GpsdMessage> messages = handler.getMessages();
		assertThat("Handled messages size 1 for version", messages, hasSize(1));
		assertThat("Handled message is version", messages.get(0), equalTo(DEFAULT_GPSD_VERSION));
	}

	@Test
	public void requestVersion() throws Exception {
		// GIVEN
		GpsdMessageHandlerLatch handler = new GpsdMessageHandlerLatch(new CountDownLatch(1));
		client.setMessageHandler(handler);
		client.startup();
		handler.await(5, TimeUnit.SECONDS);

		// WHEN
		Future<VersionMessage> result = client.requestGpsdVersion();

		// THEN
		assertThat("Future returned", result, notNullValue());
		VersionMessage response = result.get(5, TimeUnit.SECONDS);
		assertThat("Response is expected version", response, equalTo(DEFAULT_GPSD_VERSION));
	}

	@Test
	public void requestWatch() throws Exception {
		// GIVEN
		GpsdMessageHandlerLatch handler = new GpsdMessageHandlerLatch(new CountDownLatch(1));
		client.setMessageHandler(handler);
		client.startup();
		handler.await(5, TimeUnit.SECONDS);

		WatchMessage watch = WatchMessage.builder().withDevice("/dev/pts/1").withEnable(true)
				.withDumpJson(true).build();

		setGpsdServerMessageHandler(new GpsdMessageHandler() {

			@Override
			public void handleGpsdMessage(GpsdMessage message) {
				assertThat("Received message is WATCH", message, equalTo(watch));

				// echo same WATCH message in resopnse
				getGpsdServerMessagePublisher().publishMessage(watch);
			}
		});

		// WHEN
		Future<WatchMessage> result = client.configureWatchMode(watch);

		// THEN
		assertThat("Future returned", result, notNullValue());
		WatchMessage response = result.get(5, TimeUnit.SECONDS);
		assertThat("Response is expected", response, allOf(not(sameInstance(watch)), equalTo(watch)));

	}

}

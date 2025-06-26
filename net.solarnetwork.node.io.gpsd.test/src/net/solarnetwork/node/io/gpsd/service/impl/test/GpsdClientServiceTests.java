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

package net.solarnetwork.node.io.gpsd.service.impl.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessage;
import net.solarnetwork.node.io.gpsd.domain.NmeaMode;
import net.solarnetwork.node.io.gpsd.domain.SatelliteInfo;
import net.solarnetwork.node.io.gpsd.domain.SkyReportMessage;
import net.solarnetwork.node.io.gpsd.domain.TpvReportMessage;
import net.solarnetwork.node.io.gpsd.domain.VersionMessage;
import net.solarnetwork.node.io.gpsd.domain.WatchMessage;
import net.solarnetwork.node.io.gpsd.service.GpsdMessageHandler;
import net.solarnetwork.node.io.gpsd.service.GpsdMessageListener;
import net.solarnetwork.node.io.gpsd.service.impl.GpsdClientService;
import net.solarnetwork.node.io.gpsd.test.GpsdMessageHandlerLatch;
import net.solarnetwork.node.io.gpsd.test.GpsdServerTestSupport;

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

	@Test
	public void receiveTpvReport() throws Exception {
		// GIVEN
		GpsdMessageHandlerLatch handler = new GpsdMessageHandlerLatch(new CountDownLatch(1));
		client.setMessageHandler(handler);
		client.startup();
		handler.await(5, TimeUnit.SECONDS);

		// @formatter:off
		TpvReportMessage tpv = TpvReportMessage.builder().withDevice("/dev/pts/1")
				.withTimestamp(Instant.now())
				.withTimestampError(new BigDecimal("0.005"))
				.withLatitude(new BigDecimal("46.498293369"))
				.withLongitude(new BigDecimal("7.567411672"))
				.withAltitude(new BigDecimal("1343.127"))
				.withCourse(new BigDecimal("10.3788"))
				.withSpeed(new BigDecimal("0.091"))
				.withClimbRate(new BigDecimal("-0.085"))
				.withMode(NmeaMode.ThreeDimensional)
				.build();
		// @formatter:on

		CompletableFuture<TpvReportMessage> received = new CompletableFuture<TpvReportMessage>();
		client.addMessageListener(TpvReportMessage.class, new GpsdMessageListener<TpvReportMessage>() {

			@Override
			public void onGpsdMessage(TpvReportMessage message) {
				received.complete(message);
			}
		});

		// WHEN
		getGpsdServerMessagePublisher().publishMessage(tpv).get(5, TimeUnit.SECONDS);

		// THEN
		TpvReportMessage result = received.get(5, TimeUnit.SECONDS);
		assertThat("TPV report", result, equalTo(tpv));
	}

	@Test
	public void receiveSkyReport() throws Exception {
		// GIVEN
		GpsdMessageHandlerLatch handler = new GpsdMessageHandlerLatch(new CountDownLatch(1));
		client.setMessageHandler(handler);
		client.startup();
		handler.await(5, TimeUnit.SECONDS);

		// @formatter:off
		List<SatelliteInfo> sats = Arrays.asList(
				SatelliteInfo.builder()
					.withPrn(1)
					.withElevation(2)
					.withAzimuth(3)
					.withSignalStrength(4)
					.withUsed(false)
					.build(),
				SatelliteInfo.builder()
					.withPrn(2)
					.withElevation(3)
					.withAzimuth(4)
					.withSignalStrength(5)
					.withUsed(true)
					.build()
				);

		SkyReportMessage sky = SkyReportMessage.builder()
				.withDevice("/dev/pts/1")
				.withTimestamp(Instant.now())
				.withLongitudeDop(1)
				.withLatitudeDop(2)
				.withAltitudeDop(3)
				.withTimestampDop(4)
				.withHorizontalDop(5)
				.withSphericalDop(6)
				.withHypersphericalDop(7)
				.withSatellites(sats)
				.build();
		// @formatter:on

		CompletableFuture<SkyReportMessage> received = new CompletableFuture<SkyReportMessage>();
		client.addMessageListener(SkyReportMessage.class, new GpsdMessageListener<SkyReportMessage>() {

			@Override
			public void onGpsdMessage(SkyReportMessage message) {
				received.complete(message);
			}
		});

		// WHEN
		getGpsdServerMessagePublisher().publishMessage(sky).get(5, TimeUnit.SECONDS);

		// THEN
		SkyReportMessage result = received.get(5, TimeUnit.SECONDS);
		assertThat("SKY report", result, equalTo(sky));
	}

	@Test
	public void ignoreRolloverTimeCompensation() throws Exception {
		// GIVEN
		client.setGpsRolloverCompensation(false);

		Instant inputTime = Instant.parse("2000-04-02T00:48:39.000Z");
		TpvReportMessage msg = TpvReportMessage.builder().withTimestamp(inputTime).build();

		GpsdMessageHandlerLatch handler = new GpsdMessageHandlerLatch(new CountDownLatch(1));
		client.setMessageHandler(handler);

		// WHEN
		client.handleGpsdMessage(msg);

		handler.await(5, TimeUnit.SECONDS);
		assertThat("Message available", handler.getMessages(), hasSize(1));
		GpsdMessage result = handler.getMessages().get(0);
		assertThat("TpvReportMessage passed", result, instanceOf(TpvReportMessage.class));
		assertThat("Report time not adjusted", ((TpvReportMessage) result).getTimestamp(),
				equalTo(inputTime));
	}

	@Test
	public void handleRolloverTimeCompensation() throws Exception {
		// GIVEN
		OffsetDateTime now = Instant.now().atOffset(ZoneOffset.UTC);
		Instant inputTime = Instant.parse("2000-04-02T00:48:39.000Z");
		TpvReportMessage msg = TpvReportMessage.builder().withTimestamp(inputTime).build();

		GpsdMessageHandlerLatch handler = new GpsdMessageHandlerLatch(new CountDownLatch(1));
		client.setMessageHandler(handler);

		// WHEN
		client.handleGpsdMessage(msg);

		handler.await(5, TimeUnit.SECONDS);
		assertThat("Message available", handler.getMessages(), hasSize(1));
		GpsdMessage result = handler.getMessages().get(0);
		assertThat("TpvReportMessage passed", result, instanceOf(TpvReportMessage.class));
		OffsetDateTime reportTime = ((TpvReportMessage) result).getTimestamp().atOffset(ZoneOffset.UTC);

		assertThat("Report time year adjusted", reportTime.get(ChronoField.YEAR),
				equalTo(now.get(ChronoField.YEAR)));
		assertThat("Report time day-of-year adjusted", reportTime.get(ChronoField.DAY_OF_YEAR),
				equalTo(now.get(ChronoField.DAY_OF_YEAR)));
	}

	@Test
	public void handleRolloverTimeCompensationWithListener() throws Exception {
		// GIVEN
		OffsetDateTime now = Instant.now().atOffset(ZoneOffset.UTC);
		Instant inputTime = Instant.parse("2000-04-02T00:48:39.000Z");
		TpvReportMessage msg = TpvReportMessage.builder().withTimestamp(inputTime).build();

		GpsdMessageHandlerLatch handler = new GpsdMessageHandlerLatch(new CountDownLatch(1));
		client.setMessageHandler(handler);

		CompletableFuture<TpvReportMessage> received = new CompletableFuture<TpvReportMessage>();
		client.addMessageListener(TpvReportMessage.class, new GpsdMessageListener<TpvReportMessage>() {

			@Override
			public void onGpsdMessage(TpvReportMessage message) {
				received.complete(message);
			}
		});

		// WHEN
		client.handleGpsdMessage(msg);

		handler.await(5, TimeUnit.SECONDS);
		assertThat("Message available", handler.getMessages(), hasSize(1));
		GpsdMessage result = handler.getMessages().get(0);
		assertThat("TpvReportMessage passed", result, instanceOf(TpvReportMessage.class));
		OffsetDateTime reportTime = ((TpvReportMessage) result).getTimestamp().atOffset(ZoneOffset.UTC);

		assertThat("Report time year adjusted", reportTime.get(ChronoField.YEAR),
				equalTo(now.get(ChronoField.YEAR)));
		assertThat("Report time day-of-year adjusted", reportTime.get(ChronoField.DAY_OF_YEAR),
				equalTo(now.get(ChronoField.DAY_OF_YEAR)));

		TpvReportMessage report = received.getNow(null);
		assertThat("Listener received report", report, notNullValue());
		assertThat("Listener message same as handler", report, sameInstance(result));
	}

}

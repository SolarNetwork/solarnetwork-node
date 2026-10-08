/* ==================================================================
 * LiveDatumServiceTests.java - 10/10/2026 1:42:00 PM
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.stomp.server.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumHistorian;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.setup.stomp.LiveHeader;
import net.solarnetwork.node.setup.stomp.SetupHeader;
import net.solarnetwork.node.setup.stomp.SetupStatus;
import net.solarnetwork.node.setup.stomp.SetupTopic;
import net.solarnetwork.node.setup.stomp.server.LiveDatumModeManager;
import net.solarnetwork.node.setup.stomp.server.LiveDatumService;
import net.solarnetwork.node.setup.stomp.server.SetupSession;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link LiveDatumService} class.
 *
 * <p>
 * Message encoding is covered by {@link LiveDatumPublisherTests} and
 * operational mode management by {@link LiveDatumModeManagerTests}; these tests
 * cover subscriptions, event handling, housekeeping, and how they drive the
 * mode manager.
 * </p>
 *
 * @author elijah
 * @version 1.1
 */
public class LiveDatumServiceTests extends LiveDatumTestSupport {

	private LiveDatumService service;

	@Before
	public void setup() {
		service = newService(new StaticOptionalService<>(opModes),
				new StaticOptionalService<>(null), objectMapper);
	}

	private LiveDatumService newService(StaticOptionalService<OperationalModesService> ops,
			StaticOptionalService<DatumService> ds, ObjectMapper mapper) {
		LiveDatumService s = new LiveDatumService(ops, ds, mapper);
		s.setTaskScheduler(scheduler);
		s.setClock(clock);
		return s;
	}

	private void publish(NodeDatum d) {
		service.handleEvent(DatumEvents.datumCapturedEvent(d));
	}

	private SetupStatus subscribe(String subId, String props, String interval) {
		return service.subscribe(session, subId, SOURCE_ID, props, interval);
	}

	/*-------------------------------------------------------------------------
	 * Subscribe
	 *-----------------------------------------------------------------------*/

	@Test
	public void subscribe_ok_modeEnabledOnlyOnScheduler() {
		// WHEN
		SetupStatus result = subscribe("live-1", PROPS, "1000");

		// THEN
		assertThat("Subscribe OK", result, is(SetupStatus.Ok));
		assertThat("Subscription registered", service.getSubscriptionCount(), is(1));
		assertThat("Mode not enabled on calling thread", opModes.enableCalls, hasSize(0));
		assertNoFrame();

		// run the scheduled mode sync
		runScheduledTasks();

		final long exp = clock.millis() + LiveDatumModeManager.DEFAULT_MODE_EXPIRE_SECS * 1000L;
		assertThat("Mode enabled with expiration from scheduled task", opModes.enableCalls,
				contains(MODE + "@" + exp));
		assertThat("Housekeeping scheduled", scheduler.activePeriodic(), hasSize(1));
	}

	@Test
	public void subscribe_missingSource() {
		// WHEN
		SetupStatus result = service.subscribe(session, "live-1", "  ", PROPS, null);

		// THEN
		assertThat("Rejected", result, is(SetupStatus.Unprocessable));
		StompFrame f = nextFrame();
		assertStatus(f, SetupStatus.Unprocessable);
		assertThat("Subscription header", f.headers().getAsString(StompHeaders.SUBSCRIPTION),
				is("live-1"));
		assertThat("Destination", f.headers().getAsString(StompHeaders.DESTINATION),
				is(SetupTopic.DatumLive.getValue()));
		assertThat("Message", f.headers().getAsString(StompHeaders.MESSAGE),
				containsString("source-id"));
		assertThat("Not registered", service.getSubscriptionCount(), is(0));
		assertThat("No tasks scheduled", scheduler.getTasks(), hasSize(0));
		assertThat("Channel still open", channel.isActive(), is(true));
	}

	@Test
	public void subscribe_badInterval() {
		// WHEN
		SetupStatus result = subscribe("live-1", PROPS, "fast");

		// THEN
		assertThat("Rejected", result, is(SetupStatus.Unprocessable));
		StompFrame f = nextFrame();
		assertStatus(f, SetupStatus.Unprocessable);
		assertThat("Source ID header", f.headers().getAsString(LiveHeader.SourceId.getValue()),
				is(SOURCE_ID));
		assertThat("Not registered", service.getSubscriptionCount(), is(0));
	}

	@Test
	public void subscribe_tooManyProperties() {
		// GIVEN
		service.setMaxProperties(2);

		// WHEN
		SetupStatus result = subscribe("live-1", "a,b,c", null);

		// THEN
		assertThat("Rejected", result, is(SetupStatus.Unprocessable));
		assertStatus(nextFrame(), SetupStatus.Unprocessable);
	}

	@Test
	public void subscribe_duplicateProperties_dedupedBeforeLimit() {
		// GIVEN
		service.setMaxProperties(2);

		// WHEN
		SetupStatus result = subscribe("live-1", " watts, watts ,current,", null);

		// THEN
		assertThat("Accepted", result, is(SetupStatus.Ok));
	}

	@Test
	public void subscribe_duplicateId() {
		// GIVEN
		subscribe("live-1", PROPS, null);

		// WHEN
		SetupStatus result = subscribe("live-1", PROPS, null);

		// THEN
		assertThat("Rejected", result, is(SetupStatus.Unprocessable));
		assertStatus(nextFrame(), SetupStatus.Unprocessable);
		assertThat("Still one subscription", service.getSubscriptionCount(), is(1));
	}

	@Test
	public void subscribe_tooManyPerSession() {
		// GIVEN
		subscribe("live-1", PROPS, null);
		subscribe("live-2", PROPS, null);

		// WHEN
		SetupStatus result = subscribe("live-3", PROPS, null);

		// THEN
		assertThat("Rejected", result, is(SetupStatus.TooManyRequests));
		assertStatus(nextFrame(), SetupStatus.TooManyRequests);
		assertThat("Two subscriptions", service.getSubscriptionCount(), is(2));
	}

	@Test
	public void subscribe_tooManyGlobal() {
		// GIVEN
		service.setMaxSubscriptions(1);
		EmbeddedChannel ch2 = new EmbeddedChannel();
		SetupSession s2 = new SetupSession("setup", ch2);
		service.subscribe(s2, "live-1", SOURCE_ID, PROPS, null);

		// WHEN
		SetupStatus result = subscribe("live-1", PROPS, null);

		// THEN
		assertThat("Rejected", result, is(SetupStatus.TooManyRequests));
		assertStatus(nextFrame(), SetupStatus.TooManyRequests);
		ch2.finishAndReleaseAll();
	}

	@Test
	public void subscribe_noOpModesService() {
		// GIVEN
		service = newService(new StaticOptionalService<>(null), new StaticOptionalService<>(null),
				objectMapper);

		// WHEN
		SetupStatus result = subscribe("live-1", PROPS, null);

		// THEN
		assertThat("Rejected", result, is(SetupStatus.ServiceUnavailable));
		assertStatus(nextFrame(), SetupStatus.ServiceUnavailable);
		assertThat("Not registered", service.getSubscriptionCount(), is(0));
	}

	@Test
	public void subscribe_sendsLatestDatum() throws Exception {
		// GIVEN
		DatumService ds = EasyMock.createMock(DatumService.class);
		DatumHistorian unfiltered = EasyMock.createMock(DatumHistorian.class);
		final Instant ts = clock.instant().minusSeconds(2);
		expect(ds.unfiltered()).andReturn(unfiltered);
		expect(unfiltered.latest(SOURCE_ID, NodeDatum.class)).andReturn(meterDatum(SOURCE_ID, ts));
		EasyMock.replay(ds, unfiltered);
		service = newService(new StaticOptionalService<>(opModes), new StaticOptionalService<>(ds),
				objectMapper);

		// WHEN
		SetupStatus result = subscribe("live-1", "watts", null);

		// THEN
		assertThat("Accepted", result, is(SetupStatus.Ok));
		StompFrame f = nextFrame();
		assertStatus(f, SetupStatus.Ok);
		Map<String, Object> body = body(f);
		assertThat("Timestamp", ((Number) body.get("t")).longValue(), is(ts.toEpochMilli()));
		assertThat("Watts", ((Number) body.get("watts")).intValue(), is(-1520));
		EasyMock.verify(ds, unfiltered);
	}

	@Test
	public void subscribe_staleLatestDatum_sentButNotConfirmed() {
		// GIVEN
		DatumService ds = EasyMock.createMock(DatumService.class);
		DatumHistorian unfiltered = EasyMock.createMock(DatumHistorian.class);
		final Instant ts = clock.instant().minusSeconds(600);
		expect(ds.unfiltered()).andReturn(unfiltered);
		expect(unfiltered.latest(SOURCE_ID, NodeDatum.class)).andReturn(meterDatum(SOURCE_ID, ts));
		EasyMock.replay(ds, unfiltered);
		service = newService(new StaticOptionalService<>(opModes), new StaticOptionalService<>(ds),
				objectMapper);

		// WHEN
		subscribe("live-1", "watts", null);

		// THEN the old datum is still sent
		assertStatus(nextFrame(), SetupStatus.Ok);

		// WHEN no fresh datum arrives within the grace period
		clock.advance(Duration.ofSeconds(LiveDatumService.DEFAULT_SOURCE_GRACE_SECS));
		service.housekeeping();

		// THEN
		assertStatus(nextFrame(), SetupStatus.NotFound);
		assertThat("Removed", service.getSubscriptionCount(), is(0));
		EasyMock.verify(ds, unfiltered);
	}

	@Test
	public void subscribe_afterShutdown_unavailable() {
		// GIVEN
		service.shutdown();

		// WHEN
		SetupStatus result = subscribe("live-1", PROPS, null);

		// THEN
		assertThat("Rejected", result, is(SetupStatus.ServiceUnavailable));
	}

	/*-------------------------------------------------------------------------
	 * Events
	 *-----------------------------------------------------------------------*/

	@Test
	public void event_requestedPropertiesPublished() throws Exception {
		// GIVEN
		subscribe("live-1", " watts, current ,powerFactor", null);
		final Instant ts = clock.instant();

		// WHEN
		publish(meterDatum(SOURCE_ID, ts));

		// THEN
		StompFrame f = nextFrame();
		assertStatus(f, SetupStatus.Ok);
		assertThat("Subscription", f.headers().getAsString(StompHeaders.SUBSCRIPTION),
				is("live-1"));
		assertThat("Source ID", f.headers().getAsString(LiveHeader.SourceId.getValue()),
				is(SOURCE_ID));
		assertThat("Only timestamp and requested present properties", body(f).keySet(),
				contains("t", "watts", "current"));
	}

	@Test
	public void event_noPropertiesHeader_defaultPropertiesPublished() throws Exception {
		// GIVEN
		subscribe("live-1", null, null);
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("watts", 100);
		s.putAccumulatingSampleValue("wattHours", 12345L);
		s.putStatusSampleValue("phase", "Total");

		// WHEN
		publish(SimpleDatum.nodeDatum(SOURCE_ID, clock.instant(), s));

		// THEN
		assertThat("Instantaneous and accumulating, but not status", body(nextFrame()).keySet(),
				contains("t", "watts", "wattHours"));
	}

	@Test
	public void event_encodingFailure_subscriptionKept() {
		// GIVEN
		ObjectMapper failing = new ObjectMapper() {

			private static final long serialVersionUID = 1L;

			@Override
			public byte[] writeValueAsBytes(Object value) throws JsonProcessingException {
				throw JsonMappingException.fromUnexpectedIOE(new java.io.IOException("boom"));
			}

		};
		service = newService(new StaticOptionalService<>(opModes), new StaticOptionalService<>(null),
				failing);
		subscribe("live-1", PROPS, null);

		// WHEN
		publish(meterDatum(SOURCE_ID, clock.instant()));

		// THEN
		assertNoFrame();
		assertThat("Channel still open", channel.isActive(), is(true));
		assertThat("Still subscribed", service.getSubscriptionCount(), is(1));
	}

	@Test
	public void event_otherSource_noWrite() {
		// GIVEN
		subscribe("live-1", PROPS, null);

		// WHEN
		publish(meterDatum(OTHER_SOURCE_ID, clock.instant()));

		// THEN
		assertNoFrame();
	}

	@Test
	public void event_sameOrOlderTimestamp_suppressed() {
		// GIVEN
		subscribe("live-1", PROPS, null);
		final Instant ts = clock.instant();
		publish(meterDatum(SOURCE_ID, ts));
		nextFrame();

		// WHEN
		publish(meterDatum(SOURCE_ID, ts));
		publish(meterDatum(SOURCE_ID, ts.minusSeconds(5)));

		// THEN
		assertNoFrame();
	}

	@Test
	public void event_intervalThrottle() {
		// GIVEN
		subscribe("live-1", PROPS, "5000");
		final Instant ts = clock.instant();
		publish(meterDatum(SOURCE_ID, ts));
		nextFrame();

		// WHEN
		publish(meterDatum(SOURCE_ID, ts.plusMillis(1000)));
		publish(meterDatum(SOURCE_ID, ts.plusMillis(3000)));

		// THEN
		assertNoFrame();

		// WHEN (within 20% jitter tolerance of the interval)
		publish(meterDatum(SOURCE_ID, ts.plusMillis(4000)));

		// THEN
		nextFrame();
	}

	@Test
	public void event_intervalClampedToMinimum() {
		// GIVEN
		subscribe("live-1", PROPS, "10");
		final Instant ts = clock.instant();
		publish(meterDatum(SOURCE_ID, ts));
		nextFrame();

		// WHEN
		publish(meterDatum(SOURCE_ID, ts.plusMillis(100)));

		// THEN
		assertNoFrame();
	}

	@Test
	public void event_jitterTolerated() {
		// GIVEN
		subscribe("live-1", PROPS, "1000");
		final Instant ts = clock.instant();
		publish(meterDatum(SOURCE_ID, ts));
		nextFrame();

		// WHEN
		publish(meterDatum(SOURCE_ID, ts.plusMillis(900)));

		// THEN
		nextFrame();
	}

	@Test
	public void event_channelNotWritable_dropped() {
		// GIVEN
		subscribe("live-1", PROPS, null);
		final Instant ts = clock.instant();
		channel.unsafe().outboundBuffer().setUserDefinedWritability(1, false);

		// WHEN
		publish(meterDatum(SOURCE_ID, ts));

		// THEN
		assertNoFrame();

		// WHEN writable again, the next sample is sent
		channel.unsafe().outboundBuffer().setUserDefinedWritability(1, true);
		publish(meterDatum(SOURCE_ID, ts.plusSeconds(1)));

		// THEN
		nextFrame();
	}

	/*-------------------------------------------------------------------------
	 * Subscription lifecycle and operational mode
	 *-----------------------------------------------------------------------*/

	@Test
	public void unsubscribe_lingerThenDisable() {
		// GIVEN
		subscribe("live-1", PROPS, null);
		runScheduledTasks();

		// WHEN
		boolean removed = service.unsubscribe(session, "live-1");
		runScheduledTasks();

		// THEN
		assertThat("Removed", removed, is(true));
		assertThat("Not disabled during linger", opModes.disableCalls, hasSize(0));
		assertThat("Housekeeping cancelled", scheduler.activePeriodic(), hasSize(0));

		// WHEN linger passes
		clock.advance(Duration.ofSeconds(LiveDatumModeManager.DEFAULT_MODE_LINGER_SECS));
		runScheduledTasks();

		// THEN
		assertThat("Disabled after linger", opModes.disableCalls, contains(MODE));
		assertThat("Mode inactive", opModes.isOperationalModeActive(MODE), is(false));
	}

	@Test
	public void unsubscribe_unknown() {
		assertThat("Nothing removed", service.unsubscribe(session, "nope"), is(false));
	}

	@Test
	public void refCounting_disableOnlyAfterLast() {
		// GIVEN
		subscribe("live-1", PROPS, null);
		subscribe("live-2", PROPS, null);
		runScheduledTasks();

		// WHEN
		service.unsubscribe(session, "live-1");
		runScheduledTasks();
		clock.advance(Duration.ofSeconds(60));
		runScheduledTasks();

		// THEN
		assertThat("Not disabled while a subscription remains", opModes.disableCalls, hasSize(0));

		// WHEN
		service.unsubscribe(session, "live-2");
		runScheduledTasks();
		clock.advance(Duration.ofSeconds(LiveDatumModeManager.DEFAULT_MODE_LINGER_SECS));
		runScheduledTasks();

		// THEN
		assertThat("Disabled after last", opModes.disableCalls, contains(MODE));
	}

	@Test
	public void sessionClosed_removesAll() {
		// GIVEN
		subscribe("live-1", PROPS, null);
		subscribe("live-2", PROPS, null);
		runScheduledTasks();

		// WHEN
		int count = service.sessionClosed(session.getSessionId());

		// THEN
		assertThat("Both removed", count, is(2));
		assertThat("None left", service.getSubscriptionCount(), is(0));
		publish(meterDatum(SOURCE_ID, clock.instant()));
		assertNoFrame();

		// WHEN linger passes
		runScheduledTasks();
		clock.advance(Duration.ofSeconds(LiveDatumModeManager.DEFAULT_MODE_LINGER_SECS));
		runScheduledTasks();

		// THEN
		assertThat("Mode released", opModes.disableCalls, contains(MODE));
		assertThat("Housekeeping cancelled", scheduler.activePeriodic(), hasSize(0));
	}

	@Test
	public void reenable_ifRemovedExternally() {
		// GIVEN
		subscribe("live-1", PROPS, null);
		runScheduledTasks();
		opModes.disableOperationalModes(Collections.singleton(MODE));
		opModes.enableCalls.clear();

		// WHEN
		service.housekeeping();

		// THEN
		assertThat("Re-enabled", opModes.enableCalls, hasSize(1));
		assertThat("Mode active", opModes.isOperationalModeActive(MODE), is(true));
	}

	@Test
	public void shutdown_notifiesAndDisables() {
		// GIVEN
		subscribe("live-1", PROPS, null);
		runScheduledTasks();

		// WHEN
		service.shutdown();

		// THEN
		assertStatus(nextFrame(), SetupStatus.ServiceUnavailable);
		assertThat("Disabled synchronously", opModes.disableCalls, contains(MODE));
		assertThat("No subscriptions", service.getSubscriptionCount(), is(0));
		assertThat("Housekeeping cancelled", scheduler.activePeriodic(), hasSize(0));
	}

	/*-------------------------------------------------------------------------
	 * Housekeeping
	 *-----------------------------------------------------------------------*/

	@Test
	public void housekeeping_maxDuration() {
		// GIVEN
		service.setMaxDurationSecs(60);
		subscribe("live-1", PROPS, null);
		runScheduledTasks();
		publish(meterDatum(SOURCE_ID, clock.instant()));
		nextFrame();

		// WHEN
		clock.advance(Duration.ofSeconds(60));
		service.housekeeping();

		// THEN
		StompFrame f = nextFrame();
		assertStatus(f, SetupStatus.Gone);
		assertThat("Removed", service.getSubscriptionCount(), is(0));
	}

	@Test
	public void housekeeping_noDatumWithinGrace() {
		// GIVEN
		subscribe("live-1", PROPS, null);
		runScheduledTasks();

		// WHEN within grace
		clock.advance(Duration.ofSeconds(LiveDatumService.DEFAULT_SOURCE_GRACE_SECS - 1));
		service.housekeeping();

		// THEN
		assertNoFrame();

		// WHEN grace passes
		clock.advance(Duration.ofSeconds(1));
		service.housekeeping();

		// THEN
		assertStatus(nextFrame(), SetupStatus.NotFound);
		assertThat("Removed", service.getSubscriptionCount(), is(0));
	}

	@Test
	public void housekeeping_staleOnceThenRecover() {
		// GIVEN
		subscribe("live-1", PROPS, null);
		runScheduledTasks();
		publish(meterDatum(SOURCE_ID, clock.instant()));
		nextFrame();

		// WHEN
		clock.advance(Duration.ofSeconds(LiveDatumService.DEFAULT_STALE_SECS));
		service.housekeeping();
		service.housekeeping();

		// THEN only one advisory
		StompFrame f = nextFrame();
		assertStatus(f, SetupStatus.ServiceUnavailable);
		assertNoFrame();
		assertThat("Still subscribed", service.getSubscriptionCount(), is(1));

		// WHEN data resumes
		publish(meterDatum(SOURCE_ID, clock.instant()));

		// THEN
		assertStatus(nextFrame(), SetupStatus.Ok);

		// WHEN stale again
		clock.advance(Duration.ofSeconds(LiveDatumService.DEFAULT_STALE_SECS));
		service.housekeeping();

		// THEN another advisory
		assertStatus(nextFrame(), SetupStatus.ServiceUnavailable);
	}

	@Test
	public void housekeeping_closedChannelRemoved() {
		// GIVEN
		subscribe("live-1", PROPS, null);
		runScheduledTasks();

		// WHEN
		channel.close();
		service.housekeeping();

		// THEN
		assertThat("Removed", service.getSubscriptionCount(), is(0));
	}

	@Test
	public void terminalStatus_noFurtherMessages() {
		// GIVEN
		service.setMaxDurationSecs(60);
		subscribe("live-1", PROPS, null);
		publish(meterDatum(SOURCE_ID, clock.instant()));
		nextFrame();
		clock.advance(Duration.ofSeconds(60));
		service.housekeeping();
		assertStatus(nextFrame(), SetupStatus.Gone);

		// WHEN
		publish(meterDatum(SOURCE_ID, clock.instant()));

		// THEN
		assertNoFrame();
	}

	@Test
	public void terminalStatus_datumDuringStatusWrite_notSent() {
		// GIVEN a channel that delivers a datum event while the terminal status is being
		// written, simulating an event thread racing the housekeeping thread
		service.setMaxDurationSecs(60);
		final java.util.List<StompFrame> written = new java.util.ArrayList<>();
		EmbeddedChannel ch = new EmbeddedChannel(new io.netty.channel.ChannelOutboundHandlerAdapter() {

			@Override
			public void write(io.netty.channel.ChannelHandlerContext ctx, Object msg,
					io.netty.channel.ChannelPromise promise) throws Exception {
				StompFrame f = (StompFrame) msg;
				written.add(f);
				if ( String.valueOf(SetupStatus.Gone.getCode())
						.equals(f.headers().getAsString(SetupHeader.Status.getValue())) ) {
					publish(meterDatum(SOURCE_ID, clock.instant()));
				}
				promise.setSuccess();
			}

		});
		SetupSession s = new SetupSession("setup", ch);
		service.subscribe(s, "live-1", SOURCE_ID, PROPS, null);
		publish(meterDatum(SOURCE_ID, clock.instant()));
		clock.advance(Duration.ofSeconds(60));

		// WHEN
		service.housekeeping();

		// THEN
		assertThat("Datum then terminal status only", written, hasSize(2));
		assertStatus(written.get(1), SetupStatus.Gone);
		ch.finishAndReleaseAll();
	}

}

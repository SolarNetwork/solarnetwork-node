/* ==================================================================
 * LiveDatumPublisherTests.java - 10/10/2026 1:42:00 PM
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

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;
import io.netty.handler.codec.stomp.StompSubframeEncoder;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.setup.stomp.LiveHeader;
import net.solarnetwork.node.setup.stomp.SetupStatus;
import net.solarnetwork.node.setup.stomp.SetupTopic;
import net.solarnetwork.node.setup.stomp.StompUtils;
import net.solarnetwork.node.setup.stomp.server.LiveDatumPublisher;
import net.solarnetwork.node.setup.stomp.server.LiveDatumSubscription;
import net.solarnetwork.node.setup.stomp.server.SetupSession;

/**
 * Test cases for the {@link LiveDatumPublisher} class.
 *
 * @author elijah
 * @version 1.0
 */
public class LiveDatumPublisherTests extends LiveDatumTestSupport {

	private LiveDatumPublisher publisher;

	@Before
	public void setup() {
		publisher = new LiveDatumPublisher(objectMapper);
	}

	private LiveDatumSubscription sub(SetupSession s, String subId, String sourceId,
			List<String> props) {
		final long now = clock.millis();
		return new LiveDatumSubscription(s, subId, sourceId, props, 1000L, now, now + 60_000L);
	}

	@Test
	public void publishDatum_headersAndBody() throws Exception {
		// GIVEN
		LiveDatumSubscription sub = sub(session, "live-1", SOURCE_ID,
				asList("watts", "current", "powerFactor"));
		final Instant ts = clock.instant();

		// WHEN
		boolean result = publisher.publishDatum(sub, meterDatum(SOURCE_ID, ts), ts.toEpochMilli());

		// THEN
		assertThat("Published", result, is(true));
		StompFrame f = nextFrame();
		assertStatus(f, SetupStatus.Ok);
		assertThat("Destination", f.headers().getAsString(StompHeaders.DESTINATION),
				is(SetupTopic.DatumLive.getValue()));
		assertThat("Subscription", f.headers().getAsString(StompHeaders.SUBSCRIPTION),
				is("live-1"));
		assertThat("Message ID", f.headers().getAsString(StompHeaders.MESSAGE_ID),
				is(notNullValue()));
		assertThat("Source ID", f.headers().getAsString(LiveHeader.SourceId.getValue()),
				is(SOURCE_ID));
		assertThat("No message header", f.headers().getAsString(StompHeaders.MESSAGE),
				is(nullValue()));
		assertThat("Content type", f.headers().getAsString(StompHeaders.CONTENT_TYPE),
				is(StompUtils.JSON_UTF8_CONTENT_TYPE));
		assertThat("Content length", f.headers().getAsString(StompHeaders.CONTENT_LENGTH),
				is(String.valueOf(f.content().readableBytes())));

		Map<String, Object> body = body(f);
		assertThat("Only timestamp and requested present properties", body.keySet(),
				contains("t", "watts", "current"));
		assertThat("Timestamp", ((Number) body.get("t")).longValue(), is(ts.toEpochMilli()));
		assertThat("Missing property omitted", body, not(hasKey("powerFactor")));
		assertThat("Unrequested property omitted", body, not(hasKey("voltage")));
	}

	@Test
	public void publishDatum_messageIdsIncrementPerSession() {
		// GIVEN
		LiveDatumSubscription sub = sub(session, "live-1", SOURCE_ID, asList("watts"));
		final Instant ts = clock.instant();

		// WHEN
		publisher.publishDatum(sub, meterDatum(SOURCE_ID, ts), ts.toEpochMilli());
		LiveDatumPublisher.publishStatus(sub, SetupStatus.ServiceUnavailable, "No recent data.");

		// THEN
		int id1 = Integer.parseInt(nextFrame().headers().getAsString(StompHeaders.MESSAGE_ID));
		int id2 = Integer.parseInt(nextFrame().headers().getAsString(StompHeaders.MESSAGE_ID));
		assertThat("Message IDs increment", id2, is(id1 + 1));
	}

	@Test
	public void publishDatum_defaultProperties() throws Exception {
		// GIVEN
		LiveDatumSubscription sub = sub(session, "live-1", SOURCE_ID, Collections.emptyList());
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("watts", 100);
		s.putAccumulatingSampleValue("wattHours", 12345L);
		s.putStatusSampleValue("phase", "Total");
		final Instant ts = clock.instant();

		// WHEN
		publisher.publishDatum(sub, SimpleDatum.nodeDatum(SOURCE_ID, ts, s), ts.toEpochMilli());

		// THEN
		Map<String, Object> body = body(nextFrame());
		assertThat("Instantaneous and accumulating, but not status", body.keySet(),
				contains("t", "watts", "wattHours"));
	}

	@Test
	public void publishDatum_propertyNamedTimestamp_doesNotOverrideTimestamp() throws Exception {
		// GIVEN
		final Instant ts = clock.instant();
		final long tsMs = ts.toEpochMilli();

		// WHEN
		// requested by default
		publisher.publishDatum(sub(session, "live-1", SOURCE_ID, Collections.emptyList()),
				datum(SOURCE_ID, ts, Collections.singletonMap("t", 99)), tsMs);

		// AND
		// requested explicitly
		publisher.publishDatum(sub(session, "live-2", SOURCE_ID, asList("t")),
				datum(SOURCE_ID, ts, Collections.singletonMap("t", 99)), tsMs);

		// THEN
		assertThat("Timestamp kept (default)", ((Number) body(nextFrame()).get("t")).longValue(),
				is(tsMs));
		assertThat("Timestamp kept (explicit)", ((Number) body(nextFrame()).get("t")).longValue(),
				is(tsMs));
	}

	@Test
	public void publishDatum_channelNotWritable() {
		// GIVEN
		LiveDatumSubscription sub = sub(session, "live-1", SOURCE_ID, asList("watts"));
		channel.unsafe().outboundBuffer().setUserDefinedWritability(1, false);
		final Instant ts = clock.instant();

		// WHEN
		boolean result = publisher.publishDatum(sub, meterDatum(SOURCE_ID, ts), ts.toEpochMilli());

		// THEN
		assertThat("Not published", result, is(false));
		assertNoFrame();
	}

	@Test
	public void publishDatum_channelClosed() {
		// GIVEN
		LiveDatumSubscription sub = sub(session, "live-1", SOURCE_ID, asList("watts"));
		channel.close();
		final Instant ts = clock.instant();

		// WHEN
		boolean result = publisher.publishDatum(sub, meterDatum(SOURCE_ID, ts), ts.toEpochMilli());

		// THEN
		assertThat("Not published", result, is(false));
	}

	@Test
	public void publishDatum_encodingFailure_noFrame() {
		// GIVEN
		ObjectMapper failing = new ObjectMapper() {

			private static final long serialVersionUID = 1L;

			@Override
			public byte[] writeValueAsBytes(Object value) throws JsonProcessingException {
				throw JsonMappingException.fromUnexpectedIOE(new IOException("boom"));
			}

		};
		publisher = new LiveDatumPublisher(failing);
		LiveDatumSubscription sub = sub(session, "live-1", SOURCE_ID, asList("watts"));
		final Instant ts = clock.instant();

		// WHEN
		boolean result = publisher.publishDatum(sub, meterDatum(SOURCE_ID, ts), ts.toEpochMilli());

		// THEN
		assertThat("Not published", result, is(false));
		assertNoFrame();
		assertThat("Channel still open", channel.isActive(), is(true));
	}

	@Test
	public void publishStatus_headers() {
		// WHEN
		LiveDatumPublisher.publishStatus(session, "live-1", SOURCE_ID, SetupStatus.Gone,
				"Maximum subscription duration reached.");

		// THEN
		StompFrame f = nextFrame();
		assertStatus(f, SetupStatus.Gone);
		assertThat("Destination", f.headers().getAsString(StompHeaders.DESTINATION),
				is(SetupTopic.DatumLive.getValue()));
		assertThat("Subscription", f.headers().getAsString(StompHeaders.SUBSCRIPTION),
				is("live-1"));
		assertThat("Source ID", f.headers().getAsString(LiveHeader.SourceId.getValue()),
				is(SOURCE_ID));
		assertThat("Message", f.headers().getAsString(StompHeaders.MESSAGE),
				containsString("duration"));
		assertThat("No body", f.content().readableBytes(), is(0));
		assertThat("No content type", f.headers().getAsString(StompHeaders.CONTENT_TYPE),
				is(nullValue()));
	}

	@Test
	public void publishStatus_noSourceId() {
		// WHEN
		LiveDatumPublisher.publishStatus(session, "live-1", null, SetupStatus.Unprocessable,
				"Missing source-id header.");

		// THEN
		StompFrame f = nextFrame();
		assertStatus(f, SetupStatus.Unprocessable);
		assertThat("No source ID header", f.headers().getAsString(LiveHeader.SourceId.getValue()),
				is(nullValue()));
	}

	@Test
	public void publishStatus_channelClosed_noWrite() {
		// GIVEN
		channel.close();

		// WHEN
		LiveDatumPublisher.publishStatus(session, "live-1", SOURCE_ID, SetupStatus.Gone, null);

		// THEN
		assertNoFrame();
	}

	@Test
	public void headerValues_escapedOnceOnTheWire() {
		// GIVEN
		// channel with real STOMP encoder
		EmbeddedChannel wire = new EmbeddedChannel(new StompSubframeEncoder());
		SetupSession s = new SetupSession("setup", wire);
		LiveDatumSubscription sub = sub(s, "live:1", "meter:1", asList("watts"));
		final Instant ts = clock.instant();

		// WHEN
		publisher.publishDatum(sub, meterDatum("meter:1", ts), ts.toEpochMilli());

		// THEN
		StringBuilder buf = new StringBuilder();
		Object o;
		while ( (o = wire.readOutbound()) != null ) {
			ByteBuf b = (ByteBuf) o;
			buf.append(b.toString(StompUtils.UTF8));
			b.release();
		}
		String frame = buf.toString();
		// STOMP 1.2 escapes ':' as \c
		assertThat("Source ID escaped once", frame, containsString("\nsource-id:meter\\c1\n"));
		assertThat("Subscription escaped once", frame, containsString("\nsubscription:live\\c1\n"));
		wire.finishAndReleaseAll();
	}

}

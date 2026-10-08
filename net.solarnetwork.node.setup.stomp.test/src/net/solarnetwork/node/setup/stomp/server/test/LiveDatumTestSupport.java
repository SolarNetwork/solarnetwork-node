/* ==================================================================
 * LiveDatumTestSupport.java - 10/10/2026 1:42:00 PM
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

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.setup.stomp.SetupHeader;
import net.solarnetwork.node.setup.stomp.SetupStatus;
import net.solarnetwork.node.setup.stomp.StompUtils;
import net.solarnetwork.node.setup.stomp.server.LiveDatumModeManager;
import net.solarnetwork.node.setup.stomp.server.SetupSession;

/**
 * Shared fixtures for live datum tests.
 *
 * @author elijah
 * @version 1.0
 */
public abstract class LiveDatumTestSupport {

	/** A test source ID. */
	protected static final String SOURCE_ID = "/GEN/1";

	/** Another test source ID. */
	protected static final String OTHER_SOURCE_ID = "/INV/1";

	/** The default operational mode. */
	protected static final String MODE = LiveDatumModeManager.DEFAULT_OP_MODE;

	/** A test properties header value. */
	protected static final String PROPS = "watts,current,voltage,powerFactor";

	protected MutableClock clock;
	protected TestTaskScheduler scheduler;
	protected TestOperationalModesService opModes;
	protected ObjectMapper objectMapper;
	protected EmbeddedChannel channel;
	protected SetupSession session;

	@Before
	public void setupLiveDatumSupport() {
		clock = new MutableClock(Instant.parse("2026-10-08T00:00:00Z"));
		scheduler = new TestTaskScheduler();
		opModes = new TestOperationalModesService(clock);
		objectMapper = new ObjectMapper();
		channel = new EmbeddedChannel();
		session = new SetupSession("setup", channel);
	}

	@After
	public void teardownLiveDatumSupport() {
		channel.finishAndReleaseAll();
	}

	/**
	 * Create a datum with instantaneous properties.
	 *
	 * @param sourceId
	 *        the source ID
	 * @param ts
	 *        the timestamp
	 * @param inst
	 *        the instantaneous properties
	 * @return the datum
	 */
	protected NodeDatum datum(String sourceId, Instant ts, Map<String, Number> inst) {
		DatumSamples s = new DatumSamples();
		for ( Map.Entry<String, Number> e : inst.entrySet() ) {
			s.putInstantaneousSampleValue(e.getKey(), e.getValue());
		}
		return SimpleDatum.nodeDatum(sourceId, ts, s);
	}

	/**
	 * Create a meter datum with {@code watts}, {@code current}, {@code voltage},
	 * and {@code frequency} properties.
	 *
	 * @param sourceId
	 *        the source ID
	 * @param ts
	 *        the timestamp
	 * @return the datum
	 */
	protected NodeDatum meterDatum(String sourceId, Instant ts) {
		Map<String, Number> m = new LinkedHashMap<>();
		m.put("watts", -1520);
		m.put("current", 6.4f);
		m.put("voltage", 239.8f);
		m.put("frequency", 50.01f);
		return datum(sourceId, ts, m);
	}

	/**
	 * Read the next frame written to the test channel.
	 *
	 * @return the frame
	 */
	protected StompFrame nextFrame() {
		Object o = channel.readOutbound();
		assertThat("Frame written", o, is(instanceOf(StompFrame.class)));
		return (StompFrame) o;
	}

	/**
	 * Assert no frame has been written to the test channel.
	 */
	protected void assertNoFrame() {
		assertThat("No frame written", channel.readOutbound(), is(nullValue()));
	}

	/**
	 * Decode a frame's JSON object body.
	 *
	 * @param f
	 *        the frame
	 * @return the body
	 * @throws Exception
	 *         if the body cannot be decoded
	 */
	protected Map<String, Object> body(StompFrame f) throws Exception {
		String json = f.content().toString(StompUtils.UTF8);
		return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
		});
	}

	/**
	 * Assert a frame is a {@literal MESSAGE} with a given status.
	 *
	 * @param f
	 *        the frame
	 * @param status
	 *        the expected status
	 */
	protected void assertStatus(StompFrame f, SetupStatus status) {
		assertThat("MESSAGE frame", f.command(), is(StompCommand.MESSAGE));
		assertThat("Status header", f.headers().getAsString(SetupHeader.Status.getValue()),
				is(String.valueOf(status.getCode())));
	}

	/**
	 * Run all one-shot tasks due at the current clock time.
	 */
	protected void runScheduledTasks() {
		scheduler.runOneShots(clock.instant());
	}

}

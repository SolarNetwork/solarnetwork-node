/* ==================================================================
 * TimeoutDatumFilterServiceTests.java - 10/12/2025 8:10:16 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.std.test;

import static java.util.Map.entry;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.set;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.datum.filter.std.TimeoutDatumFilterService;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link TimeoutDatumFilterService}.
 *
 * @author matt
 * @version 1.1
 */
public class TimeoutDatumFilterServiceTests {

	private static final String SOURCE_ID = "TEST";
	private static final String PROP_1 = "watts";
	private static final String PROP_2 = "amps";
	private static final String OUTPUT_SOURCE_ID = "OUT";

	/**
	 * How far the sample clock is from the system clock, so tests can tell that
	 * a filter uses it.
	 */
	private static final Duration CLOCK_OFFSET = Duration.ofDays(-1);

	private Clock sampleClock;
	private DatumQueue datumQueue;
	private SimpleAsyncTaskScheduler scheduler;
	private TimeoutDatumFilterService xform;

	@Before
	public void setup() {
		sampleClock = Clock.offset(Clock.systemUTC(), CLOCK_OFFSET);
		datumQueue = EasyMock.createMock(DatumQueue.class);
		xform = new TimeoutDatumFilterService(sampleClock, new StaticOptionalService<>(datumQueue));
		xform.setSourceId("^%s$".formatted(SOURCE_ID));
		xform.setOutputSourceId(OUTPUT_SOURCE_ID);

		// the scheduler runs tasks on the sample clock, like a node's scheduler
		// on the system clock that the node's filters use
		scheduler = new SimpleAsyncTaskScheduler();
		scheduler.setClock(sampleClock);
		scheduler.start();
		xform.setTaskScheduler(scheduler);
	}

	@After
	public void teardown() {
		scheduler.stop();
		EasyMock.verify(datumQueue);
	}

	private void replayAll() {
		EasyMock.replay(datumQueue);
	}

	private SimpleDatum createTestSimpleDatum(String sourceId, String prop, Number val) {
		SimpleDatum datum = SimpleDatum.nodeDatum(sourceId);
		datum.getSamples().putInstantaneousSampleValue(prop, val);
		return datum;
	}

	@Test
	public void timeout() throws Exception {
		// GIVEN
		xform.setTimeout(Duration.ofSeconds(1));
		final Capture<NodeDatum> outputCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(outputCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		final Instant start = sampleClock.instant();
		xform.start();

		final SimpleDatum d1 = createTestSimpleDatum(SOURCE_ID, PROP_1, 123);
		DatumSamplesOperations result1 = null;
		try {
			result1 = xform.filter(d1, d1.getSamples(), null);
			Thread.sleep(xform.getTimeout().plusMillis(200).toMillis());
		} finally {
			xform.stop();
		}
		final Instant end = sampleClock.instant();

		// THEN
		// @formatter:off
		then(result1)
			.as("Result 1 unchanged")
			.isSameAs(d1.getSamples())
			;
		then(outputCaptor.getValue())
			.as("Output datum generated after timeout even though < coalesce threshold sources seen")
			.isInstanceOf(SimpleDatum.class)
			.asInstanceOf(type(SimpleDatum.class))
			.as("Generated datum for configured output source and clock time")
			.returns(OUTPUT_SOURCE_ID, from(SimpleDatum::getSourceId))
			.satisfies(d -> {
				then(d.getTimestamp())
					.as("Timestamp from the sample clock")
					.isBetween(start, end)
					;
			})
			.extracting(d -> d.getSamples().getInstantaneous())
			.isNull();
			;
		// @formatter:on
	}

	@Test
	public void timeout_thresholdMet() throws Exception {
		// GIVEN
		xform.setTimeout(Duration.ofSeconds(1));

		// WHEN
		replayAll();
		xform.start();

		final SimpleDatum d1 = createTestSimpleDatum(SOURCE_ID, PROP_1, 123);
		Thread.sleep(100);
		final SimpleDatum d2 = createTestSimpleDatum(SOURCE_ID, PROP_2, 234);

		DatumSamplesOperations result1 = null;
		DatumSamplesOperations result2 = null;
		try {
			result1 = xform.filter(d1, d1.getSamples(), null);
			Thread.sleep(100);
			result2 = xform.filter(d2, d2.getSamples(), null);
			Thread.sleep(xform.getTimeout().minusMillis(200).toMillis());
		} finally {
			xform.stop();
		}

		// THEN
		// @formatter:off
		then(result1)
			.as("Result 1 unchanged")
			.isSameAs(d1.getSamples())
			;
		then(result2)
			.as("Result 2 unchanged")
			.isSameAs(d2.getSamples())
			;
		// @formatter:on
	}

	@Test
	public void timeout_withTag() throws Exception {
		// GIVEN
		xform.setTimeout(Duration.ofSeconds(1));
		xform.setTagName("timeout");
		final Capture<NodeDatum> outputCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(outputCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		final Instant start = sampleClock.instant();
		xform.start();

		final SimpleDatum d1 = createTestSimpleDatum(SOURCE_ID, PROP_1, 123);
		DatumSamplesOperations result1 = null;
		try {
			result1 = xform.filter(d1, d1.getSamples(), null);
			Thread.sleep(xform.getTimeout().plusMillis(200).toMillis());
		} finally {
			xform.stop();
		}
		final Instant end = sampleClock.instant();

		// THEN
		// @formatter:off
		then(result1)
			.as("Result 1 unchanged")
			.isSameAs(d1.getSamples())
			;
		then(outputCaptor.getValue())
			.as("Output datum generated after timeout even though < coalesce threshold sources seen")
			.isInstanceOf(SimpleDatum.class)
			.asInstanceOf(type(SimpleDatum.class))
			.as("Generated datum for configured output source and clock time")
			.returns(OUTPUT_SOURCE_ID, from(SimpleDatum::getSourceId))
			.satisfies(d -> {
				then(d.getTimestamp())
					.as("Timestamp from the sample clock")
					.isBetween(start, end)
					;
			})
			.extracting(d -> d.getSamples().getTags(), set(String.class))
			.as("Tags")
			.containsExactly(xform.getTagName())
			;
		// @formatter:on
	}

	@Test
	public void timeout_withStatusProperty() throws Exception {
		// GIVEN
		xform.setTimeout(Duration.ofSeconds(1));
		xform.setStatusPropertyName("timeout");
		xform.setStatusPropertyValue("true");
		final Capture<NodeDatum> outputCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(outputCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		final Instant start = sampleClock.instant();
		xform.start();

		final SimpleDatum d1 = createTestSimpleDatum(SOURCE_ID, PROP_1, 123);
		DatumSamplesOperations result1 = null;
		try {
			result1 = xform.filter(d1, d1.getSamples(), null);
			Thread.sleep(xform.getTimeout().plusMillis(200).toMillis());
		} finally {
			xform.stop();
		}
		final Instant end = sampleClock.instant();

		// THEN
		// @formatter:off
		then(result1)
			.as("Result 1 unchanged")
			.isSameAs(d1.getSamples())
			;
		then(outputCaptor.getValue())
			.as("Output datum generated after timeout even though < coalesce threshold sources seen")
			.isInstanceOf(SimpleDatum.class)
			.asInstanceOf(type(SimpleDatum.class))
			.as("Generated datum for configured output source and clock time")
			.returns(OUTPUT_SOURCE_ID, from(SimpleDatum::getSourceId))
			.satisfies(d -> {
				then(d.getTimestamp())
					.as("Timestamp from the sample clock")
					.isBetween(start, end)
					;
			})
			.extracting(d -> d.getSamples().getStatus(), map(String.class, Object.class))
			.as("Status property")
			.containsExactly(
					entry(xform.getStatusPropertyName(), xform.getStatusPropertyValue())
					)
			;
		// @formatter:on
	}

}

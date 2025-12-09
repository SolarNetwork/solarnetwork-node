/* ==================================================================
 * JoinDatumFilterServiceTests.java - 17/02/2022 10:38:11 AM
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

import static java.lang.String.format;
import static java.util.Map.entry;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.datum.filter.std.JoinDatumFilterService;
import net.solarnetwork.node.datum.filter.std.PatternKeyValuePair;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link JoinDatumFilterService} class.
 *
 * @author matt
 * @version 1.1
 */
public class JoinDatumFilterServiceTests {

	private static final String SOURCE_ID_1 = "F_1";
	private static final String SOURCE_ID_2 = "F_2";
	private static final String PROP_1 = "watts";
	private static final String PROP_2 = "amps";
	private static final String OUTPUT_SOURCE_ID = "OUT";

	private InstantSource sampleClock;
	private DatumQueue datumQueue;
	private SimpleAsyncTaskScheduler scheduler;
	private JoinDatumFilterService xform;

	@Before
	public void setup() {
		sampleClock = EasyMock.createMock(InstantSource.class);
		datumQueue = EasyMock.createMock(DatumQueue.class);
		xform = new JoinDatumFilterService(sampleClock, new StaticOptionalService<>(datumQueue));
		xform.setSourceId("^F");
		xform.setOutputSourceId(OUTPUT_SOURCE_ID);

		scheduler = new SimpleAsyncTaskScheduler();
		scheduler.start();
		xform.setTaskScheduler(scheduler);
	}

	@After
	public void teardown() {
		scheduler.stop();
		EasyMock.verify(sampleClock, datumQueue);
	}

	private void replayAll() {
		EasyMock.replay(sampleClock, datumQueue);
	}

	private SimpleDatum createTestSimpleDatum(String sourceId, String prop, Number val) {
		SimpleDatum datum = SimpleDatum.nodeDatum(sourceId);
		datum.getSamples().putInstantaneousSampleValue(prop, val);
		return datum;
	}

	@Test
	public void simpleMerge() throws Exception {
		// GIVEN
		Capture<NodeDatum> outputCaptor = Capture.newInstance(CaptureType.ALL);
		expect(datumQueue.offer(capture(outputCaptor), eq(true))).andReturn(true).times(2);

		// WHEN
		replayAll();
		SimpleDatum d1 = createTestSimpleDatum(SOURCE_ID_1, PROP_1, 123);
		Thread.sleep(100);
		SimpleDatum d2 = createTestSimpleDatum(SOURCE_ID_2, PROP_2, 234);
		DatumSamplesOperations result1 = xform.filter(d1, d1.getSamples(), null);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);

		// THEN
		assertThat("Result unchanged", result1, is(sameInstance(d1.getSamples())));
		assertThat("Result unchanged", result2, is(sameInstance(d2.getSamples())));
		assertThat("Output datum generated", outputCaptor.getValues(), hasSize(2));
		SimpleDatum o1 = (SimpleDatum) outputCaptor.getValues().get(0);
		assertThat("Output ID 1", o1.getId(),
				is(equalTo(DatumId.nodeId(null, OUTPUT_SOURCE_ID, d1.getTimestamp()))));
		Map<String, Number> i1 = o1.getSamples().getInstantaneous();
		assertThat("Output props count 1", i1.keySet(), hasSize(1));
		assertThat("Output props 1", i1, hasEntry(PROP_1, 123));

		SimpleDatum o2 = (SimpleDatum) outputCaptor.getValues().get(1);
		assertThat("Output ID 2", o2.getId(),
				is(equalTo(DatumId.nodeId(null, OUTPUT_SOURCE_ID, d2.getTimestamp()))));
		Map<String, Number> i2 = o2.getSamples().getInstantaneous();
		assertThat("Output props count 2", i2.keySet(), hasSize(2));
		assertThat("Output props 2", i2, allOf(hasEntry(PROP_1, 123), hasEntry(PROP_2, 234)));
	}

	@Test
	public void noMergeSelf() throws Exception {
		// GIVEN
		xform.setOutputSourceId(SOURCE_ID_1);
		// WHEN
		replayAll();
		SimpleDatum d1 = createTestSimpleDatum(SOURCE_ID_1, PROP_1, 123);
		DatumSamplesOperations result1 = xform.filter(d1, d1.getSamples(), null);

		// THEN
		assertThat("Result unchanged", result1, is(sameInstance(d1.getSamples())));
	}

	@Test
	public void simpleMerge_coalesce() throws Exception {
		// GIVEN
		final int expectedOutputCount = 3;
		xform.setCoalesceThreshold(2);
		Capture<NodeDatum> outputCaptor = Capture.newInstance(CaptureType.ALL);
		expect(datumQueue.offer(capture(outputCaptor), eq(true))).andReturn(true)
				.times(expectedOutputCount);

		// WHEN
		replayAll();
		List<SimpleDatum> s1 = new ArrayList<>();
		List<DatumSamplesOperations> r1 = new ArrayList<>();
		List<SimpleDatum> s2 = new ArrayList<>();
		List<DatumSamplesOperations> r2 = new ArrayList<>();
		for ( int i = 0; i < expectedOutputCount; i++ ) {
			SimpleDatum d1 = createTestSimpleDatum(SOURCE_ID_1, PROP_1, i);
			DatumSamplesOperations result1 = xform.filter(d1, d1.getSamples(), null);
			s1.add(d1);
			r1.add(result1);
			Thread.sleep(100);
			SimpleDatum d2 = createTestSimpleDatum(SOURCE_ID_2, PROP_2, 100 + i);
			DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);
			s2.add(d2);
			r2.add(result2);
		}

		// THEN
		// so from our 6 filter inputs, 3 datum output
		assertThat("Output datum generated", outputCaptor.getValues(), hasSize(3));
		for ( int i = 0; i < expectedOutputCount; i++ ) {
			assertThat(format("Stream 1 result %d unchanged", i), r1.get(i),
					is(sameInstance(s1.get(i).getSamples())));
			assertThat(format("Stream 2 result %d unchanged", i), r2.get(i),
					is(sameInstance(s2.get(i).getSamples())));

			SimpleDatum o = (SimpleDatum) outputCaptor.getValues().get(i);
			assertThat(format("Output %d ID uses coalesced input timestamp", i), o.getId(),
					is(equalTo(DatumId.nodeId(null, OUTPUT_SOURCE_ID, s2.get(i).getTimestamp()))));
			Map<String, Number> inst = o.getSamples().getInstantaneous();
			assertThat(format("Output %d  props count", i), inst.keySet(), hasSize(2));
			assertThat(format("Output %d props", i), inst,
					allOf(hasEntry(PROP_1, i), hasEntry(PROP_2, 100 + i)));
		}
	}

	@Test
	public void simpleMerge_coalesce_repeatedSingleSource() throws Exception {
		// GIVEN
		xform.setCoalesceThreshold(2);
		Capture<NodeDatum> outputCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(outputCaptor), eq(true))).andReturn(true);

		// WHEN
		replayAll();
		SimpleDatum d1 = createTestSimpleDatum(SOURCE_ID_1, PROP_1, 123);
		DatumSamplesOperations result1 = xform.filter(d1, d1.getSamples(), null);
		Thread.sleep(100);
		SimpleDatum d2 = createTestSimpleDatum(SOURCE_ID_1, PROP_1, 321);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);
		Thread.sleep(100);
		SimpleDatum d3 = createTestSimpleDatum(SOURCE_ID_2, PROP_2, 234);
		DatumSamplesOperations result3 = xform.filter(d3, d3.getSamples(), null);

		// THEN
		assertThat("Result 1 unchanged", result1, is(sameInstance(d1.getSamples())));
		assertThat("Result 2 unchanged", result2, is(sameInstance(d2.getSamples())));
		assertThat("Result 3 unchanged", result3, is(sameInstance(d3.getSamples())));
		assertThat("Output datum generated", outputCaptor.getValue(), is(notNullValue()));
		SimpleDatum o = (SimpleDatum) outputCaptor.getValue();
		assertThat("Output ID has timestamp from most recent input datum", o.getId(),
				is(equalTo(DatumId.nodeId(null, OUTPUT_SOURCE_ID, d3.getTimestamp()))));
		Map<String, Number> i = o.getSamples().getInstantaneous();
		assertThat("Output props count", i.keySet(), hasSize(2));
		assertThat("Output prop 1 from 2nd input datum", i, hasEntry(PROP_1, 321));
		assertThat("Output prop 2 from 3rd input datum", i, hasEntry(PROP_2, 234));
	}

	@Test
	public void sourceMappedMerge_coalesce() throws Exception {
		// GIVEN
		xform.setCoalesceThreshold(2);
		xform.setPropertySourceMappings(new PatternKeyValuePair[] {
				new PatternKeyValuePair("1$", "{p}_s1"), new PatternKeyValuePair("2$", "{p}_s2"), });
		Capture<NodeDatum> outputCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(outputCaptor), eq(true))).andReturn(true);

		// WHEN
		replayAll();
		SimpleDatum d1 = createTestSimpleDatum(SOURCE_ID_1, PROP_1, 123);
		DatumSamplesOperations result1 = xform.filter(d1, d1.getSamples(), null);
		Thread.sleep(100);
		SimpleDatum d2 = createTestSimpleDatum(SOURCE_ID_2, PROP_2, 234);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);

		// THEN
		assertThat("Result 1 unchanged", result1, is(sameInstance(d1.getSamples())));
		assertThat("Result 2 unchanged", result2, is(sameInstance(d2.getSamples())));
		assertThat("Output datum generated", outputCaptor.getValue(), is(notNullValue()));
		SimpleDatum o = (SimpleDatum) outputCaptor.getValue();
		assertThat("Output ID has timestamp from most recent input datum", o.getId(),
				is(equalTo(DatumId.nodeId(null, OUTPUT_SOURCE_ID, d2.getTimestamp()))));
		Map<String, Number> i = o.getSamples().getInstantaneous();
		assertThat("Output props count", i.keySet(), hasSize(2));
		assertThat("Output prop 1 mapped from 2nd input datum", i,
				hasEntry(format("%s_s1", PROP_1), 123));
		assertThat("Output prop 2 mapped from 2rd input datum", i,
				hasEntry(format("%s_s2", PROP_2), 234));
	}

	@Test
	public void sourceMappedMerge_regexPlaceholders_coalesce() throws Exception {
		// GIVEN
		xform.setCoalesceThreshold(2);
		xform.setPropertySourceMappings(
				new PatternKeyValuePair[] { new PatternKeyValuePair("_(\\d+)$", "{p}_s{1}") });
		Capture<NodeDatum> outputCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(outputCaptor), eq(true))).andReturn(true);

		// WHEN
		replayAll();
		SimpleDatum d1 = createTestSimpleDatum(SOURCE_ID_1, PROP_1, 123);
		DatumSamplesOperations result1 = xform.filter(d1, d1.getSamples(), null);
		Thread.sleep(100);
		SimpleDatum d2 = createTestSimpleDatum(SOURCE_ID_2, PROP_2, 234);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);

		// THEN
		assertThat("Result 1 unchanged", result1, is(sameInstance(d1.getSamples())));
		assertThat("Result 2 unchanged", result2, is(sameInstance(d2.getSamples())));
		assertThat("Output datum generated", outputCaptor.getValue(), is(notNullValue()));
		SimpleDatum o = (SimpleDatum) outputCaptor.getValue();
		assertThat("Output ID has timestamp from most recent input datum", o.getId(),
				is(equalTo(DatumId.nodeId(null, OUTPUT_SOURCE_ID, d2.getTimestamp()))));
		Map<String, Number> i = o.getSamples().getInstantaneous();
		assertThat("Output props count", i.keySet(), hasSize(2));
		assertThat("Output prop 1 mapped from 2nd input datum", i,
				hasEntry(format("%s_s1", PROP_1), 123));
		assertThat("Output prop 2 mapped from 2rd input datum", i,
				hasEntry(format("%s_s2", PROP_2), 234));
	}

	@Test
	public void coalesce_withTimeout() throws Exception {
		// GIVEN
		xform.setCoalesceThreshold(2);
		xform.setCoalesceTimeout(Duration.ofSeconds(1));
		xform.setPropertySourceMappings(
				new PatternKeyValuePair[] { new PatternKeyValuePair("_(\\d+)$", "{p}_s{1}") });
		final Capture<NodeDatum> outputCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(outputCaptor), eq(true))).andReturn(true);

		final Instant timestamp = Instant.now().minusSeconds(1L);
		expect(sampleClock.instant()).andReturn(timestamp);

		// WHEN
		replayAll();
		xform.start();

		final SimpleDatum d1 = createTestSimpleDatum(SOURCE_ID_1, PROP_1, 123);
		DatumSamplesOperations result1 = null;
		try {
			result1 = xform.filter(d1, d1.getSamples(), null);
			Thread.sleep(xform.getCoalesceTimeout().plusMillis(200).toMillis());
		} finally {
			xform.stop();
		}

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
			.returns(DatumId.nodeId(null, OUTPUT_SOURCE_ID, timestamp), from(SimpleDatum::getId))
			.extracting(d -> d.getSamples().getInstantaneous(), map(String.class, Number.class))
			.as("Only properties from seen sample included")
			.containsExactly(entry(format("%s_s1", PROP_1), 123))
			;
		// @formatter:on
	}

	@Test
	public void coalesce_withTimeout_thresholdMet() throws Exception {
		// GIVEN
		xform.setCoalesceThreshold(2);
		xform.setCoalesceTimeout(Duration.ofSeconds(1));
		xform.setPropertySourceMappings(
				new PatternKeyValuePair[] { new PatternKeyValuePair("_(\\d+)$", "{p}_s{1}") });
		final Capture<NodeDatum> outputCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(outputCaptor), eq(true))).andReturn(true);

		// WHEN
		replayAll();
		xform.start();

		final SimpleDatum d1 = createTestSimpleDatum(SOURCE_ID_1, PROP_1, 123);
		Thread.sleep(100);
		final SimpleDatum d2 = createTestSimpleDatum(SOURCE_ID_2, PROP_2, 234);

		DatumSamplesOperations result1 = null;
		DatumSamplesOperations result2 = null;
		try {
			result1 = xform.filter(d1, d1.getSamples(), null);
			Thread.sleep(100);
			result2 = xform.filter(d2, d2.getSamples(), null);
			Thread.sleep(xform.getCoalesceTimeout().minusMillis(200).toMillis());
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
		then(outputCaptor.getValue())
			.as("Output datum generated immediately after coalesce threshold met")
			.isInstanceOf(SimpleDatum.class)
			.asInstanceOf(type(SimpleDatum.class))
			.as("Generated datum for configured output source and most recently collected sample")
			.returns(DatumId.nodeId(null, OUTPUT_SOURCE_ID, d2.getTimestamp()), from(SimpleDatum::getId))
			.extracting(d -> d.getSamples().getInstantaneous(), map(String.class, Number.class))
			.as("Properties from seen samples included")
			.containsExactly(
					  entry(format("%s_s1", PROP_1), 123)
					, entry(format("%s_s2", PROP_2), 234)
					)
			;
		// @formatter:on
	}

}

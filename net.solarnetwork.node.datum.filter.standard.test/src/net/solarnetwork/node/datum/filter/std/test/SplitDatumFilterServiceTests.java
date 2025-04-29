/* ==================================================================
 * SplitDatumFilterServiceTests.java - 30/03/2023 2:27:04 pm
 *
 * Copyright 2023 SolarNetwork.net Dev Team
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

import static java.util.Collections.singletonMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.util.List;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.filter.std.PatternKeyValuePair;
import net.solarnetwork.node.datum.filter.std.SplitDatumFilterService;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.support.SettingsPlaceholderService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link SplitDatumFilterService} class.
 *
 * @author matt
 * @version 1.1
 */
public class SplitDatumFilterServiceTests {

	private static final String SOURCE_ID_1 = "F_1";
	private static final String SOURCE_ID_2 = "R_2";
	private static final String OUT_SOURCE_ID_1 = "O_1";
	private static final String OUT_SOURCE_ID_2 = "O_2";
	private static final String PROP_1 = "watts";
	private static final String PROP_2 = "amps";
	private static final String PROP_3 = "wattHours";
	private static final String PROP_4 = "foo";
	private static final String PROP_5 = "abc";
	private static final String TAG_1 = "t1";
	private static final String TAG_2 = "t2";

	private DatumQueue datumQueue;
	private SplitDatumFilterService xform;

	@Before
	public void setup() {
		datumQueue = EasyMock.createMock(DatumQueue.class);
		xform = new SplitDatumFilterService(new StaticOptionalService<>(datumQueue));
		xform.setSourceId("^F");
		xform.setPlaceholderService(new StaticOptionalService<>(
				new SettingsPlaceholderService(new StaticOptionalService<>(null))));
	}

	@After
	public void teardown() {
		EasyMock.verify(datumQueue);
	}

	private void replayAll() {
		EasyMock.replay(datumQueue);
	}

	private SimpleDatum createTestDatum(String sourceId) {
		SimpleDatum datum = SimpleDatum.nodeDatum(sourceId);
		datum.putSampleValue(DatumSamplesType.Instantaneous, PROP_1, 1);
		datum.putSampleValue(DatumSamplesType.Instantaneous, PROP_2, 2);
		datum.putSampleValue(DatumSamplesType.Accumulating, PROP_3, 3);
		datum.putSampleValue(DatumSamplesType.Status, PROP_4, "4");
		datum.putSampleValue(DatumSamplesType.Status, PROP_5, "5");
		datum.addTag(TAG_1);
		datum.addTag(TAG_2);
		return datum;
	}

	@Test
	public void ignore() {
		// GIVEN
		SimpleDatum in = createTestDatum(SOURCE_ID_2);

		// @formatter:off
		xform.setPropertySourceMappings(new PatternKeyValuePair[] {
				new PatternKeyValuePair("^watt", OUT_SOURCE_ID_1),
				new PatternKeyValuePair("^a", OUT_SOURCE_ID_2),
		});
		// @formatter:on

		// WHEN
		replayAll();
		DatumSamplesOperations result = xform.filter(in, in.asSampleOperations(), null);

		assertThat("Filter ignored input because of input source ID", result,
				is(sameInstance(in.asSampleOperations())));
	}

	@Test
	public void ignore_noPropertyMatchers() {
		// GIVEN
		SimpleDatum in = createTestDatum(SOURCE_ID_1);

		// WHEN
		replayAll();
		DatumSamplesOperations result = xform.filter(in, in.asSampleOperations(), null);

		assertThat("Filter ignored input because no property matchers configured", result,
				is(sameInstance(in.asSampleOperations())));
	}

	@Test
	public void split() {
		// GIVEN
		SimpleDatum in = createTestDatum(SOURCE_ID_1);

		// @formatter:off
		xform.setPropertySourceMappings(new PatternKeyValuePair[] {
				new PatternKeyValuePair("^watt", OUT_SOURCE_ID_1),
				new PatternKeyValuePair("^a", OUT_SOURCE_ID_2),
		});
		// @formatter:on

		Capture<NodeDatum> datumCaptor = Capture.newInstance(CaptureType.ALL);
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true).times(2);

		// WHEN
		replayAll();
		DatumSamplesOperations result = xform.filter(in, in.asSampleOperations(), null);

		// THEN
		assertThat("Input swallowed", result, is(nullValue()));
		List<NodeDatum> output = datumCaptor.getValues();
		assertThat("Output datum generated", output, hasSize(2));
		Map<String, NodeDatum> outputSources = output.stream()
				.collect(toMap(NodeDatum::getSourceId, identity()));
		assertThat("Expected output sources generated", outputSources.keySet(),
				containsInAnyOrder(OUT_SOURCE_ID_1, OUT_SOURCE_ID_2));

		for ( NodeDatum out : output ) {
			assertThat(String.format("Timestamp %s copied from input", out.getObjectId()),
					out.getTimestamp(), is(equalTo(in.getTimestamp())));
		}

		DatumSamples expected1 = new DatumSamples();
		expected1.putInstantaneousSampleValue(PROP_1, 1);
		expected1.putAccumulatingSampleValue(PROP_3, 3);
		expected1.setTags(in.getTags());
		assertThat("Output 1 properties copied",
				(DatumSamples) outputSources.get(OUT_SOURCE_ID_1).asSampleOperations(),
				is(equalTo(expected1)));

		DatumSamples expected2 = new DatumSamples();
		expected2.putInstantaneousSampleValue(PROP_2, 2);
		expected2.putStatusSampleValue(PROP_5, "5");
		expected2.setTags(in.getTags());
		assertThat("Output 2 properties copied",
				(DatumSamples) outputSources.get(OUT_SOURCE_ID_2).asSampleOperations(),
				is(equalTo(expected2)));
	}

	@Test
	public void noMatchingProperties() {
		// GIVEN
		SimpleDatum in = createTestDatum(SOURCE_ID_1);

		// @formatter:off
		xform.setPropertySourceMappings(new PatternKeyValuePair[] {
				new PatternKeyValuePair("^W", OUT_SOURCE_ID_1),
				new PatternKeyValuePair("^A", OUT_SOURCE_ID_2),
		});
		// @formatter:on

		// WHEN
		replayAll();
		DatumSamplesOperations result = xform.filter(in, in.asSampleOperations(), null);

		// THEN
		assertThat("Input swallowed", result, is(nullValue()));
	}

	@Test
	public void split_sourceIdParameters() {
		// GIVEN
		SimpleDatum in = createTestDatum(SOURCE_ID_1);

		// @formatter:off
		xform.setPropertySourceMappings(new PatternKeyValuePair[] {
				new PatternKeyValuePair("^watt", "{foo}/1"),
				new PatternKeyValuePair("^a", "{foo}/2"),
		});
		// @formatter:on

		Capture<NodeDatum> datumCaptor = Capture.newInstance(CaptureType.ALL);
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true).times(2);

		// WHEN
		replayAll();
		DatumSamplesOperations result = xform.filter(in, in.asSampleOperations(),
				singletonMap("foo", "bar"));

		// THEN
		assertThat("Input swallowed", result, is(nullValue()));
		List<NodeDatum> output = datumCaptor.getValues();
		assertThat("Output datum generated", output, hasSize(2));
		Map<String, NodeDatum> outputSources = output.stream()
				.collect(toMap(NodeDatum::getSourceId, identity()));
		assertThat("Expected output sources generated", outputSources.keySet(),
				containsInAnyOrder("bar/1", "bar/2"));

		for ( NodeDatum out : output ) {
			assertThat(String.format("Timestamp %s copied from input", out.getObjectId()),
					out.getTimestamp(), is(equalTo(in.getTimestamp())));
		}

		DatumSamples expected1 = new DatumSamples();
		expected1.putInstantaneousSampleValue(PROP_1, 1);
		expected1.putAccumulatingSampleValue(PROP_3, 3);
		expected1.setTags(in.getTags());
		assertThat("Output 1 properties copied",
				(DatumSamples) outputSources.get("bar/1").asSampleOperations(), is(equalTo(expected1)));

		DatumSamples expected2 = new DatumSamples();
		expected2.putInstantaneousSampleValue(PROP_2, 2);
		expected2.putStatusSampleValue(PROP_5, "5");
		expected2.setTags(in.getTags());
		assertThat("Output 2 properties copied",
				(DatumSamples) outputSources.get("bar/2").asSampleOperations(), is(equalTo(expected2)));
	}

	@Test
	public void split_augmentedSample() {
		// GIVEN
		SimpleDatum in = createTestDatum(SOURCE_ID_1);

		// @formatter:off
		xform.setPropertySourceMappings(new PatternKeyValuePair[] {
				new PatternKeyValuePair("^watt", OUT_SOURCE_ID_1),
				new PatternKeyValuePair("^a", OUT_SOURCE_ID_2),
		});
		// @formatter:on

		Capture<NodeDatum> datumCaptor = Capture.newInstance(CaptureType.ALL);
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true).times(2);

		// WHEN
		replayAll();

		// create a new samples instance, with an additional property not present in the Datum
		// as this is how earlier filters may have modified the property state
		final String augmentPropertyName = "aNewProperty";
		DatumSamples samples = new DatumSamples(in.asSampleOperations());
		samples.putInstantaneousSampleValue(augmentPropertyName, 123);
		DatumSamplesOperations result = xform.filter(in, samples, null);

		// THEN
		assertThat("Input swallowed", result, is(nullValue()));
		List<NodeDatum> output = datumCaptor.getValues();
		assertThat("Output datum generated", output, hasSize(2));
		Map<String, NodeDatum> outputSources = output.stream()
				.collect(toMap(NodeDatum::getSourceId, identity()));
		assertThat("Expected output sources generated", outputSources.keySet(),
				containsInAnyOrder(OUT_SOURCE_ID_1, OUT_SOURCE_ID_2));

		for ( NodeDatum out : output ) {
			assertThat(String.format("Timestamp %s copied from input", out.getObjectId()),
					out.getTimestamp(), is(equalTo(in.getTimestamp())));
		}

		DatumSamples expected1 = new DatumSamples();
		expected1.putInstantaneousSampleValue(PROP_1, 1);
		expected1.putAccumulatingSampleValue(PROP_3, 3);
		expected1.setTags(in.getTags());
		assertThat("Output 1 properties copied",
				(DatumSamples) outputSources.get(OUT_SOURCE_ID_1).asSampleOperations(),
				is(equalTo(expected1)));

		DatumSamples expected2 = new DatumSamples();
		expected2.putInstantaneousSampleValue(PROP_2, 2);
		expected2.putInstantaneousSampleValue(augmentPropertyName,
				samples.getI().get(augmentPropertyName));
		expected2.putStatusSampleValue(PROP_5, "5");
		expected2.setTags(in.getTags());
		assertThat("Output 2 properties copied",
				(DatumSamples) outputSources.get(OUT_SOURCE_ID_2).asSampleOperations(),
				is(equalTo(expected2)));
	}

}

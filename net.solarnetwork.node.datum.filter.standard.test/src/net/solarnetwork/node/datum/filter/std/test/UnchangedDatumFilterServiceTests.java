/* ==================================================================
 * UnchangedDatumFilterServiceTests.java - 28/03/2023 7:22:14 am
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

import static java.util.Arrays.asList;
import static net.solarnetwork.domain.datum.DatumId.nodeId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.filter.std.UnchangedDatumFilterService;
import net.solarnetwork.node.domain.datum.SimpleDatum;

/**
 * Test cases for the {@link UnchangedDatumFilterService} class.
 * 
 * @author matt
 * @version 1.1
 */
public class UnchangedDatumFilterServiceTests {

	private static final String SOURCE_ID_1 = "S_1";
	private static final String SOURCE_ID_2 = "S_2";
	private static final String PROP_1 = "watts";
	private static final String PROP_2 = "amps";
	private static final String PROP_3 = "state";
	private static final String PROP_4 = "status";
	private static final int UNCHANGED_SECS = 10;
	private static final int DEBOUNCE_SECS = 5;

	private UnchangedDatumFilterService xform;

	@Before
	public void setup() {
		xform = new UnchangedDatumFilterService();
		xform.setUid("Test Unchanged");
		xform.setSourceId("^S");
		xform.setUnchangedPublishMaxSeconds(UNCHANGED_SECS);
	}

	private SimpleDatum createTestDatum(Instant ts, String sourceId, String prop, Number val) {
		SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, ts);
		datum.getSamples().putInstantaneousSampleValue(prop, val);
		return datum;
	}

	@Test
	public void firstDatum() {
		// GIVEN
		SimpleDatum d = createTestDatum(Instant.now(), SOURCE_ID_1, PROP_1, 1);

		// WHEN
		DatumSamplesOperations result = xform.filter(d, d.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result, is(sameInstance(d.getSamples())));
	}

	@Test
	public void unchanged() {
		// GIVEN
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);
		SimpleDatum d2 = d.copyWithId(nodeId(d.getObjectId(), d.getSourceId(), start.plusSeconds(1)));
		SimpleDatum d3 = d
				.copyWithId(nodeId(d.getObjectId(), d.getSourceId(), start.plusSeconds(UNCHANGED_SECS)));
		SimpleDatum d4 = d
				.copyWithId(nodeId(d.getObjectId(), d.getSourceId(), d3.getTimestamp().plusSeconds(1)));

		// WHEN
		DatumSamplesOperations result1 = xform.filter(d, d.getSamples(), null);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);
		DatumSamplesOperations result3 = xform.filter(d3, d3.getSamples(), null);
		DatumSamplesOperations result4 = xform.filter(d4, d4.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result1, is(sameInstance(d.getSamples())));
		assertThat("Second datum within 1st time period filtered", result2, is(nullValue()));
		assertThat("Third datum after time period not filtered", result3,
				is(sameInstance(d3.getSamples())));
		assertThat("Forth datum within 2nd time period filtered", result4, is(nullValue()));
	}

	@Test
	public void changed_propValue() {
		// GIVEN
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);
		SimpleDatum d2 = createTestDatum(start.plusSeconds(1), SOURCE_ID_1, PROP_1, 2);
		SimpleDatum d3 = d2.copyWithId(nodeId(d2.getObjectId(), d2.getSourceId(), start.plusSeconds(2)));
		SimpleDatum d4 = d3.copyWithId(nodeId(d3.getObjectId(), d3.getSourceId(),
				d2.getTimestamp().plusSeconds(UNCHANGED_SECS)));

		// WHEN
		DatumSamplesOperations result1 = xform.filter(d, d.getSamples(), null);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);
		DatumSamplesOperations result3 = xform.filter(d3, d3.getSamples(), null);
		DatumSamplesOperations result4 = xform.filter(d4, d4.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result1, is(sameInstance(d.getSamples())));
		assertThat("Second datum within 1st time period but changed property value not filtered",
				result2, is(sameInstance(d2.getSamples())));
		assertThat("Third datum within 2nd time period with same property value filtered", result3,
				is(nullValue()));
		assertThat("Forth datum after 2nd time period not filtered", result4,
				is(sameInstance(d4.getSamples())));
	}

	@Test
	public void changed_propValueAdded() {
		// GIVEN
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);
		SimpleDatum d2 = createTestDatum(start.plusSeconds(1), SOURCE_ID_1, PROP_1, 1);
		d2.putSampleValue(DatumSamplesType.Accumulating, PROP_2, 1);
		SimpleDatum d3 = d2.copyWithId(
				nodeId(d2.getObjectId(), d2.getSourceId(), d2.getTimestamp().plusSeconds(1)));

		// WHEN
		DatumSamplesOperations result1 = xform.filter(d, d.getSamples(), null);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);
		DatumSamplesOperations result3 = xform.filter(d3, d3.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result1, is(sameInstance(d.getSamples())));
		assertThat("Second datum within 1st time period but added property value not filtered", result2,
				is(sameInstance(d2.getSamples())));
		assertThat("Third datum within 2nd time period with same property value filtered", result3,
				is(nullValue()));
	}

	@Test
	public void changed_propValue_multiSourceIds() {
		// GIVEN
		Instant start_1 = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum da_1 = createTestDatum(start_1, SOURCE_ID_1, PROP_1, 1);
		SimpleDatum da_2 = createTestDatum(start_1.plusSeconds(1), SOURCE_ID_1, PROP_1, 2);
		SimpleDatum da_3 = da_2
				.copyWithId(nodeId(da_2.getObjectId(), da_2.getSourceId(), start_1.plusSeconds(2)));
		SimpleDatum da_4 = da_3.copyWithId(nodeId(da_3.getObjectId(), da_3.getSourceId(),
				da_2.getTimestamp().plusSeconds(UNCHANGED_SECS)));

		Instant start_2 = start_1.plusSeconds(2);
		SimpleDatum db_1 = createTestDatum(start_2, SOURCE_ID_2, PROP_1, 1);
		SimpleDatum db_2 = createTestDatum(start_2.plusSeconds(1), SOURCE_ID_2, PROP_1, 2);
		SimpleDatum db_3 = db_2
				.copyWithId(nodeId(db_2.getObjectId(), db_2.getSourceId(), start_2.plusSeconds(2)));
		SimpleDatum db_4 = db_3.copyWithId(nodeId(db_3.getObjectId(), db_3.getSourceId(),
				db_2.getTimestamp().plusSeconds(UNCHANGED_SECS)));

		// WHEN
		DatumSamplesOperations result_a1 = xform.filter(da_1, da_1.getSamples(), null);
		DatumSamplesOperations result_a2 = xform.filter(da_2, da_2.getSamples(), null);
		DatumSamplesOperations result_b1 = xform.filter(db_1, db_1.getSamples(), null);
		DatumSamplesOperations result_a3 = xform.filter(da_3, da_3.getSamples(), null);
		DatumSamplesOperations result_b2 = xform.filter(db_2, db_2.getSamples(), null);
		DatumSamplesOperations result_a4 = xform.filter(da_4, da_4.getSamples(), null);
		DatumSamplesOperations result_b3 = xform.filter(db_3, db_3.getSamples(), null);
		DatumSamplesOperations result_b4 = xform.filter(db_4, db_4.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result_a1, is(sameInstance(da_1.getSamples())));
		assertThat("Second datum within 1st time period but changed property value not filtered",
				result_a2, is(sameInstance(da_2.getSamples())));
		assertThat("Third datum within 2nd time period with same property value filtered", result_a3,
				is(nullValue()));
		assertThat("Forth datum after 2nd time period not filtered", result_a4,
				is(sameInstance(da_4.getSamples())));

		assertThat("First datum not filtered", result_b1, is(sameInstance(db_1.getSamples())));
		assertThat("Second datum within 1st time period but changed property value not filtered",
				result_b2, is(sameInstance(db_2.getSamples())));
		assertThat("Third datum within 2nd time period with same property value filtered", result_b3,
				is(nullValue()));
		assertThat("Forth datum after 2nd time period not filtered", result_b4,
				is(sameInstance(db_4.getSamples())));
	}

	@Test
	public void changed_propValue_noTimeLimit() {
		// GIVEN
		xform.setUnchangedPublishMaxSeconds(0);
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);
		SimpleDatum d2 = createTestDatum(start.plusSeconds(1), SOURCE_ID_1, PROP_1, 2);
		SimpleDatum d3 = d2.copyWithId(nodeId(d2.getObjectId(), d2.getSourceId(), start.plusSeconds(2)));
		SimpleDatum d4 = d3.copyWithId(nodeId(d3.getObjectId(), d3.getSourceId(),
				d2.getTimestamp().plusSeconds(UNCHANGED_SECS)));

		// WHEN
		DatumSamplesOperations result1 = xform.filter(d, d.getSamples(), null);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);
		DatumSamplesOperations result3 = xform.filter(d3, d3.getSamples(), null);
		DatumSamplesOperations result4 = xform.filter(d4, d4.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result1, is(sameInstance(d.getSamples())));
		assertThat("Second datum within 1st time period but changed property value not filtered",
				result2, is(sameInstance(d2.getSamples())));
		assertThat("Third datum within 2nd time period with same property value filtered", result3,
				is(nullValue()));
		assertThat("Forth datum after 2nd time period and same property value filtered", result4,
				is(nullValue()));
	}

	private void configurePropertyPattern() {
		xform.setPropertyIncludePatternValue("^s");
	}

	@Test
	public void firstDatum_pat() {
		// GIVEN
		configurePropertyPattern();
		SimpleDatum d = createTestDatum(Instant.now(), SOURCE_ID_1, PROP_1, 1);
		d.putSampleValue(DatumSamplesType.Status, PROP_3, "a");

		// WHEN
		DatumSamplesOperations result = xform.filter(d, d.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result, is(sameInstance(d.getSamples())));
	}

	@Test
	public void changed_pat_otherPropAdded() {
		// GIVEN
		configurePropertyPattern();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);
		d.putSampleValue(DatumSamplesType.Status, PROP_3, "a");
		SimpleDatum d2 = createTestDatum(start.plusSeconds(1), SOURCE_ID_1, PROP_1, 1);
		d2.putSampleValue(DatumSamplesType.Status, PROP_3, "a");
		d2.putSampleValue(DatumSamplesType.Accumulating, PROP_2, 1);
		SimpleDatum d3 = d2.copyWithId(
				nodeId(d2.getObjectId(), d2.getSourceId(), d2.getTimestamp().plusSeconds(1)));

		// WHEN
		DatumSamplesOperations result1 = xform.filter(d, d.getSamples(), null);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);
		DatumSamplesOperations result3 = xform.filter(d3, d3.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result1, is(sameInstance(d.getSamples())));
		assertThat("Second datum within 1st time period and added non-managed property value filtered",
				result2, is(nullValue()));
		assertThat("Third datum within 2nd time period with same property value filtered", result3,
				is(nullValue()));
	}

	@Test
	public void changed_pat_otherPropChanged() {
		// GIVEN
		configurePropertyPattern();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);
		d.putSampleValue(DatumSamplesType.Status, PROP_3, "a");
		SimpleDatum d2 = createTestDatum(start.plusSeconds(1), SOURCE_ID_1, PROP_1, 1);
		d2.putSampleValue(DatumSamplesType.Status, PROP_3, "a");
		d2.putSampleValue(DatumSamplesType.Accumulating, PROP_1, 2);
		SimpleDatum d3 = d2.copyWithId(
				nodeId(d2.getObjectId(), d2.getSourceId(), d2.getTimestamp().plusSeconds(1)));

		// WHEN
		DatumSamplesOperations result1 = xform.filter(d, d.getSamples(), null);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);
		DatumSamplesOperations result3 = xform.filter(d3, d3.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result1, is(sameInstance(d.getSamples())));
		assertThat("Second datum within 1st time period and changed non-managed property value filtered",
				result2, is(nullValue()));
		assertThat("Third datum within 2nd time period with same property value filtered", result3,
				is(nullValue()));
	}

	@Test
	public void changed_pat_propAdded() {
		// GIVEN
		configurePropertyPattern();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);
		SimpleDatum d2 = createTestDatum(start.plusSeconds(1), SOURCE_ID_1, PROP_1, 1);
		d2.putSampleValue(DatumSamplesType.Status, PROP_3, "a");
		SimpleDatum d3 = d2.copyWithId(
				nodeId(d2.getObjectId(), d2.getSourceId(), d2.getTimestamp().plusSeconds(1)));

		// WHEN
		DatumSamplesOperations result1 = xform.filter(d, d.getSamples(), null);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);
		DatumSamplesOperations result3 = xform.filter(d3, d3.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result1, is(sameInstance(d.getSamples())));
		assertThat("Second datum within 1st time period but added managed property value, not filtered",
				result2, is(sameInstance(d2.getSamples())));
		assertThat("Third datum within 2nd time period with same property value filtered", result3,
				is(nullValue()));
	}

	@Test
	public void changed_pat_propRemoved() {
		// GIVEN
		configurePropertyPattern();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);
		d.putSampleValue(DatumSamplesType.Status, PROP_3, "a");
		SimpleDatum d2 = createTestDatum(start.plusSeconds(1), SOURCE_ID_1, PROP_1, 1);
		SimpleDatum d3 = d2.copyWithId(
				nodeId(d2.getObjectId(), d2.getSourceId(), d2.getTimestamp().plusSeconds(1)));

		// WHEN
		DatumSamplesOperations result1 = xform.filter(d, d.getSamples(), null);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);
		DatumSamplesOperations result3 = xform.filter(d3, d3.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result1, is(sameInstance(d.getSamples())));
		assertThat(
				"Second datum within 1st time period but removed managed property value, not filtered",
				result2, is(sameInstance(d2.getSamples())));
		assertThat("Third datum within 2nd time period with same property value filtered", result3,
				is(nullValue()));
	}

	@Test
	public void changed_pat_propChanged() {
		// GIVEN
		configurePropertyPattern();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);
		d.putSampleValue(DatumSamplesType.Status, PROP_3, "a");
		SimpleDatum d2 = createTestDatum(start.plusSeconds(1), SOURCE_ID_1, PROP_1, 1);
		d2.putSampleValue(DatumSamplesType.Status, PROP_3, "b");
		SimpleDatum d3 = d2.copyWithId(
				nodeId(d2.getObjectId(), d2.getSourceId(), d2.getTimestamp().plusSeconds(1)));

		// WHEN
		DatumSamplesOperations result1 = xform.filter(d, d.getSamples(), null);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);
		DatumSamplesOperations result3 = xform.filter(d3, d3.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result1, is(sameInstance(d.getSamples())));
		assertThat(
				"Second datum within 1st time period but changed managed property value, not filtered",
				result2, is(sameInstance(d2.getSamples())));
		assertThat("Third datum within 2nd time period with same property value filtered", result3,
				is(nullValue()));
	}

	@Test
	public void changed_pat_propChanged_multi() {
		// GIVEN
		configurePropertyPattern();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);
		d.putSampleValue(DatumSamplesType.Status, PROP_3, "a");
		d.putSampleValue(DatumSamplesType.Status, PROP_4, "A");
		SimpleDatum d2 = createTestDatum(start.plusSeconds(1), SOURCE_ID_1, PROP_1, 1);
		d2.putSampleValue(DatumSamplesType.Status, PROP_3, "a");
		d2.putSampleValue(DatumSamplesType.Status, PROP_4, "B");
		SimpleDatum d3 = d2.copyWithId(
				nodeId(d2.getObjectId(), d2.getSourceId(), d2.getTimestamp().plusSeconds(1)));

		// WHEN
		DatumSamplesOperations result1 = xform.filter(d, d.getSamples(), null);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);
		DatumSamplesOperations result3 = xform.filter(d3, d3.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result1, is(sameInstance(d.getSamples())));
		assertThat(
				"Second datum within 1st time period but changed managed property value, not filtered",
				result2, is(sameInstance(d2.getSamples())));
		assertThat("Third datum within 2nd time period with same property value filtered", result3,
				is(nullValue()));
	}

	@Test
	public void changed_pat_propNotChanged_multi_otherPropChanged() {
		// GIVEN
		configurePropertyPattern();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);
		d.putSampleValue(DatumSamplesType.Status, PROP_3, "a");
		d.putSampleValue(DatumSamplesType.Status, PROP_4, "A");
		SimpleDatum d2 = createTestDatum(start.plusSeconds(1), SOURCE_ID_1, PROP_1, 2);
		d2.putSampleValue(DatumSamplesType.Status, PROP_3, "a");
		d2.putSampleValue(DatumSamplesType.Status, PROP_4, "A");
		SimpleDatum d3 = d2.copyWithId(
				nodeId(d2.getObjectId(), d2.getSourceId(), d2.getTimestamp().plusSeconds(1)));

		// WHEN
		DatumSamplesOperations result1 = xform.filter(d, d.getSamples(), null);
		DatumSamplesOperations result2 = xform.filter(d2, d2.getSamples(), null);
		DatumSamplesOperations result3 = xform.filter(d3, d3.getSamples(), null);

		// THEN
		assertThat("First datum not filtered", result1, is(sameInstance(d.getSamples())));
		assertThat(
				"Second datum within 1st time period but changed managed property value, not filtered",
				result2, is(nullValue()));
		assertThat("Third datum within 2nd time period with same property value filtered", result3,
				is(nullValue()));
	}

	private void configureDebounce() {
		configurePropertyPattern();
		xform.setDebounceThreshold(Duration.ofSeconds(DEBOUNCE_SECS));
	}

	private SimpleDatum createStatusDatum(Instant ts, String sourceId, Number watts, String state) {
		SimpleDatum datum = createTestDatum(ts, sourceId, PROP_1, watts);
		datum.putSampleValue(DatumSamplesType.Status, PROP_3, state);
		return datum;
	}

	private List<SimpleDatum> createStatusData(Instant start, int stepSecs, String... states) {
		final List<SimpleDatum> result = new ArrayList<>(states.length);
		for ( int i = 0; i < states.length; i++ ) {
			final int secs = i * stepSecs;
			result.add(createStatusDatum(start.plusSeconds(secs), SOURCE_ID_1, secs, states[i]));
		}
		return result;
	}

	private List<DatumSamplesOperations> filterAll(List<SimpleDatum> data) {
		final List<DatumSamplesOperations> result = new ArrayList<>(data.size());
		for ( SimpleDatum d : data ) {
			result.add(xform.filter(d, d.getSamples(), null));
		}
		return result;
	}

	/**
	 * Assert filter results.
	 *
	 * @param expected
	 *        the expected result of each datum: {@code P} for passed through,
	 *        {@code *} for published as a modified copy, or {@code .} for
	 *        discarded
	 * @param data
	 *        the input datum
	 * @param results
	 *        the filter results
	 */
	private static void assertResults(String expected, List<SimpleDatum> data,
			List<DatumSamplesOperations> results) {
		assertThat("Result count", results, hasSize(expected.length()));
		for ( int i = 0; i < expected.length(); i++ ) {
			final DatumSamplesOperations input = data.get(i).getSamples();
			final DatumSamplesOperations result = results.get(i);
			switch (expected.charAt(i)) {
				case 'P':
					assertThat(String.format("Datum %d passed through", i), result,
							is(sameInstance(input)));
					break;

				case '*':
					assertThat(String.format("Datum %d published as copy", i), result,
							is(allOf(notNullValue(), not(sameInstance(input)))));
					break;

				default:
					assertThat(String.format("Datum %d discarded", i), result, is(nullValue()));
			}
		}
	}

	@Test
	public void debounceThresholdMillis() {
		// WHEN
		xform.setDebounceThresholdMillis(1500);

		// THEN
		assertThat("Threshold set from millis", xform.getDebounceThreshold(),
				is(equalTo(Duration.ofMillis(1500))));
		assertThat("Threshold millis", xform.getDebounceThresholdMillis(), is(equalTo(1500L)));

		// WHEN
		xform.setDebounceThresholdMillis(0);

		// THEN
		assertThat("Threshold cleared from 0 millis", xform.getDebounceThreshold(), is(nullValue()));
		assertThat("Threshold millis", xform.getDebounceThresholdMillis(), is(equalTo(0L)));
	}

	@Test
	public void debounce_stableChange() {
		// GIVEN
		configureDebounce();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		List<SimpleDatum> data = createStatusData(start, 1, "A", "B", "B", "A", "B", "B", "B", "B", "B",
				"B", "B", "B");

		// WHEN
		List<DatumSamplesOperations> results = filterAll(data);

		// THEN
		// B @ 1s reverts @ 3s; B @ 4s published @ 9s once stable
		assertResults("P........P..", data, results);
	}

	@Test
	public void debounce_glitchesDiscarded() {
		// GIVEN
		configureDebounce();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		List<SimpleDatum> data = createStatusData(start, 1, "A", "B", "A", "B", "B", "A", "B", "B", "B",
				"A");

		// WHEN
		List<DatumSamplesOperations> results = filterAll(data);

		// THEN
		assertResults("P.........", data, results);
	}

	@Test
	public void debounce_pendingRestartsOnNewValue() {
		// GIVEN
		configureDebounce();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		List<SimpleDatum> data = createStatusData(start, 1, "A", "B", "C", "C", "C", "C", "C", "C");

		// WHEN
		List<DatumSamplesOperations> results = filterAll(data);

		// THEN
		// C @ 2s published @ 7s, not 6s from when B first changed
		assertResults("P......P", data, results);
	}

	@Test
	public void debounce_flapping_unchangedMaxElapsed() {
		// GIVEN
		configureDebounce();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		List<SimpleDatum> data = createStatusData(start, 1, "A", "B", "A", "B", "A", "B", "A", "B", "A",
				"B", "A", "B");

		// WHEN
		List<DatumSamplesOperations> results = filterAll(data);

		// THEN
		// A @ 10s is unchanged from stable value, so published after max seconds
		assertResults("P.........P.", data, results);
	}

	@Test
	public void debounce_unchangedMaxElapsedWhilePending() {
		// GIVEN
		configureDebounce();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		List<SimpleDatum> data = createStatusData(start, 2, "A", "B", "A", "A", "B", "B", "B", "B");

		// WHEN
		List<DatumSamplesOperations> results = filterAll(data);

		// THEN
		// B @ 8s still pending @ 10s when max seconds elapses, so published with stable A value;
		// B then published @ 14s, stable since 8s
		assertResults("P....*.P", data, results);
		DatumSamplesOperations debounced = results.get(5);
		assertThat("Debounced datum has last stable monitored property value",
				debounced.getSampleString(DatumSamplesType.Status, PROP_3), is(equalTo("A")));
		assertThat("Debounced datum has current non-monitored property value",
				debounced.getSampleInteger(DatumSamplesType.Instantaneous, PROP_1), is(equalTo(10)));
		assertThat("Input datum not modified",
				data.get(5).getSamples().getSampleString(DatumSamplesType.Status, PROP_3),
				is(equalTo("B")));
	}

	@Test
	public void debounce_unchangedMaxElapsedWhilePending_monitoredPropsAddedRemoved() {
		// GIVEN
		configurePropertyPattern();
		xform.setDebounceThreshold(Duration.ofSeconds(UNCHANGED_SECS * 2));
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d1 = createStatusDatum(start, SOURCE_ID_1, 1, "A");

		// monitored state property removed, monitored status property added
		SimpleDatum d2 = createTestDatum(start.plusSeconds(1), SOURCE_ID_1, PROP_1, 2);
		d2.putSampleValue(DatumSamplesType.Instantaneous, PROP_4, 1);
		SimpleDatum d3 = createTestDatum(start.plusSeconds(UNCHANGED_SECS), SOURCE_ID_1, PROP_1, 3);
		d3.putSampleValue(DatumSamplesType.Instantaneous, PROP_4, 1);
		d3.putSampleValue(DatumSamplesType.Accumulating, PROP_2, 3);
		List<SimpleDatum> data = asList(d1, d2, d3);

		// WHEN
		List<DatumSamplesOperations> results = filterAll(data);

		// THEN
		assertResults("P.*", data, results);
		DatumSamplesOperations debounced = results.get(2);
		assertThat("Debounced datum has removed stable monitored property restored",
				debounced.getSampleString(DatumSamplesType.Status, PROP_3), is(equalTo("A")));
		assertThat("Debounced datum has added unstable monitored property removed",
				debounced.getSampleValue(DatumSamplesType.Instantaneous, PROP_4), is(nullValue()));
		assertThat("Debounced datum has current non-monitored property value",
				debounced.getSampleInteger(DatumSamplesType.Instantaneous, PROP_1), is(equalTo(3)));
		assertThat("Debounced datum has current non-monitored property value",
				debounced.getSampleInteger(DatumSamplesType.Accumulating, PROP_2), is(equalTo(3)));
	}

	@Test
	public void debounce_noPattern_unchangedMaxElapsedWhilePending() {
		// GIVEN
		xform.setDebounceThreshold(Duration.ofSeconds(DEBOUNCE_SECS));
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// all properties monitored, and watts changes in every datum so never stable
		List<SimpleDatum> data = createStatusData(start, 2, "A", "A", "A", "A", "A", "A");

		// WHEN
		List<DatumSamplesOperations> results = filterAll(data);

		// THEN
		assertResults("P....*", data, results);
		assertThat("Debounced datum is copy of last published datum",
				results.get(5).differsFrom(data.get(0).getSamples()), is(false));
	}

	@Test
	public void debounce_multiSourceIds() {
		// GIVEN
		configureDebounce();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum da1 = createStatusDatum(start, SOURCE_ID_1, 1, "A");
		SimpleDatum db1 = createStatusDatum(start, SOURCE_ID_2, 1, "A");
		SimpleDatum da2 = createStatusDatum(start.plusSeconds(1), SOURCE_ID_1, 1, "B");
		SimpleDatum db2 = createStatusDatum(start.plusSeconds(2), SOURCE_ID_2, 1, "B");
		SimpleDatum da3 = createStatusDatum(start.plusSeconds(6), SOURCE_ID_1, 1, "B");
		SimpleDatum db3 = createStatusDatum(start.plusSeconds(6), SOURCE_ID_2, 1, "B");
		SimpleDatum db4 = createStatusDatum(start.plusSeconds(7), SOURCE_ID_2, 1, "B");
		List<SimpleDatum> data = asList(da1, db1, da2, db2, da3, db3, db4);

		// WHEN
		List<DatumSamplesOperations> results = filterAll(data);

		// THEN
		assertResults("PP..P.P", data, results);
	}

}

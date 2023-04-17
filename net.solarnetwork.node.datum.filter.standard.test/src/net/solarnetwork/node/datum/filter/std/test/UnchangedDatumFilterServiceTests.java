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

import static net.solarnetwork.domain.datum.DatumId.nodeId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * @version 1.0
 */
public class UnchangedDatumFilterServiceTests {

	private static final String SOURCE_ID_1 = "S_1";
	private static final String SOURCE_ID_2 = "S_2";
	private static final String PROP_1 = "watts";
	private static final String PROP_2 = "amps";
	private static final int UNCHANGED_SECS = 10;

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

}

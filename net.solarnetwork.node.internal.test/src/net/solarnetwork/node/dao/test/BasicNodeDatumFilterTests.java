/* ==================================================================
 * BasicNodeDatumFilterTests.java - 11 Nov 2026 11:02:42 am
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

package net.solarnetwork.node.dao.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;

/**
 * Test cases for the {@link BasicNodeDatumFilter} class.
 *
 * @author matt
 * @version 1.1
 */
public class BasicNodeDatumFilterTests {

	private static final UUID STREAM_ID_1 = new UUID(0L, 1L);
	private static final UUID STREAM_ID_2 = new UUID(0L, 2L);

	private static BasicNodeDatumFilter allCriteria() {
		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setStartDate(now);
		f.setEndDate(now.plusSeconds(1));
		f.setLocalStartDate(LocalDateTime.of(2026, 11, 1, 0, 0));
		f.setLocalEndDate(LocalDateTime.of(2026, 11, 2, 0, 0));
		f.setAggregation(Aggregation.Hour);
		f.setPartialAggregation(Aggregation.FifteenMinute);
		f.setObjectKind(ObjectDatumKind.Location);
		f.setObjectIds(new Long[] { 1L, 2L });
		f.setSourceIds(new String[] { "a", "b/*" });
		f.setStreamIds(new UUID[] { STREAM_ID_1, STREAM_ID_2 });
		f.setTypes(new String[] { "x.Y" });
		f.setSorts(new ArrayList<>(List.of(new SimpleSortDescriptor("time", true))));
		f.setOffset(1L);
		f.setMax(2);
		f.setWithoutTotalResultsCount(false);
		f.setMostRecent(true);
		return f;
	}

	@Test
	public void defaults() {
		// WHEN
		BasicNodeDatumFilter f = new BasicNodeDatumFilter();

		// THEN
		then(f.isWithoutTotalResultsCount()).as("Without total results count by default").isTrue();
		then(f.isMostRecent()).as("Not most recent by default").isFalse();
		then(f.hasObjectCriteria()).as("No object criteria").isFalse();
		then(f.hasSourceCriteria()).as("No source criteria").isFalse();
		then(f.hasTypeCriteria()).as("No type criteria").isFalse();
		then(f.hasStreamCriteria()).as("No stream criteria").isFalse();
		then(f.hasDate()).as("No date criteria").isFalse();
		then(f.hasLocalDate()).as("No local date criteria").isFalse();
		then(f.getAggregation()).as("No aggregation").isNull();
		then(f.getPartialAggregation()).as("No partial aggregation").isNull();
	}

	@Test
	public void singleValueSetters() {
		// GIVEN
		BasicNodeDatumFilter f = new BasicNodeDatumFilter();

		// WHEN
		f.setObjectId(1L);
		f.setSourceId("a");
		f.setStreamId(STREAM_ID_1);
		f.setType("x.Y");

		// THEN
		then(f.getObjectIds()).as("Object ID set as array").containsExactly(1L);
		then(f.getObjectId()).as("First object ID").isEqualTo(1L);
		then(f.getSourceIds()).as("Source ID set as array").containsExactly("a");
		then(f.getSourceId()).as("First source ID").isEqualTo("a");
		then(f.getStreamIds()).as("Stream ID set as array").containsExactly(STREAM_ID_1);
		then(f.getStreamId()).as("First stream ID").isEqualTo(STREAM_ID_1);
		then(f.getTypes()).as("Type set as array").containsExactly("x.Y");
		then(f.getType()).as("First type").isEqualTo("x.Y");

		// WHEN
		f.setObjectId(null);
		f.setSourceId(null);
		f.setStreamId(null);
		f.setType("");

		// THEN
		then(f.getObjectIds()).as("Object IDs cleared").isNull();
		then(f.getSourceIds()).as("Source IDs cleared").isNull();
		then(f.getSourceId()).as("No first source ID").isNull();
		then(f.getStreamIds()).as("Stream IDs cleared").isNull();
		then(f.getStreamId()).as("No first stream ID").isNull();
		then(f.getTypes()).as("Types cleared").isNull();
	}

	@Test
	public void copyOf() {
		// GIVEN
		BasicNodeDatumFilter f = allCriteria();

		// WHEN
		BasicNodeDatumFilter result = BasicNodeDatumFilter.copyOf(f);

		// THEN
		then(result).as("Copy is new instance").isNotSameAs(f).as("Copy is equal").isEqualTo(f)
				.as("Same hash code").hasSameHashCodeAs(f);
		then(result.getObjectIds()).as("Object IDs copied").isNotSameAs(f.getObjectIds());
		then(result.getSourceIds()).as("Source IDs copied").isNotSameAs(f.getSourceIds());
		then(result.getStreamIds()).as("Stream IDs copied").isNotSameAs(f.getStreamIds());
		then(result.getTypes()).as("Types copied").isNotSameAs(f.getTypes());
		then(result.getSorts()).as("Sorts copied").isNotSameAs(f.getSorts());

		// WHEN
		result.getSourceIds()[0] = "changed";
		result.getSorts().add(new SimpleSortDescriptor("source"));

		// THEN
		then(f.getSourceIds()).as("Original source IDs not changed").containsExactly("a", "b/*");
		then(f.getSorts()).as("Original sorts not changed").hasSize(1);
	}

	@Test
	public void copyOf_empty() {
		// GIVEN
		BasicNodeDatumFilter f = new BasicNodeDatumFilter();

		// WHEN
		BasicNodeDatumFilter result = BasicNodeDatumFilter.copyOf(f);

		// THEN
		then(result).as("Empty copy is equal").isEqualTo(f);
	}

	@Test
	public void copyOf_null() {
		thenIllegalArgumentException().isThrownBy(() -> BasicNodeDatumFilter.copyOf(null));
	}

	@Test
	public void cloned() {
		// GIVEN
		BasicNodeDatumFilter f = allCriteria();

		// WHEN
		BasicNodeDatumFilter result = f.clone();

		// THEN
		then(result).as("Clone is new instance").isNotSameAs(f).as("Clone is equal").isEqualTo(f);
	}

	@Test
	public void equals_differentCriteria() {
		// GIVEN
		BasicNodeDatumFilter f = allCriteria();
		BasicNodeDatumFilter other = allCriteria();
		other.setEndDate(f.getEndDate());
		other.setStartDate(f.getStartDate());

		// THEN
		then(other).as("Same criteria are equal").isEqualTo(f);

		// WHEN
		other.setSourceIds(new String[] { "a" });

		// THEN
		then(other).as("Different source IDs not equal").isNotEqualTo(f);

		// WHEN
		other = BasicNodeDatumFilter.copyOf(f);
		other.setObjectKind(ObjectDatumKind.Node);

		// THEN
		then(other).as("Different object kind not equal").isNotEqualTo(f);

		// WHEN
		other = BasicNodeDatumFilter.copyOf(f);
		other.setSorts(List.<SortDescriptor> of(new SimpleSortDescriptor("time")));

		// THEN
		then(other).as("Different sorts not equal").isNotEqualTo(f);

		// WHEN
		other = BasicNodeDatumFilter.copyOf(f);
		other.setStreamIds(new UUID[] { STREAM_ID_1 });

		// THEN
		then(other).as("Different stream IDs not equal").isNotEqualTo(f);

		// WHEN
		other = BasicNodeDatumFilter.copyOf(f);
		other.setLocalStartDate(f.getLocalStartDate().minusDays(1));

		// THEN
		then(other).as("Different local start date not equal").isNotEqualTo(f);

		// WHEN
		other = BasicNodeDatumFilter.copyOf(f);
		other.setLocalEndDate(f.getLocalEndDate().plusDays(1));

		// THEN
		then(other).as("Different local end date not equal").isNotEqualTo(f);

		// WHEN
		other = BasicNodeDatumFilter.copyOf(f);
		other.setAggregation(Aggregation.Day);

		// THEN
		then(other).as("Different aggregation not equal").isNotEqualTo(f);

		// WHEN
		other = BasicNodeDatumFilter.copyOf(f);
		other.setPartialAggregation(Aggregation.Hour);

		// THEN
		then(other).as("Different partial aggregation not equal").isNotEqualTo(f);
	}

	@Test
	public void string() {
		// GIVEN
		BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setObjectKind(ObjectDatumKind.Node);
		f.setObjectId(1L);
		f.setSourceId("a");
		f.setMax(2);

		// WHEN
		String result = f.toString();

		// THEN
		then(result).as("Set criteria included").isEqualTo(
				"BasicNodeDatumFilter{objectKind=Node, objectIds=[1], sourceIds=[a], max=2, withoutTotalResultsCount=true}");
	}

	@Test
	public void string_streamAndAggregation() {
		// GIVEN
		BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setStreamId(STREAM_ID_1);
		f.setLocalStartDate(LocalDateTime.of(2026, 11, 1, 0, 0));
		f.setLocalEndDate(LocalDateTime.of(2026, 11, 2, 0, 0));
		f.setAggregation(Aggregation.Hour);
		f.setPartialAggregation(Aggregation.FifteenMinute);

		// WHEN
		String result = f.toString();

		// THEN
		then(result).as("Stream, local date and aggregation criteria included").isEqualTo(
				"BasicNodeDatumFilter{streamIds=[00000000-0000-0000-0000-000000000001], localStartDate=2026-11-01T00:00, localEndDate=2026-11-02T00:00, aggregation=Hour, partialAggregation=FifteenMinute, withoutTotalResultsCount=true}");
	}

}

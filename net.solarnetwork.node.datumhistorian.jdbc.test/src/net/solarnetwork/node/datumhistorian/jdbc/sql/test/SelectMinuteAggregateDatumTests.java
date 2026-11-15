/* ==================================================================
 * SelectMinuteAggregateDatumTests.java - 15 Nov 2026 8:31:44 pm
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

package net.solarnetwork.node.datumhistorian.jdbc.sql.test;

import static net.solarnetwork.node.test.NodeTestUtils.utf8StringResource;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectMinuteAggregateDatum;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.service.FilterableDatumHistorian;

/**
 * Test cases for the {@link SelectMinuteAggregateDatum} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class SelectMinuteAggregateDatumTests {

	private static final long TEST_NODE_ID = 123L;
	private static final int TEST_FETCH_SIZE = 50;

	private static final LocalDateTime LOCAL_START = LocalDateTime.of(2026, 1, 1, 0, 0);
	private static final LocalDateTime LOCAL_END = LocalDateTime.of(2026, 1, 2, 0, 0);

	@Mock
	private Connection conn;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array array;

	@Mock
	private Array array2;

	@Mock
	private Array array3;

	@Mock
	private Array array4;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private final Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
	private final Instant end = start.plus(1, ChronoUnit.DAYS);

	private void givenPrepare() throws SQLException {
		given(conn.prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
	}

	private void givenPrepareCount() throws SQLException {
		given(conn.prepareStatement(sqlCaptor.capture())).willReturn(stmt);
	}

	private BasicNodeDatumFilter filter(Aggregation aggregation) {
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setAggregation(aggregation);
		f.setStartDate(start);
		f.setEndDate(end);
		return f;
	}

	@Test
	public void minuteAggregations() {
		for ( Aggregation agg : Aggregation.values() ) {
			// @formatter:off
			and.then(SelectMinuteAggregateDatum.isMinuteAggregation(agg))
				.as("Aggregation %s is a minute aggregation only if finer than Hour", agg)
				.isEqualTo(agg == Aggregation.Minute || agg == Aggregation.FiveMinute
						|| agg == Aggregation.TenMinute || agg == Aggregation.FifteenMinute
						|| agg == Aggregation.ThirtyMinute)
				;
			// @formatter:on
		}
		and.then(SelectMinuteAggregateDatum.isMinuteAggregation(null))
				.as("No aggregation not a minute aggregation").isFalse();
	}

	@Test
	public void slotSeconds() {
		// @formatter:off
		and.then(new SelectMinuteAggregateDatum("solarhist", filter(Aggregation.FiveMinute), 1L, null, 0))
			.returns(300, SelectMinuteAggregateDatum::getSlotSeconds)
			.returns("solarhist", SelectMinuteAggregateDatum::getSchemaName)
			;
		and.then(new SelectMinuteAggregateDatum("solarhist", filter(Aggregation.TenMinute), 1L, null, 0))
			.returns(600, SelectMinuteAggregateDatum::getSlotSeconds)
			;
		and.then(new SelectMinuteAggregateDatum("solarhist", filter(Aggregation.FifteenMinute), 1L, null, 0))
			.returns(900, SelectMinuteAggregateDatum::getSlotSeconds)
			;
		and.then(new SelectMinuteAggregateDatum("solarhist", filter(Aggregation.ThirtyMinute), 1L, null, 0))
			.returns(1800, SelectMinuteAggregateDatum::getSlotSeconds)
			;
		// @formatter:on
	}

	@Test
	public void construct_invalid() {
		final BasicNodeDatumFilter f = filter(Aggregation.FiveMinute);
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectMinuteAggregateDatum("solarhist", null, 1L, null, 0));
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectMinuteAggregateDatum("solar.hist", f, 1L, null, 0));
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectMinuteAggregateDatum(null, f, 1L, null, 0));
		thenIllegalArgumentException().as("No aggregation").isThrownBy(() -> {
			final BasicNodeDatumFilter f2 = filter(null);
			new SelectMinuteAggregateDatum("solarhist", f2, 1L, null, 0);
		});
		thenIllegalArgumentException().as("None aggregation")
				.isThrownBy(() -> new SelectMinuteAggregateDatum("solarhist", filter(Aggregation.None),
						1L, null, 0));
		thenIllegalArgumentException().as("Hour aggregation")
				.isThrownBy(() -> new SelectMinuteAggregateDatum("solarhist", filter(Aggregation.Hour),
						1L, null, 0));
	}

	@Test
	public void construct_minute() {
		// @formatter:off
		thenIllegalArgumentException()
			.as("Minute not supported, as in SolarNet")
			.isThrownBy(() -> new SelectMinuteAggregateDatum("solarhist", filter(Aggregation.Minute),
					1L, null, 0))
			.withMessage("The Minute aggregation is not supported; please query the data directly by "
					+ "omitting the aggregation criteria or using None.")
			;
		// @formatter:on
	}

	@Test
	public void construct_mostRecent() {
		final BasicNodeDatumFilter f = filter(Aggregation.TenMinute);
		f.setMostRecent(true);
		// @formatter:off
		thenIllegalArgumentException()
			.as("Most recent not supported, as in SolarNet")
			.isThrownBy(() -> new SelectMinuteAggregateDatum("solarhist", f, 1L, null, 0))
			.withMessage("The mostRecent flag cannot be used with aggregation TenMinute.")
			;
		// @formatter:on
	}

	@Test
	public void construct_dateRangeRequired() {
		final BasicNodeDatumFilter noDates = filter(Aggregation.FifteenMinute);
		noDates.setStartDate(null);
		noDates.setEndDate(null);

		final BasicNodeDatumFilter startOnly = filter(Aggregation.FifteenMinute);
		startOnly.setEndDate(null);

		final BasicNodeDatumFilter localStartOnly = filter(Aggregation.FifteenMinute);
		localStartOnly.setStartDate(null);
		localStartOnly.setEndDate(null);
		localStartOnly.setLocalStartDate(LOCAL_START);

		// @formatter:off
		for ( BasicNodeDatumFilter f : List.of(noDates, startOnly, localStartOnly) ) {
			thenIllegalArgumentException()
				.as("Date range required, as in SolarNet")
				.isThrownBy(() -> new SelectMinuteAggregateDatum("solarhist", f, 1L, null, 0))
				.withMessage("A date range must be specified for aggregation FifteenMinute.")
				;
		}
		// @formatter:on
	}

	@Test
	public void defaultNode() throws SQLException {
		// GIVEN
		givenPrepare();
		final SelectMinuteAggregateDatum sql = new SelectMinuteAggregateDatum("solarhist",
				filter(Aggregation.FiveMinute), TEST_NODE_ID, null, TEST_FETCH_SIZE);

		// WHEN
		PreparedStatement result = sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Statement created")
			.isSameAs(stmt)
			;
		and.then(sqlCaptor.getValue())
			.as("SQL generated for slot rollup function, limited to node, in default order")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-minute-aggregate-datum-01.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(sql.getSql())
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(TEST_FETCH_SIZE);
		then(stmt).should(ordered).setTimestamp(1, Timestamp.from(start));
		then(stmt).should(ordered).setTimestamp(2, Timestamp.from(end));
		then(stmt).should(ordered).setInt(3, 300);
		then(stmt).should(ordered).setObject(4, TEST_NODE_ID, Types.BIGINT);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void defaultNode_count() throws SQLException {
		// GIVEN
		givenPrepareCount();
		final SelectMinuteAggregateDatum sql = new SelectMinuteAggregateDatum("solarhist",
				filter(Aggregation.FiveMinute), TEST_NODE_ID, null, TEST_FETCH_SIZE);

		// WHEN
		PreparedStatementCreator count = sql.countPreparedStatementCreator();
		PreparedStatement result = count.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Statement created")
			.isSameAs(stmt)
			;
		and.then(sqlCaptor.getValue())
			.as("Count SQL generated with slot count function, without order or pagination")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-minute-aggregate-datum-count-01.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(((SqlProvider) count).getSql())
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setTimestamp(1, Timestamp.from(start));
		then(stmt).should(ordered).setTimestamp(2, Timestamp.from(end));
		then(stmt).should(ordered).setInt(3, 300);
		then(stmt).should(ordered).setObject(4, TEST_NODE_ID, Types.BIGINT);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void driverDefaultFetchSize() throws SQLException {
		// GIVEN
		givenPrepare();
		final SelectMinuteAggregateDatum sql = new SelectMinuteAggregateDatum("solarhist",
				filter(Aggregation.FiveMinute), TEST_NODE_ID, null, 0);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		then(stmt).should().setTimestamp(1, Timestamp.from(start));
		then(stmt).should().setTimestamp(2, Timestamp.from(end));
		then(stmt).should().setInt(3, 300);
		then(stmt).should().setObject(4, TEST_NODE_ID, Types.BIGINT);
		// Fetch size not set
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	private BasicNodeDatumFilter allCriteriaFilter() {
		final BasicNodeDatumFilter f = filter(Aggregation.FifteenMinute);
		f.setObjectKind(ObjectDatumKind.Node);
		f.setObjectIds(new Long[] { 1L, 2L });
		f.setSourceIds(new String[] { "a", "b/*" });
		f.setType(AcEnergyDatum.class.getName());
		// a single local date is ignored, as SolarNet uses local dates only as a range
		f.setLocalStartDate(LOCAL_START);
		f.setSorts(List.of(new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_TIME, true),
				new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_SOURCE)));
		f.setMax(10);
		f.setOffset(20L);
		return f;
	}

	private void givenAllCriteriaArrays(BasicNodeDatumFilter f) throws SQLException {
		given(conn.createArrayOf(eq("bigint"), aryEq(f.getObjectIds()))).willReturn(array);
		given(conn.createArrayOf(eq("text"), aryEq(f.getSourceIds()))).willReturn(array2);
		given(conn.createArrayOf(eq("text"), aryEq(f.getTypes()))).willReturn(array3);
	}

	private Integer[] givenStreamIdsArray() throws SQLException {
		final Integer[] streamIds = new Integer[] { 3, 4 };
		given(conn.createArrayOf(eq("integer"), aryEq(streamIds))).willReturn(array4);
		return streamIds;
	}

	@Test
	public void allCriteria() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = allCriteriaFilter();
		givenAllCriteriaArrays(f);
		final Integer[] streamIds = givenStreamIdsArray();
		final SelectMinuteAggregateDatum sql = new SelectMinuteAggregateDatum("solaruhist", f,
				TEST_NODE_ID, streamIds, TEST_FETCH_SIZE);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated with instant date range, criteria on streams, sorts and pagination")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-minute-aggregate-datum-02.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(TEST_FETCH_SIZE);
		then(stmt).should(ordered).setTimestamp(1, Timestamp.from(start));
		then(stmt).should(ordered).setTimestamp(2, Timestamp.from(end));
		then(stmt).should(ordered).setInt(3, 900);
		then(stmt).should(ordered).setArray(4, array);
		then(stmt).should(ordered).setArray(5, array4);
		then(stmt).should(ordered).setArray(6, array2);
		then(stmt).should(ordered).setArray(7, array3);
		then(stmt).should(ordered).setInt(8, 10);
		then(stmt).should(ordered).setLong(9, 20L);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void allCriteria_count() throws SQLException {
		// GIVEN
		givenPrepareCount();
		final BasicNodeDatumFilter f = allCriteriaFilter();
		givenAllCriteriaArrays(f);
		final Integer[] streamIds = givenStreamIdsArray();
		final SelectMinuteAggregateDatum sql = new SelectMinuteAggregateDatum("solaruhist", f,
				TEST_NODE_ID, streamIds, TEST_FETCH_SIZE);

		// WHEN
		PreparedStatementCreator count = sql.countPreparedStatementCreator();
		count.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("Count SQL generated, without order or pagination")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-minute-aggregate-datum-count-02.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setTimestamp(1, Timestamp.from(start));
		then(stmt).should(ordered).setTimestamp(2, Timestamp.from(end));
		then(stmt).should(ordered).setInt(3, 900);
		then(stmt).should(ordered).setArray(4, array);
		then(stmt).should(ordered).setArray(5, array4);
		then(stmt).should(ordered).setArray(6, array2);
		then(stmt).should(ordered).setArray(7, array3);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	private BasicNodeDatumFilter localDatesFilter() {
		final BasicNodeDatumFilter f = filter(Aggregation.ThirtyMinute);
		f.setObjectKind(ObjectDatumKind.Location);
		f.setObjectIds(new Long[] { 3L, 4L });
		// the local date range is used in place of the instants
		f.setLocalStartDate(LOCAL_START);
		f.setLocalEndDate(LOCAL_END);
		return f;
	}

	@Test
	public void localDates() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = localDatesFilter();
		given(conn.createArrayOf(eq("bigint"), aryEq(f.getObjectIds()))).willReturn(array);
		final SelectMinuteAggregateDatum sql = new SelectMinuteAggregateDatum("solarhist", f,
				TEST_NODE_ID, null, TEST_FETCH_SIZE);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated with local date range in node time zone of each stream")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-minute-aggregate-datum-03.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(TEST_FETCH_SIZE);
		then(stmt).should(ordered).setObject(1, LOCAL_START, Types.TIMESTAMP);
		then(stmt).should(ordered).setObject(2, LOCAL_END, Types.TIMESTAMP);
		then(stmt).should(ordered).setInt(3, 1800);
		then(stmt).should(ordered).setArray(4, array);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void localDates_count() throws SQLException {
		// GIVEN
		givenPrepareCount();
		final BasicNodeDatumFilter f = localDatesFilter();
		given(conn.createArrayOf(eq("bigint"), aryEq(f.getObjectIds()))).willReturn(array);
		final SelectMinuteAggregateDatum sql = new SelectMinuteAggregateDatum("solarhist", f,
				TEST_NODE_ID, null, TEST_FETCH_SIZE);

		// WHEN
		sql.countPreparedStatementCreator().createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("Count SQL generated with local date range")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-minute-aggregate-datum-count-03.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setObject(1, LOCAL_START, Types.TIMESTAMP);
		then(stmt).should(ordered).setObject(2, LOCAL_END, Types.TIMESTAMP);
		then(stmt).should(ordered).setInt(3, 1800);
		then(stmt).should(ordered).setArray(4, array);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

}

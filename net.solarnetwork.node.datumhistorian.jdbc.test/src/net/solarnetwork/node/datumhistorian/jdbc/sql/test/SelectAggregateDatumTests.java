/* ==================================================================
 * SelectAggregateDatumTests.java - 15 Nov 2026 5:02:11 pm
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
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectAggregateDatum;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.service.FilterableDatumHistorian;

/**
 * Test cases for the {@link SelectAggregateDatum} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class SelectAggregateDatumTests {

	private static final long TEST_NODE_ID = 123L;
	private static final int TEST_FETCH_SIZE = 50;

	private static final LocalDateTime LOCAL_START = LocalDateTime.of(2026, 1, 1, 0, 0);
	private static final LocalDateTime LOCAL_END = LocalDateTime.of(2026, 2, 1, 0, 0);

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

	private static BasicNodeDatumFilter filter(Aggregation aggregation) {
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setAggregation(aggregation);
		return f;
	}

	@Test
	public void supportedAggregations() {
		for ( Aggregation agg : Aggregation.values() ) {
			// @formatter:off
			and.then(SelectAggregateDatum.isSupportedAggregation(agg))
				.as("Aggregation %s supported only if stored", agg)
				.isEqualTo(agg == Aggregation.Hour || agg == Aggregation.Day || agg == Aggregation.Month)
				;
			// @formatter:on
		}
		and.then(SelectAggregateDatum.isSupportedAggregation(null)).as("No aggregation not supported")
				.isFalse();
	}

	@Test
	public void construct_invalid() {
		final BasicNodeDatumFilter f = filter(Aggregation.Hour);
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectAggregateDatum("solarhist", null, 1L, null, 0));
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectAggregateDatum("solar.hist", f, 1L, null, 0));
		thenIllegalArgumentException().isThrownBy(() -> new SelectAggregateDatum(null, f, 1L, null, 0));
		thenIllegalArgumentException().as("No aggregation").isThrownBy(
				() -> new SelectAggregateDatum("solarhist", new BasicNodeDatumFilter(), 1L, null, 0));
		thenIllegalArgumentException().as("Aggregation not stored").isThrownBy(
				() -> new SelectAggregateDatum("solarhist", filter(Aggregation.Week), 1L, null, 0));
		thenIllegalArgumentException().as("None aggregation").isThrownBy(
				() -> new SelectAggregateDatum("solarhist", filter(Aggregation.None), 1L, null, 0));
	}

	@Test
	public void tableNames() {
		// @formatter:off
		and.then(new SelectAggregateDatum("solarhist", filter(Aggregation.Hour), 1L, null, 0))
			.returns("dh_agg_hourly", SelectAggregateDatum::getTableName)
			.returns("solarhist", SelectAggregateDatum::getSchemaName)
			;
		and.then(new SelectAggregateDatum("solarhist", filter(Aggregation.Day), 1L, null, 0))
			.returns("dh_agg_daily", SelectAggregateDatum::getTableName)
			;
		and.then(new SelectAggregateDatum("solarhist", filter(Aggregation.Month), 1L, null, 0))
			.returns("dh_agg_monthly", SelectAggregateDatum::getTableName)
			;
		// @formatter:on
	}

	@Test
	public void defaultNode() throws SQLException {
		// GIVEN
		givenPrepare();
		final SelectAggregateDatum sql = new SelectAggregateDatum("solarhist", filter(Aggregation.Hour),
				TEST_NODE_ID, null, TEST_FETCH_SIZE);

		// WHEN
		PreparedStatement result = sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Statement created")
			.isSameAs(stmt)
			;
		and.then(sqlCaptor.getValue())
			.as("SQL generated for hourly table, limited to node, in default order")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-aggregate-datum-01.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(sql.getSql())
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(TEST_FETCH_SIZE);
		then(stmt).should(ordered).setObject(1, TEST_NODE_ID, Types.BIGINT);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void driverDefaultFetchSize() throws SQLException {
		// GIVEN
		givenPrepare();
		final SelectAggregateDatum sql = new SelectAggregateDatum("solarhist", filter(Aggregation.Hour),
				TEST_NODE_ID, null, 0);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		then(stmt).should().setObject(1, TEST_NODE_ID, Types.BIGINT);
		// Fetch size not set
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	private BasicNodeDatumFilter allCriteriaFilter() {
		final BasicNodeDatumFilter f = filter(Aggregation.Day);
		f.setObjectKind(ObjectDatumKind.Node);
		f.setObjectIds(new Long[] { 1L, 2L });
		f.setSourceIds(new String[] { "a", "b/*" });
		f.setType(AcEnergyDatum.class.getName());
		f.setStartDate(start);
		f.setEndDate(end);
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
		final SelectAggregateDatum sql = new SelectAggregateDatum("solarhist", f, TEST_NODE_ID,
				streamIds, TEST_FETCH_SIZE);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated for daily table, with dates on period start, and type criteria only on streams")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-aggregate-datum-02.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(TEST_FETCH_SIZE);
		then(stmt).should(ordered).setArray(1, array);
		then(stmt).should(ordered).setArray(2, array4);
		then(stmt).should(ordered).setArray(3, array2);
		then(stmt).should(ordered).setArray(4, array3);
		then(stmt).should(ordered).setTimestamp(5, Timestamp.from(start));
		then(stmt).should(ordered).setTimestamp(6, Timestamp.from(end));
		then(stmt).should(ordered).setInt(7, 10);
		then(stmt).should(ordered).setLong(8, 20L);
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
		final SelectAggregateDatum sql = new SelectAggregateDatum("solarhist", f, TEST_NODE_ID,
				streamIds, TEST_FETCH_SIZE);

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
			.as("Count SQL generated, without order or pagination")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-aggregate-datum-count-02.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(((SqlProvider) count).getSql())
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setArray(1, array);
		then(stmt).should(ordered).setArray(2, array4);
		then(stmt).should(ordered).setArray(3, array2);
		then(stmt).should(ordered).setArray(4, array3);
		then(stmt).should(ordered).setTimestamp(5, Timestamp.from(start));
		then(stmt).should(ordered).setTimestamp(6, Timestamp.from(end));
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	private BasicNodeDatumFilter locationIdsMostRecentFilter() {
		final BasicNodeDatumFilter f = filter(Aggregation.Month);
		f.setObjectKind(ObjectDatumKind.Location);
		f.setObjectIds(new Long[] { 3L, 4L });
		f.setSourceIds(new String[] { "a", "b/*" });
		f.setType(AcEnergyDatum.class.getName());
		f.setStartDate(start);
		f.setEndDate(end);
		f.setMostRecent(true);
		f.setSorts(List.of(new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_NODE, true),
				new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_TIME, true)));
		f.setMax(10);
		f.setOffset(20L);
		return f;
	}

	@Test
	public void locationIds_mostRecent() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = locationIdsMostRecentFilter();
		givenAllCriteriaArrays(f);
		final SelectAggregateDatum sql = new SelectAggregateDatum("solaruhist", f, TEST_NODE_ID, null,
				TEST_FETCH_SIZE);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated for monthly table, with most recent row of each location stream, not merged, with node sort")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-aggregate-datum-03.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(TEST_FETCH_SIZE);
		then(stmt).should(ordered).setTimestamp(1, Timestamp.from(start));
		then(stmt).should(ordered).setTimestamp(2, Timestamp.from(end));
		then(stmt).should(ordered).setArray(3, array);
		then(stmt).should(ordered).setArray(4, array2);
		then(stmt).should(ordered).setArray(5, array3);
		then(stmt).should(ordered).setInt(6, 10);
		then(stmt).should(ordered).setLong(7, 20L);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void locationIds_mostRecent_count() throws SQLException {
		// GIVEN
		givenPrepareCount();
		final BasicNodeDatumFilter f = locationIdsMostRecentFilter();
		givenAllCriteriaArrays(f);
		final SelectAggregateDatum sql = new SelectAggregateDatum("solaruhist", f, TEST_NODE_ID, null,
				TEST_FETCH_SIZE);

		// WHEN
		PreparedStatementCreator count = sql.countPreparedStatementCreator();
		count.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("Count SQL generated, without order or pagination")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-aggregate-datum-count-03.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(((SqlProvider) count).getSql())
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setTimestamp(1, Timestamp.from(start));
		then(stmt).should(ordered).setTimestamp(2, Timestamp.from(end));
		then(stmt).should(ordered).setArray(3, array);
		then(stmt).should(ordered).setArray(4, array2);
		then(stmt).should(ordered).setArray(5, array3);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void locationId() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = filter(Aggregation.Hour);
		f.setObjectKind(ObjectDatumKind.Location);
		f.setObjectId(3L);
		f.setStartDate(start);
		final SelectAggregateDatum sql = new SelectAggregateDatum("solarhist", f, TEST_NODE_ID, null,
				TEST_FETCH_SIZE);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, with the location streams of all nodes, not merged")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-aggregate-datum-04.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(TEST_FETCH_SIZE);
		then(stmt).should(ordered).setObject(1, 3L);
		then(stmt).should(ordered).setTimestamp(2, Timestamp.from(start));
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	private BasicNodeDatumFilter localDatesFilter() {
		final BasicNodeDatumFilter f = filter(Aggregation.Day);
		f.setStartDate(start);
		f.setEndDate(end);
		f.setLocalStartDate(LOCAL_START);
		f.setLocalEndDate(LOCAL_END);
		return f;
	}

	@Test
	public void localDates() throws SQLException {
		// GIVEN
		givenPrepare();
		final SelectAggregateDatum sql = new SelectAggregateDatum("solarhist", localDatesFilter(),
				TEST_NODE_ID, null, TEST_FETCH_SIZE);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, with local dates in the zone of each node used in place of dates")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-aggregate-datum-05.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(sql.getSql())
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(TEST_FETCH_SIZE);
		then(stmt).should(ordered).setObject(1, TEST_NODE_ID, Types.BIGINT);
		then(stmt).should(ordered).setObject(2, LOCAL_START, Types.TIMESTAMP);
		then(stmt).should(ordered).setObject(3, LOCAL_END, Types.TIMESTAMP);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void localDates_count() throws SQLException {
		// GIVEN
		givenPrepareCount();
		final SelectAggregateDatum sql = new SelectAggregateDatum("solarhist", localDatesFilter(),
				TEST_NODE_ID, null, TEST_FETCH_SIZE);

		// WHEN
		PreparedStatementCreator count = sql.countPreparedStatementCreator();
		count.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("Count SQL generated, with local dates in the zone of each node")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-aggregate-datum-count-05.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(((SqlProvider) count).getSql())
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setObject(1, TEST_NODE_ID, Types.BIGINT);
		then(stmt).should(ordered).setObject(2, LOCAL_START, Types.TIMESTAMP);
		then(stmt).should(ordered).setObject(3, LOCAL_END, Types.TIMESTAMP);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void localStartDate_mostRecent() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = filter(Aggregation.Month);
		f.setMostRecent(true);
		f.setEndDate(end);
		f.setLocalStartDate(LOCAL_START);
		final SelectAggregateDatum sql = new SelectAggregateDatum("solarhist", f, TEST_NODE_ID, null,
				TEST_FETCH_SIZE);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, with local start date used in place of dates for most recent aggregate")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-aggregate-datum-06.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(TEST_FETCH_SIZE);
		then(stmt).should(ordered).setObject(1, LOCAL_START, Types.TIMESTAMP);
		then(stmt).should(ordered).setObject(2, TEST_NODE_ID, Types.BIGINT);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

}

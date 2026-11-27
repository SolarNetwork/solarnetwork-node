/* ==================================================================
 * SelectThirtyMinuteAggregateDatumTests.java - 25 Nov 2026 4:41:12 pm
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
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectThirtyMinuteAggregateDatum;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.service.FilterableDatumHistorian;

/**
 * Test cases for the {@link SelectThirtyMinuteAggregateDatum} class.
 *
 * <p>
 * The expected SQL finds and pages the half hours, and then combines the two
 * 15 minute rows of each, with the statements of the prototype whose results
 * were compared in a database with those of the earlier statement, which was
 * compared with the day rollup rules, so it does not repeat the class.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class SelectThirtyMinuteAggregateDatumTests {

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
	public void construct_invalid() {
		final BasicNodeDatumFilter f = filter(Aggregation.ThirtyMinute);
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectThirtyMinuteAggregateDatum("solarhist", null, 1L, null, 0));
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectThirtyMinuteAggregateDatum("solar.hist", f, 1L, null, 0));
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectThirtyMinuteAggregateDatum(null, f, 1L, null, 0));
		// @formatter:off
		thenIllegalArgumentException()
			.as("No aggregation")
			.isThrownBy(() -> new SelectThirtyMinuteAggregateDatum("solarhist", new BasicNodeDatumFilter(),
					1L, null, 0))
			.withMessage("The null aggregation is not supported.")
			;
		for ( Aggregation agg : Aggregation.values() ) {
			if ( agg == Aggregation.ThirtyMinute ) {
				continue;
			}
			thenIllegalArgumentException()
				.as("Aggregation %s not supported", agg)
				.isThrownBy(() -> new SelectThirtyMinuteAggregateDatum("solarhist", filter(agg),
						1L, null, 0))
				.withMessage("The %s aggregation is not supported.", agg)
				;
		}
		// @formatter:on
	}

	@Test
	public void construct_mostRecent() {
		final BasicNodeDatumFilter f = filter(Aggregation.ThirtyMinute);
		f.setMostRecent(true);
		// @formatter:off
		thenIllegalArgumentException()
			.as("Most recent not supported, as in SolarNet")
			.isThrownBy(() -> new SelectThirtyMinuteAggregateDatum("solarhist", f, 1L, null, 0))
			.withMessage("The mostRecent flag cannot be used with aggregation ThirtyMinute.")
			;
		// @formatter:on
	}

	@Test
	public void schemaName() {
		// @formatter:off
		and.then(new SelectThirtyMinuteAggregateDatum("solaruhist", filter(Aggregation.ThirtyMinute),
					1L, null, 0))
			.returns("solaruhist", SelectThirtyMinuteAggregateDatum::getSchemaName)
			;
		// @formatter:on
	}

	@Test
	public void defaultNode() throws SQLException {
		// GIVEN
		givenPrepare();
		final SelectThirtyMinuteAggregateDatum sql = new SelectThirtyMinuteAggregateDatum("solarhist",
				filter(Aggregation.ThirtyMinute), TEST_NODE_ID, null, TEST_FETCH_SIZE);

		// WHEN
		PreparedStatement result = sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Statement created")
			.isSameAs(stmt)
			;
		and.then(sqlCaptor.getValue())
			.as("SQL generated combining the 15 minute rows of each half hour, limited to node, in default order")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-thirty-minute-aggregate-datum-01.sql", getClass()))
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
		final SelectThirtyMinuteAggregateDatum sql = new SelectThirtyMinuteAggregateDatum("solarhist",
				filter(Aggregation.ThirtyMinute), TEST_NODE_ID, null, 0);

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
		final BasicNodeDatumFilter f = filter(Aggregation.ThirtyMinute);
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
		final SelectThirtyMinuteAggregateDatum sql = new SelectThirtyMinuteAggregateDatum("solarhist", f,
				TEST_NODE_ID, streamIds, TEST_FETCH_SIZE);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, with dates taken to the next half hour and compared with the start of the 15 minute rows, "
					+ "type criteria only on streams, and the half hours sorted and paged before they are combined")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-thirty-minute-aggregate-datum-02.sql", getClass()))
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
		final SelectThirtyMinuteAggregateDatum sql = new SelectThirtyMinuteAggregateDatum("solarhist", f,
				TEST_NODE_ID, streamIds, TEST_FETCH_SIZE);

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
			.as("Count SQL generated, counting the half hours of each stream, without order or pagination")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-thirty-minute-aggregate-datum-count-02.sql", getClass()))
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

	@Test
	public void locationId_startDate() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = filter(Aggregation.ThirtyMinute);
		f.setObjectKind(ObjectDatumKind.Location);
		f.setObjectId(3L);
		f.setStartDate(start);
		f.setSorts(List.of(new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_NODE, true),
				new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_TIME)));
		final SelectThirtyMinuteAggregateDatum sql = new SelectThirtyMinuteAggregateDatum("solaruhist",
				f, TEST_NODE_ID, null, TEST_FETCH_SIZE);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated in schema, with the location streams of all nodes, not merged, only a start date, "
					+ "and node sort")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-thirty-minute-aggregate-datum-03.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(TEST_FETCH_SIZE);
		then(stmt).should(ordered).setObject(1, 3L);
		then(stmt).should(ordered).setTimestamp(2, Timestamp.from(start));
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	private BasicNodeDatumFilter localDatesFilter() {
		final BasicNodeDatumFilter f = filter(Aggregation.ThirtyMinute);
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
		final SelectThirtyMinuteAggregateDatum sql = new SelectThirtyMinuteAggregateDatum("solarhist",
				localDatesFilter(), TEST_NODE_ID, null, TEST_FETCH_SIZE);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, with local dates in the zone of each node taken to the next half hour, "
					+ "used in place of dates")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-thirty-minute-aggregate-datum-04.sql", getClass()))
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
		final SelectThirtyMinuteAggregateDatum sql = new SelectThirtyMinuteAggregateDatum("solarhist",
				localDatesFilter(), TEST_NODE_ID, null, TEST_FETCH_SIZE);

		// WHEN
		PreparedStatementCreator count = sql.countPreparedStatementCreator();
		count.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("Count SQL generated, with local dates in the zone of each node")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-thirty-minute-aggregate-datum-count-04.sql", getClass()))
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

}

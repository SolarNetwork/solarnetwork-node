/* ==================================================================
 * SelectReadingDifferenceTests.java - 15 Nov 2026 5:31:44 pm
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
import net.solarnetwork.domain.datum.DatumReadingType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectReadingDifference;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.service.FilterableDatumHistorian;

/**
 * Test cases for the {@link SelectReadingDifference} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class SelectReadingDifferenceTests {

	private static final long TEST_NODE_ID = 123L;
	private static final int TEST_FETCH_SIZE = 50;

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

	private final Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.DAYS);
	private final Instant end = start.plus(1, ChronoUnit.DAYS);

	private void givenPrepare() throws SQLException {
		given(conn.prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
	}

	private void givenPrepareCount() throws SQLException {
		given(conn.prepareStatement(sqlCaptor.capture())).willReturn(stmt);
	}

	private static BasicNodeDatumFilter filter() {
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setReadingType(DatumReadingType.Difference);
		return f;
	}

	@Test
	public void construct_invalid() {
		final BasicNodeDatumFilter f = filter();
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectReadingDifference("solarhist", null, 1L, null, 0));
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectReadingDifference("solar.hist", f, 1L, null, 0));
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectReadingDifference(null, f, 1L, null, 0));
	}

	@Test
	public void defaultNode() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = filter();
		f.setStartDate(start);
		f.setEndDate(end);
		final SelectReadingDifference sql = new SelectReadingDifference("solarhist", f, TEST_NODE_ID,
				null, TEST_FETCH_SIZE);

		// WHEN
		PreparedStatement result = sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Statement created")
			.isSameAs(stmt)
			;
		and.then(sqlCaptor.getValue())
			.as("SQL generated, with reading difference of each stream, limited to node, in default order")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-reading-difference-01.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(sql.getSql())
			;
		and.then(sql)
			.as("Start date from filter")
			.returns(start, SelectReadingDifference::getStartDate)
			.as("End date from filter")
			.returns(end, SelectReadingDifference::getEndDate)
			.as("Schema name")
			.returns("solarhist", SelectReadingDifference::getSchemaName)
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(TEST_FETCH_SIZE);
		then(stmt).should(ordered).setTimestamp(1, Timestamp.from(start));
		then(stmt).should(ordered).setTimestamp(2, Timestamp.from(end));
		then(stmt).should(ordered).setObject(3, TEST_NODE_ID, Types.BIGINT);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void noDates_now() throws SQLException {
		// GIVEN
		givenPrepare();
		final Instant before = Instant.now();
		final SelectReadingDifference sql = new SelectReadingDifference("solarhist", filter(),
				TEST_NODE_ID, null, 0);
		final Instant after = Instant.now();

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sql.getStartDate())
			.as("Start date is the time the statement was created")
			.isBetween(before, after)
			.as("End date is the same time")
			.isEqualTo(sql.getEndDate())
			;
		and.then(sqlCaptor.getValue())
			.as("SQL generated as with dates")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-reading-difference-01.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		// fetch size not set for driver default
		then(stmt).should(ordered).setTimestamp(1, Timestamp.from(sql.getStartDate()));
		then(stmt).should(ordered).setTimestamp(2, Timestamp.from(sql.getStartDate()));
		then(stmt).should(ordered).setObject(3, TEST_NODE_ID, Types.BIGINT);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void startDateOnly_endNow() {
		// GIVEN
		final BasicNodeDatumFilter f = filter();
		f.setStartDate(start);

		// WHEN
		final Instant before = Instant.now();
		final SelectReadingDifference sql = new SelectReadingDifference("solarhist", f, TEST_NODE_ID,
				null, 0);
		final Instant after = Instant.now();

		// THEN
		// @formatter:off
		and.then(sql)
			.as("Start date from filter")
			.returns(start, SelectReadingDifference::getStartDate)
			;
		and.then(sql.getEndDate())
			.as("End date is the time the statement was created")
			.isBetween(before, after)
			;
		// @formatter:on
	}

	private BasicNodeDatumFilter allCriteriaFilter() {
		final BasicNodeDatumFilter f = filter();
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
		final SelectReadingDifference sql = new SelectReadingDifference("solarhist", f, TEST_NODE_ID,
				streamIds, TEST_FETCH_SIZE);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, with dates as reading times, and type criteria only on streams")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-reading-difference-02.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(TEST_FETCH_SIZE);
		then(stmt).should(ordered).setTimestamp(1, Timestamp.from(start));
		then(stmt).should(ordered).setTimestamp(2, Timestamp.from(end));
		then(stmt).should(ordered).setArray(3, array);
		then(stmt).should(ordered).setArray(4, array4);
		then(stmt).should(ordered).setArray(5, array2);
		then(stmt).should(ordered).setArray(6, array3);
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
		final SelectReadingDifference sql = new SelectReadingDifference("solarhist", f, TEST_NODE_ID,
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
			.as("Count SQL generated, counting streams with readings, without order or pagination")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-reading-difference-count-02.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(((SqlProvider) count).getSql())
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setTimestamp(1, Timestamp.from(start));
		then(stmt).should(ordered).setTimestamp(2, Timestamp.from(end));
		then(stmt).should(ordered).setArray(3, array);
		then(stmt).should(ordered).setArray(4, array4);
		then(stmt).should(ordered).setArray(5, array2);
		then(stmt).should(ordered).setArray(6, array3);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void locationIds_mostRecentIgnored() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = filter();
		f.setAggregation(Aggregation.Day);
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
		givenAllCriteriaArrays(f);
		final SelectReadingDifference sql = new SelectReadingDifference("solaruhist", f, TEST_NODE_ID,
				null, TEST_FETCH_SIZE);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated for the location streams of all nodes, not merged, ignoring most recent and aggregation")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-reading-difference-03.sql", getClass()))
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

}

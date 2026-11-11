/* ==================================================================
 * SelectFilteredDatumTests.java - 11 Nov 2026 11:02:42 am
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

import static java.time.ZoneOffset.UTC;
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
import java.sql.Types;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
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
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectFilteredDatum;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.service.FilterableDatumHistorian;

/**
 * Test cases for the {@link SelectFilteredDatum} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class SelectFilteredDatumTests {

	private static final long TEST_NODE_ID = 123L;

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

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private final Instant start = Instant.now().truncatedTo(ChronoUnit.MILLIS);
	private final Instant end = start.plusSeconds(60);

	private void givenPrepare() throws SQLException {
		given(conn.prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT)))
				.willReturn(stmt);
	}

	private void givenPrepareCount() throws SQLException {
		given(conn.prepareStatement(sqlCaptor.capture())).willReturn(stmt);
	}

	@Test
	public void construct_invalid() {
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		thenIllegalArgumentException().isThrownBy(() -> new SelectFilteredDatum("solarhist", null, 1L));
		thenIllegalArgumentException().isThrownBy(() -> new SelectFilteredDatum("solar.hist", f, 1L));
		thenIllegalArgumentException().isThrownBy(() -> new SelectFilteredDatum(null, f, 1L));
	}

	@Test
	public void defaultNode() throws SQLException {
		// GIVEN
		givenPrepare();
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", new BasicNodeDatumFilter(),
				TEST_NODE_ID);

		// WHEN
		PreparedStatement result = sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Statement created")
			.isSameAs(stmt)
			;
		and.then(sqlCaptor.getValue())
			.as("SQL generated, limited to node, in default order")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-filtered-datum-01.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(sql.getSql())
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(SelectFilteredDatum.DEFAULT_FETCH_SIZE);
		then(stmt).should(ordered).setLong(1, TEST_NODE_ID);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	private BasicNodeDatumFilter allCriteriaFilter() {
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
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

	@Test
	public void allCriteria() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = allCriteriaFilter();
		givenAllCriteriaArrays(f);
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", f, TEST_NODE_ID);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, with node ID criteria used in place of default node")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-filtered-datum-02.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(SelectFilteredDatum.DEFAULT_FETCH_SIZE);
		then(stmt).should(ordered).setArray(1, array);
		then(stmt).should(ordered).setArray(2, array2);
		then(stmt).should(ordered).setArray(3, array3);
		then(stmt).should(ordered).setObject(4, start.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setObject(5, end.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
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
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", f, TEST_NODE_ID);

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
			.isEqualToNormalizingWhitespace(utf8StringResource("select-filtered-datum-count-02.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(((SqlProvider) count).getSql())
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setArray(1, array);
		then(stmt).should(ordered).setArray(2, array2);
		then(stmt).should(ordered).setArray(3, array3);
		then(stmt).should(ordered).setObject(4, start.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setObject(5, end.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setArray(6, array3);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void locationIds() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setObjectKind(ObjectDatumKind.Location);
		f.setObjectId(3L);
		given(conn.createArrayOf(eq("bigint"), aryEq(new Long[] { 3L }))).willReturn(array);
		SelectFilteredDatum sql = new SelectFilteredDatum("solaruhist", f, TEST_NODE_ID);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, merging the location datum of all nodes, preferring the given node")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-filtered-datum-03.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(sql.getSql())
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(SelectFilteredDatum.DEFAULT_FETCH_SIZE);
		then(stmt).should(ordered).setArray(1, array);
		then(stmt).should(ordered).setLong(2, TEST_NODE_ID);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void locationIds_noNodeId() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setObjectKind(ObjectDatumKind.Location);
		f.setObjectId(3L);
		f.setStartDate(start);
		given(conn.createArrayOf(eq("bigint"), aryEq(new Long[] { 3L }))).willReturn(array);
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", f, null);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, merging the location datum of all nodes, preferring the lowest node ID")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-filtered-datum-08.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(SelectFilteredDatum.DEFAULT_FETCH_SIZE);
		then(stmt).should(ordered).setArray(1, array);
		then(stmt).should(ordered).setObject(2, start.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	private BasicNodeDatumFilter locationIdsMostRecentFilter() {
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
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
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", f, TEST_NODE_ID);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, with most recent merged location datum, without node sort")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-filtered-datum-07.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(SelectFilteredDatum.DEFAULT_FETCH_SIZE);
		then(stmt).should(ordered).setObject(1, start.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setObject(2, end.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setArray(3, array3);
		then(stmt).should(ordered).setArray(4, array);
		then(stmt).should(ordered).setArray(5, array2);
		then(stmt).should(ordered).setArray(6, array3);
		then(stmt).should(ordered).setLong(7, TEST_NODE_ID);
		then(stmt).should(ordered).setInt(8, 10);
		then(stmt).should(ordered).setLong(9, 20L);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void locationIds_mostRecent_count() throws SQLException {
		// GIVEN
		givenPrepareCount();
		final BasicNodeDatumFilter f = locationIdsMostRecentFilter();
		givenAllCriteriaArrays(f);
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", f, TEST_NODE_ID);

		// WHEN
		PreparedStatementCreator count = sql.countPreparedStatementCreator();
		count.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("Count SQL generated, counting merged location datum")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-filtered-datum-count-07.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(((SqlProvider) count).getSql())
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setObject(1, start.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setObject(2, end.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setArray(3, array3);
		then(stmt).should(ordered).setArray(4, array);
		then(stmt).should(ordered).setArray(5, array2);
		then(stmt).should(ordered).setArray(6, array3);
		then(stmt).should(ordered).setLong(7, TEST_NODE_ID);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void objectIds_noKind_nodeIds() throws SQLException {
		// GIVEN
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setObjectId(1L);
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", f, TEST_NODE_ID);

		// WHEN
		String result = sql.getSql();

		// THEN
		// @formatter:off
		and.then(result)
			.as("Object IDs without kind treated as node IDs")
			.contains("WHERE m.node_id = ANY(?)\n\tAND m.loc_id IS NULL\n")
			;
		// @formatter:on
	}

	private BasicNodeDatumFilter mostRecentFilter() {
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setObjectKind(ObjectDatumKind.Location);
		f.setMostRecent(true);
		f.setType(AcEnergyDatum.class.getName());
		f.setStartDate(start);
		f.setEndDate(end);
		return f;
	}

	@Test
	public void mostRecent() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = mostRecentFilter();
		given(conn.createArrayOf(eq("text"), aryEq(f.getTypes()))).willReturn(array);
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", f, TEST_NODE_ID);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, with date and type criteria applied to most recent datum of streams")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-filtered-datum-04.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(SelectFilteredDatum.DEFAULT_FETCH_SIZE);
		then(stmt).should(ordered).setObject(1, start.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setObject(2, end.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setArray(3, array);
		then(stmt).should(ordered).setLong(4, TEST_NODE_ID);
		then(stmt).should(ordered).setArray(5, array);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void mostRecent_count() throws SQLException {
		// GIVEN
		givenPrepareCount();
		final BasicNodeDatumFilter f = mostRecentFilter();
		given(conn.createArrayOf(eq("text"), aryEq(f.getTypes()))).willReturn(array);
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", f, TEST_NODE_ID);

		// WHEN
		sql.countPreparedStatementCreator().createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("Count SQL generated")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-filtered-datum-count-04.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setObject(1, start.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setObject(2, end.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setArray(3, array);
		then(stmt).should(ordered).setLong(4, TEST_NODE_ID);
		then(stmt).should(ordered).setArray(5, array);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void nodeKind_allNodes() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setObjectKind(ObjectDatumKind.Node);
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", f, null);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, for node datum of any node")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-filtered-datum-05.sql", getClass()))
			;
		then(stmt).should().setFetchSize(SelectFilteredDatum.DEFAULT_FETCH_SIZE);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void noCriteria() throws SQLException {
		// GIVEN
		givenPrepare();
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", new BasicNodeDatumFilter(), null,
				0);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, without WHERE clause")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-filtered-datum-06.sql", getClass()))
			;
		then(stmt).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void sorts_unsupportedIgnored() {
		// GIVEN
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setSorts(Arrays.asList(new SimpleSortDescriptor("foo", true), null,
				new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_LOCATION, true),
				new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_LOCATION)));
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", f, TEST_NODE_ID);

		// WHEN
		String result = sql.getSql();

		// THEN
		// @formatter:off
		and.then(result)
			.as("Unsupported and repeated sort keys ignored, default sorts added")
			.endsWith("ORDER BY m.loc_id DESC, m.node_id, m.source_id, d.ts")
			;
		// @formatter:on
	}

	@Test
	public void sorts_nullSortKeyIgnored() {
		// GIVEN
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setSorts(List.of(new SortDescriptor() {

			@Override
			public String getSortKey() {
				return null;
			}

			@Override
			public boolean isDescending() {
				return true;
			}
		}));
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", f, TEST_NODE_ID);

		// WHEN
		String result = sql.getSql();

		// THEN
		// @formatter:off
		and.then(result)
			.as("Null sort key ignored")
			.endsWith("ORDER BY m.node_id, m.loc_id, m.source_id, d.ts")
			;
		// @formatter:on
	}

	@Test
	public void pagination_zeroValuesOmitted() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setMax(0);
		f.setOffset(0L);
		SelectFilteredDatum sql = new SelectFilteredDatum("solarhist", f, TEST_NODE_ID);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("No LIMIT or OFFSET for zero values")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-filtered-datum-01.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(SelectFilteredDatum.DEFAULT_FETCH_SIZE);
		then(stmt).should(ordered).setLong(1, TEST_NODE_ID);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

}

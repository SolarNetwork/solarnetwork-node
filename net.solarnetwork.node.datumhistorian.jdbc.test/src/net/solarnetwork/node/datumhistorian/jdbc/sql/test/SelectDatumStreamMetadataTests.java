/* ==================================================================
 * SelectDatumStreamMetadataTests.java - 13 Nov 2026 10:18:37 am
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
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatumStreamMetadata;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.service.FilterableDatumHistorian;

/**
 * Test cases for the {@link SelectDatumStreamMetadata} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class SelectDatumStreamMetadataTests {

	private static final long TEST_NODE_ID = 123L;

	@Mock
	private Connection conn;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array objectIdsArray;

	@Mock
	private Array streamIdsArray;

	@Mock
	private Array sourceIdsArray;

	@Mock
	private Array typesArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepare() throws SQLException {
		given(conn.prepareStatement(sqlCaptor.capture())).willReturn(stmt);
	}

	@Test
	public void construct_invalid() {
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectDatumStreamMetadata("solarhist", null, 1L, null));
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectDatumStreamMetadata("solar.hist", f, 1L, null));
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectDatumStreamMetadata(null, f, 1L, null));
	}

	@Test
	public void defaultNode() throws SQLException {
		// GIVEN
		givenPrepare();
		SelectDatumStreamMetadata sql = new SelectDatumStreamMetadata("solarhist",
				new BasicNodeDatumFilter(), TEST_NODE_ID, null);

		// WHEN
		PreparedStatement result = sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Statement created")
			.isSameAs(stmt)
			;
		and.then(sqlCaptor.getValue())
			.as("SQL generated, limited to node, by node, location, and source")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-datum-stream-meta-01.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(sql.getSql())
			;
		and.then(sql)
			.as("Schema name provided")
			.returns("solarhist", SelectDatumStreamMetadata::getSchemaName)
			;
		then(stmt).should().setObject(1, TEST_NODE_ID, Types.BIGINT);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void allCriteria() throws SQLException {
		// GIVEN
		givenPrepare();
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setObjectKind(ObjectDatumKind.Node);
		f.setObjectIds(new Long[] { 1L, 2L });
		f.setSourceIds(new String[] { "a", "b/*" });
		f.setType(AcEnergyDatum.class.getName());

		// the datum criteria and pagination do not apply to metadata
		f.setStartDate(Instant.now());
		f.setEndDate(Instant.now().plusSeconds(60));
		f.setMostRecent(true);
		f.setSorts(List.of(new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_TIME, true)));
		f.setMax(10);
		f.setOffset(20L);

		final Integer[] streamIds = new Integer[] { 3, 4 };
		given(conn.createArrayOf(eq("bigint"), aryEq(f.getObjectIds()))).willReturn(objectIdsArray);
		given(conn.createArrayOf(eq("integer"), aryEq(streamIds))).willReturn(streamIdsArray);
		given(conn.createArrayOf(eq("text"), aryEq(f.getSourceIds()))).willReturn(sourceIdsArray);
		given(conn.createArrayOf(eq("text"), aryEq(f.getTypes()))).willReturn(typesArray);
		SelectDatumStreamMetadata sql = new SelectDatumStreamMetadata("solarhist", f, TEST_NODE_ID,
				streamIds);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, with only object, stream, source, and type criteria")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-datum-stream-meta-02.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setArray(1, objectIdsArray);
		then(stmt).should(ordered).setArray(2, streamIdsArray);
		then(stmt).should(ordered).setArray(3, sourceIdsArray);
		then(stmt).should(ordered).setArray(4, typesArray);
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
		SelectDatumStreamMetadata sql = new SelectDatumStreamMetadata("solaruhist", f, TEST_NODE_ID,
				new Integer[] { 7 });

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, for the location streams of all nodes, with single IDs compared directly")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-datum-stream-meta-03.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setObject(1, 3L);
		then(stmt).should(ordered).setObject(2, 7);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void locationKind_defaultNode() {
		// GIVEN
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setObjectKind(ObjectDatumKind.Location);
		SelectDatumStreamMetadata sql = new SelectDatumStreamMetadata("solarhist", f, TEST_NODE_ID,
				null);

		// WHEN
		String result = sql.getSql();

		// THEN
		// @formatter:off
		and.then(result)
			.as("Location streams of node selected")
			.contains("WHERE m.node_id = ?\n\tAND m.loc_id IS NOT NULL\nORDER BY ")
			;
		// @formatter:on
	}

	@Test
	public void noCriteria() throws SQLException {
		// GIVEN
		givenPrepare();
		SelectDatumStreamMetadata sql = new SelectDatumStreamMetadata("solarhist",
				new BasicNodeDatumFilter(), null, new Integer[0]);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated, without WHERE clause for no node ID and empty stream IDs")
			.isEqualTo("""
					SELECT m.stream_id, m.node_id, m.source_id, m.loc_id, m.names_i, m.names_a, m.names_s
					FROM solarhist.dh_datum_meta m
					ORDER BY m.node_id, m.loc_id, m.source_id""")
			;
		then(stmt).shouldHaveNoInteractions();
		// @formatter:on
	}

}

/* ==================================================================
 * SelectDatumTests.java - 10 Nov 2026 11:12:40 pm
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatum;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;

/**
 * Test cases for the {@link SelectDatum} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class SelectDatumTests {

	private static final long TEST_NODE_ID = 123L;

	@Mock
	private Connection conn;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array array;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private PreparedStatement givenPrepare() throws SQLException {
		given(conn.prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT)))
				.willReturn(stmt);
		return stmt;
	}

	@Test
	public void source() throws SQLException {
		// GIVEN
		givenPrepare();
		SelectDatum sql = SelectDatum.forSource("solarhist", TEST_NODE_ID, "a", null, 0, 1, null);

		// WHEN
		PreparedStatement result = sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Statement created")
			.isSameAs(stmt)
			;
		and.then(sqlCaptor.getValue())
			.as("SQL generated")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-datum-source-01.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(sql.getSql())
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(SelectDatum.DEFAULT_FETCH_SIZE);
		then(stmt).should(ordered).setLong(1, 1L);
		then(stmt).should(ordered).setLong(2, TEST_NODE_ID);
		then(stmt).should(ordered).setString(3, "a");
		then(stmt).should(ordered).setLong(4, 1L);
		then(stmt).should(ordered).setLong(5, 1L);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void source_timestamp_type() throws SQLException {
		// GIVEN
		givenPrepare();
		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final String type = AcEnergyDatum.class.getName();
		SelectDatum sql = SelectDatum.forSource("solarhist", TEST_NODE_ID, "a", ts, 1, 2, type);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-datum-source-02.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(SelectDatum.DEFAULT_FETCH_SIZE);
		then(stmt).should(ordered).setObject(1, ts.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setLong(2, 3L);
		then(stmt).should(ordered).setLong(3, TEST_NODE_ID);
		then(stmt).should(ordered).setString(4, "a");
		then(stmt).should(ordered).setLong(5, TEST_NODE_ID);
		then(stmt).should(ordered).setString(6, type);
		then(stmt).should(ordered).setLong(7, 2L);
		then(stmt).should(ordered).setLong(8, 3L);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void streams_all() throws SQLException {
		// GIVEN
		givenPrepare();
		SelectDatum sql = SelectDatum.forStreams("solarhist", TEST_NODE_ID, null, null, 0, null);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-datum-streams-01.sql", getClass()))
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(SelectDatum.DEFAULT_FETCH_SIZE);
		then(stmt).should(ordered).setLong(1, 1L);
		then(stmt).should(ordered).setLong(2, TEST_NODE_ID);
		then(stmt).should(ordered).setLong(3, 1L);
		then(stmt).should(ordered).setLong(4, 1L);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void streams_ids_timestamp_type() throws SQLException {
		// GIVEN
		givenPrepare();
		given(stmt.getConnection()).willReturn(conn);
		given(conn.createArrayOf(eq("integer"), aryEq(new Object[] { 1, 2 }))).willReturn(array);

		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final String type = AcEnergyDatum.class.getName();
		SelectDatum sql = SelectDatum.forStreams("solaruhist", TEST_NODE_ID, List.of(1, 2), ts, 2,
				type);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("SQL generated for schema")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-datum-streams-02.sql", getClass()))
			;
		and.then(sql.getSchemaName())
			.as("Schema name provided")
			.isEqualTo("solaruhist")
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setFetchSize(SelectDatum.DEFAULT_FETCH_SIZE);
		then(stmt).should(ordered).setObject(1, ts.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setLong(2, 3L);
		then(stmt).should(ordered).setLong(3, TEST_NODE_ID);
		then(stmt).should(ordered).getConnection();
		then(stmt).should(ordered).setArray(4, array);
		then(stmt).should(ordered).setLong(5, TEST_NODE_ID);
		then(stmt).should(ordered).setString(6, type);
		then(stmt).should(ordered).setLong(7, 3L);
		then(stmt).should(ordered).setLong(8, 3L);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void setValues_withoutCreate() throws SQLException {
		// GIVEN
		SelectDatum sql = SelectDatum.forSource("solarhist", TEST_NODE_ID, "a", null, 4, 5, null);

		// WHEN
		sql.setValues(stmt);

		// THEN
		// @formatter:off
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setLong(1, 9L);
		then(stmt).should(ordered).setLong(2, TEST_NODE_ID);
		then(stmt).should(ordered).setString(3, "a");
		then(stmt).should(ordered).setLong(4, 5L);
		then(stmt).should(ordered).setLong(5, 9L);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void maxRow_noOverflow() throws SQLException {
		// GIVEN
		SelectDatum sql = SelectDatum.forSource("solarhist", TEST_NODE_ID, "a", null,
				Integer.MAX_VALUE, Integer.MAX_VALUE, null);

		// WHEN
		sql.setValues(stmt);

		// THEN
		final long max = 2L * Integer.MAX_VALUE;
		then(stmt).should().setLong(4, Integer.MAX_VALUE + 1L);
		then(stmt).should().setLong(1, max);
		then(stmt).should().setLong(5, max);
	}

	@Test
	public void source_fetchSize() throws SQLException {
		// GIVEN
		givenPrepare();
		SelectDatum sql = SelectDatum.forSource("solarhist", TEST_NODE_ID, "a", null, 0, 1, null,
				5);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		then(stmt).should().setFetchSize(5);
	}

	@Test
	public void streams_fetchSize_driverDefault() throws SQLException {
		// GIVEN
		givenPrepare();
		SelectDatum sql = SelectDatum.forStreams("solarhist", TEST_NODE_ID, null, null, 0, null, 0);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// no fetch size set, so the JDBC driver default is used
		then(stmt).should(never()).setFetchSize(anyInt());
	}

	@Test
	public void invalidSchema() {
		thenIllegalArgumentException()
				.isThrownBy(() -> SelectDatum.forSource("bad; DROP", TEST_NODE_ID, "a", null, 0, 1,
						null))
				.withMessageContaining("bad; DROP");
		thenIllegalArgumentException()
				.isThrownBy(() -> SelectDatum.forStreams(null, TEST_NODE_ID, null, null, 0, null));
	}

	@Test
	public void invalidOffset() {
		thenIllegalArgumentException()
				.isThrownBy(
						() -> SelectDatum.forSource("solarhist", TEST_NODE_ID, "a", null, -1, 1, null))
				.withMessageContaining("offset");
		thenIllegalArgumentException().isThrownBy(
				() -> SelectDatum.forStreams("solarhist", TEST_NODE_ID, null, null, -1, null));
	}

	@Test
	public void invalidCount() {
		thenIllegalArgumentException()
				.isThrownBy(
						() -> SelectDatum.forSource("solarhist", TEST_NODE_ID, "a", null, 0, 0, null))
				.withMessageContaining("count");
	}

	@Test
	public void nullSourceId() {
		thenIllegalArgumentException().isThrownBy(
				() -> SelectDatum.forSource("solarhist", TEST_NODE_ID, null, null, 0, 1, null));
	}

}

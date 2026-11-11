/* ==================================================================
 * StoreDatumTests.java - 10 Nov 2026 11:24:08 pm
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.datumhistorian.jdbc.sql.StoreDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;

/**
 * Test cases for the {@link StoreDatum} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class StoreDatumTests {

	private static final long TEST_NODE_ID = 123L;
	private static final Long TEST_LOC_ID = 234L;
	private static final String JDATA = "{\"i\":{\"watts\":1}}";

	@Mock
	private Connection conn;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array array;

	@Mock
	private ResultSet resultSet;

	private final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

	@Test
	public void typeId_nodeDatum() throws SQLException {
		// GIVEN
		final String expectedSql = """
				SELECT ?::INTEGER AS type_id, solarhist.dh_store_datum(?, ?, ?, ?, ?, ?) AS stream_id""";
		given(conn.prepareStatement(expectedSql)).willReturn(stmt);
		SimpleDatum d = SimpleDatum.nodeDatum("a", ts, new DatumSamples());
		StoreDatum sql = new StoreDatum("solarhist", TEST_NODE_ID, d, 3, JDATA);

		// WHEN
		PreparedStatement result = sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Statement created")
			.isSameAs(stmt)
			;
		and.then(sql.getSql())
			.as("SQL provided")
			.isEqualTo(expectedSql)
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setInt(1, 3);
		then(stmt).should(ordered).setObject(2, ts.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setObject(3, TEST_NODE_ID);
		then(stmt).should(ordered).setString(4, "a");
		then(stmt).should(ordered).setNull(5, Types.BIGINT);
		then(stmt).should(ordered).setInt(6, 3);
		then(stmt).should(ordered).setString(7, JDATA);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void typeNames_locationDatum() throws SQLException {
		// GIVEN
		final String expectedSql = """
				SELECT t.type_id, solaruhist.dh_store_datum(?, ?, ?, ?, t.type_id, ?) AS stream_id
				FROM solaruhist.dh_store_datum_type(?, ?) AS t(type_id)""";
		given(conn.prepareStatement(expectedSql)).willReturn(stmt);
		given(stmt.getConnection()).willReturn(conn);
		final String[] typeNames = new String[] { "foo.Bar", "foo.Datum" };
		given(conn.createArrayOf(eq("text"), aryEq((Object[]) typeNames))).willReturn(array);
		SimpleDatum d = SimpleDatum.locationDatum(TEST_LOC_ID, "a", ts, new DatumSamples());
		StoreDatum sql = new StoreDatum("solaruhist", TEST_NODE_ID, d,
				new LinkedHashSet<>(List.of(typeNames)), JDATA);

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(sql.getSchemaName())
			.as("Schema name provided")
			.isEqualTo("solaruhist")
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setObject(1, ts.atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		then(stmt).should(ordered).setObject(2, TEST_NODE_ID);
		then(stmt).should(ordered).setString(3, "a");
		then(stmt).should(ordered).setObject(4, TEST_LOC_ID, Types.BIGINT);
		then(stmt).should(ordered).setString(5, JDATA);
		then(stmt).should(ordered).setString(6, "foo.Bar");
		then(stmt).should(ordered).getConnection();
		then(stmt).should(ordered).setArray(7, array);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void resultMapper() throws SQLException {
		// GIVEN
		given(resultSet.getInt(1)).willReturn(1);
		given(resultSet.getInt(2)).willReturn(2);

		// WHEN
		StoreDatum.Result result = StoreDatum.RESULT_MAPPER.mapRow(resultSet, 1);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result mapped")
			.isEqualTo(new StoreDatum.Result(1, 2))
			;
		// @formatter:on
	}

	@Test
	public void invalidArguments() {
		final SimpleDatum d = SimpleDatum.nodeDatum("a", ts, new DatumSamples());
		thenIllegalArgumentException()
				.isThrownBy(() -> new StoreDatum("Bad-Schema", TEST_NODE_ID, d, 1, JDATA));
		thenIllegalArgumentException()
				.isThrownBy(() -> new StoreDatum("solarhist", TEST_NODE_ID, null, 1, JDATA));
		thenIllegalArgumentException()
				.isThrownBy(() -> new StoreDatum("solarhist", TEST_NODE_ID, d, 1, null));
		thenIllegalArgumentException().isThrownBy(
				() -> new StoreDatum("solarhist", TEST_NODE_ID, d, (Set<String>) null, JDATA));
		thenIllegalArgumentException()
				.isThrownBy(() -> new StoreDatum("solarhist", TEST_NODE_ID, d, Set.of(), JDATA));
	}

}

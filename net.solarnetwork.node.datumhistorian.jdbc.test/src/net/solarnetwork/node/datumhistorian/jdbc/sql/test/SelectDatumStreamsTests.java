/* ==================================================================
 * SelectDatumStreamsTests.java - 10 Nov 2026 11:31:52 pm
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

import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatumStreams;

/**
 * Test cases for the {@link SelectDatumStreams} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class SelectDatumStreamsTests {

	private static final long TEST_NODE_ID = 123L;

	@Mock
	private Connection conn;

	@Mock
	private PreparedStatement stmt;

	@Test
	public void select() throws SQLException {
		// GIVEN
		final String expectedSql = """
				SELECT stream_id, source_id, loc_id
				FROM solaruhist.dh_datum_meta
				WHERE node_id = ?
				ORDER BY stream_id""";
		given(conn.prepareStatement(expectedSql)).willReturn(stmt);
		SelectDatumStreams sql = new SelectDatumStreams("solaruhist", TEST_NODE_ID);

		// WHEN
		PreparedStatement result = sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Statement created")
			.isSameAs(stmt)
			;
		and.then(sql)
			.as("SQL provided")
			.returns(expectedSql, SelectDatumStreams::getSql)
			.as("Schema name provided")
			.returns("solaruhist", SelectDatumStreams::getSchemaName)
			;
		then(stmt).should().setLong(1, TEST_NODE_ID);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void invalidSchema() {
		thenIllegalArgumentException()
				.isThrownBy(() -> new SelectDatumStreams("x.dh_datum_meta; --", TEST_NODE_ID));
	}

}

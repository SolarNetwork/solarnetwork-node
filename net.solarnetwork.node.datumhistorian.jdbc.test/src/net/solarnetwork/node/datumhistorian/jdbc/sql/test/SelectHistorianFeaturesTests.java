/* ==================================================================
 * SelectHistorianFeaturesTests.java - 15 Nov 2026 5:38:12 pm
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectHistorianFeatures;

/**
 * Test cases for the {@link SelectHistorianFeatures} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class SelectHistorianFeaturesTests {

	@Mock
	private Connection conn;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private ResultSet rs;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	@Test
	public void construct_invalid() {
		thenIllegalArgumentException().isThrownBy(() -> new SelectHistorianFeatures(null));
		thenIllegalArgumentException().isThrownBy(() -> new SelectHistorianFeatures("solar.hist"));
	}

	@Test
	public void sql() throws SQLException {
		// GIVEN
		given(conn.prepareStatement(sqlCaptor.capture())).willReturn(stmt);
		final SelectHistorianFeatures sql = new SelectHistorianFeatures("solarhist");

		// WHEN
		PreparedStatement result = sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Statement created")
			.isSameAs(stmt)
			;
		and.then(sqlCaptor.getValue())
			.as("SQL generated, testing for each aggregate setup table and sub-hour function")
			.isEqualToNormalizingWhitespace(utf8StringResource("select-historian-features-01.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(sql.getSql())
			;
		and.then(sql)
			.as("Schema name")
			.returns("solarhist", SelectHistorianFeatures::getSchemaName)
			;
		then(stmt).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void mapRow() throws SQLException {
		// GIVEN
		given(rs.getBoolean(1)).willReturn(true);
		given(rs.getBoolean(2)).willReturn(true);

		// WHEN
		SelectHistorianFeatures.Result result = SelectHistorianFeatures.RESULT_MAPPER.mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Aggregates from first column")
			.returns(true, SelectHistorianFeatures.Result::aggregates)
			.as("Minute aggregates from second column")
			.returns(true, SelectHistorianFeatures.Result::minuteAggregates)
			;
		// @formatter:on
	}

	@Test
	public void mapRow_aggregatesWithoutMinuteAggregates() throws SQLException {
		// GIVEN
		given(rs.getBoolean(1)).willReturn(true);
		given(rs.getBoolean(2)).willReturn(false);

		// WHEN
		SelectHistorianFeatures.Result result = SelectHistorianFeatures.RESULT_MAPPER.mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result mapped, for an aggregate setup from before the sub-hour functions")
			.isEqualTo(new SelectHistorianFeatures.Result(true, false))
			;
		// @formatter:on
	}

	@Test
	public void mapRow_noAggregates() throws SQLException {
		// GIVEN
		given(rs.getBoolean(1)).willReturn(false);
		given(rs.getBoolean(2)).willReturn(false);

		// WHEN
		SelectHistorianFeatures.Result result = SelectHistorianFeatures.RESULT_MAPPER.mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result mapped")
			.isEqualTo(new SelectHistorianFeatures.Result(false, false))
			;
		// @formatter:on
	}

}

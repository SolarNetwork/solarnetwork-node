/* ==================================================================
 * StoreNodeTimeZoneTests.java - 15 Nov 2026 5:41:27 pm
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
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import net.solarnetwork.node.datumhistorian.jdbc.sql.StoreNodeTimeZone;

/**
 * Test cases for the {@link StoreNodeTimeZone} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class StoreNodeTimeZoneTests {

	private static final long TEST_NODE_ID = 123L;
	private static final ZoneId TEST_ZONE = ZoneId.of("Pacific/Auckland");

	@Mock
	private Connection conn;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	@Test
	public void construct_invalid() {
		thenIllegalArgumentException()
				.isThrownBy(() -> new StoreNodeTimeZone(null, TEST_NODE_ID, TEST_ZONE));
		thenIllegalArgumentException()
				.isThrownBy(() -> new StoreNodeTimeZone("solar.hist", TEST_NODE_ID, TEST_ZONE));
		thenIllegalArgumentException()
				.isThrownBy(() -> new StoreNodeTimeZone("solarhist", TEST_NODE_ID, null));
	}

	@Test
	public void sql() throws SQLException {
		// GIVEN
		given(conn.prepareStatement(sqlCaptor.capture())).willReturn(stmt);
		final StoreNodeTimeZone sql = new StoreNodeTimeZone("solarhist", TEST_NODE_ID, TEST_ZONE);

		// WHEN
		PreparedStatement result = sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Statement created")
			.isSameAs(stmt)
			;
		and.then(sqlCaptor.getValue())
			.as("SQL generated, updating only a different zone")
			.isEqualToNormalizingWhitespace(utf8StringResource("store-node-time-zone-01.sql", getClass()))
			.as("Same SQL from SqlProvider")
			.isEqualTo(sql.getSql())
			;
		and.then(sql)
			.as("Schema name")
			.returns("solarhist", StoreNodeTimeZone::getSchemaName)
			.as("Node ID")
			.returns(TEST_NODE_ID, StoreNodeTimeZone::getNodeId)
			.as("Time zone")
			.returns(TEST_ZONE, StoreNodeTimeZone::getTimeZone)
			.as("Region zone stored by its ID")
			.returns("Pacific/Auckland", StoreNodeTimeZone::getTimeZoneName)
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setLong(1, TEST_NODE_ID);
		then(stmt).should(ordered).setString(2, "Pacific/Auckland");
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void sql_fixedOffsetZone() throws SQLException {
		// GIVEN
		given(conn.prepareStatement(sqlCaptor.capture())).willReturn(stmt);
		final StoreNodeTimeZone sql = new StoreNodeTimeZone("solarhist", TEST_NODE_ID,
				ZoneOffset.ofHours(12));

		// WHEN
		sql.createPreparedStatement(conn);

		// THEN
		// @formatter:off
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setLong(1, TEST_NODE_ID);
		then(stmt).should(ordered).setString(2, "<+1200>-12:00");
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void timeZoneNames() {
		// GIVEN
		final Map<ZoneId, String> zoneNames = new LinkedHashMap<>(8);
		zoneNames.put(ZoneId.of("America/New_York"), "America/New_York");
		zoneNames.put(ZoneOffset.UTC, "UTC");
		zoneNames.put(ZoneId.of("Etc/UTC"), "UTC");
		zoneNames.put(ZoneId.of("GMT"), "UTC");
		zoneNames.put(ZoneOffset.ofHours(12), "<+1200>-12:00");
		zoneNames.put(ZoneId.of("GMT-05:30"), "<-0530>+05:30");
		zoneNames.put(ZoneId.of("UTC+01"), "<+0100>-01:00");
		zoneNames.put(ZoneId.of("Etc/GMT-12"), "Etc/GMT-12");

		// WHEN
		final Map<ZoneId, String> result = new LinkedHashMap<>(8);
		for ( ZoneId zone : zoneNames.keySet() ) {
			result.put(zone, new StoreNodeTimeZone("solarhist", TEST_NODE_ID, zone).getTimeZoneName());
		}

		// THEN
		// @formatter:off
		and.then(result)
			.as("Region zones stored as is, zones without offset as UTC, and fixed offset zones as POSIX zones with the offset reversed")
			.containsExactlyEntriesOf(zoneNames)
			;
		// @formatter:on
	}

}

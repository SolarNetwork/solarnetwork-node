/* ==================================================================
 * HistorianSqlUtilsTests.java - 10 Nov 2026 11:35:17 pm
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

import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.prepareLocalDateRange;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.requireValidSchemaName;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.timeZoneId;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.timeZoneName;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.whereLocalDateRange;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils;

/**
 * Test cases for the {@link HistorianSqlUtils} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class HistorianSqlUtilsTests {

	private static final LocalDateTime TEST_START = LocalDateTime.of(2026, 1, 1, 0, 0);
	private static final LocalDateTime TEST_END = LocalDateTime.of(2026, 2, 1, 0, 0);

	@Mock
	private PreparedStatement stmt;

	@Test
	public void validSchemaNames() {
		for ( String name : new String[] { "solarhist", "solaruhist", "_x", "a1_b2", "a".repeat(63) } ) {
			// @formatter:off
			and.then(requireValidSchemaName(name))
				.as("Valid schema name [%s] returned", name)
				.isSameAs(name)
				;
			// @formatter:on
		}
	}

	@Test
	public void invalidSchemaNames() {
		for ( String name : new String[] { null, "", "1abc", "SolarHist", "solar-hist", "solar hist",
				"solarhist.dh_datum", "solarhist;", "\"solarhist\"", "a".repeat(64) } ) {
			thenIllegalArgumentException().as("Invalid schema name [%s] rejected", name)
					.isThrownBy(() -> requireValidSchemaName(name));
		}
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
			result.put(zone, timeZoneName(zone));
		}

		// THEN
		// @formatter:off
		and.then(result)
			.as("Region zones named as is, zones without offset as UTC, and fixed offset zones as POSIX zones with the offset reversed")
			.containsExactlyEntriesOf(zoneNames)
			;
		// @formatter:on
	}

	@Test
	public void timeZoneIds() {
		// GIVEN
		final Map<String, String> zoneIds = new LinkedHashMap<>(8);
		zoneIds.put("<+1200>-12:00", "+12:00");
		zoneIds.put("<-0530>+05:30", "-05:30");
		zoneIds.put("<+053015>-05:30:15", "+05:30:15");
		zoneIds.put("UTC", "UTC");
		zoneIds.put("Pacific/Auckland", "Pacific/Auckland");
		zoneIds.put("Etc/GMT-12", "Etc/GMT-12");
		zoneIds.put("<+1200>", "<+1200>");

		// WHEN
		final Map<String, String> result = new LinkedHashMap<>(8);
		for ( String name : zoneIds.keySet() ) {
			result.put(name, timeZoneId(name));
		}

		// THEN
		// @formatter:off
		and.then(result)
			.as("POSIX fixed offset zones given as Java offset IDs, other names as is")
			.containsExactlyEntriesOf(zoneIds)
			;
		and.then(timeZoneId(null))
			.as("No ID for no name")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void timeZoneIds_roundTrip() {
		for ( ZoneId zone : new ZoneId[] { ZoneId.of("America/New_York"), ZoneId.of("Etc/UTC"),
				ZoneOffset.ofHours(12), ZoneId.of("GMT-05:30"),
				ZoneOffset.ofHoursMinutesSeconds(5, 30, 15), ZoneId.of("Etc/GMT-12") } ) {
			// WHEN
			final String id = timeZoneId(timeZoneName(zone));

			// THEN
			for ( Instant ts : new Instant[] { Instant.parse("2026-01-15T00:00:00Z"),
					Instant.parse("2026-07-15T00:00:00Z") } ) {
				// @formatter:off
				and.then(ZoneId.of(id).getRules().getOffset(ts))
					.as("Zone %s has the same offset at %s after being named and read back as %s", zone, ts, id)
					.isEqualTo(zone.getRules().getOffset(ts))
					;
				// @formatter:on
			}
		}
	}

	@Test
	public void appendNodeTimeZoneJoin() {
		// GIVEN
		final StringBuilder buf = new StringBuilder();

		// WHEN
		HistorianSqlUtils.appendNodeTimeZoneJoin("solarhist", buf);

		// THEN
		// @formatter:off
		and.then(buf.toString())
			.as("Join of node time zone table as z table, to stream meta table, as line")
			.isEqualTo("LEFT OUTER JOIN solarhist.dh_node_time_zone z ON z.node_id = m.node_id\n")
			;
		and.then(HistorianSqlUtils.STREAM_TIME_ZONE_SQL)
			.as("Stream zone is zone of joined node, or UTC, as dh_stream_time_zone() gives")
			.isEqualTo("COALESCE(z.time_zone, 'UTC')")
			;
		// @formatter:on
	}

	@Test
	public void whereLocalDateRange_both() {
		// GIVEN
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setLocalStartDate(TEST_START);
		f.setLocalEndDate(TEST_END);
		final StringBuilder buf = new StringBuilder();

		// WHEN
		final int result = whereLocalDateRange(f, "d.ts", "z.tz", buf);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Parameter of each date")
			.isEqualTo(2)
			;
		and.then(buf.toString())
			.as("Minimum and maximum local dates at time zone")
			.isEqualTo("\tAND d.ts >= ? AT TIME ZONE z.tz\n\tAND d.ts < ? AT TIME ZONE z.tz\n")
			;
		// @formatter:on
	}

	@Test
	public void whereLocalDateRange_startOnly() {
		// GIVEN
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setLocalStartDate(TEST_START);
		final StringBuilder buf = new StringBuilder();

		// WHEN
		final int result = whereLocalDateRange(f, "d.ts_start", "'UTC'", buf);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Parameter of start date")
			.isEqualTo(1)
			;
		and.then(buf.toString())
			.as("Minimum local date at time zone")
			.isEqualTo("\tAND d.ts_start >= ? AT TIME ZONE 'UTC'\n")
			;
		// @formatter:on
	}

	@Test
	public void whereLocalDateRange_endOnly() {
		// GIVEN
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setLocalEndDate(TEST_END);
		final StringBuilder buf = new StringBuilder();

		// WHEN
		final int result = whereLocalDateRange(f, "d.ts", "z.tz", buf);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Parameter of end date")
			.isEqualTo(1)
			;
		and.then(buf.toString())
			.as("Maximum local date at time zone")
			.isEqualTo("\tAND d.ts < ? AT TIME ZONE z.tz\n")
			;
		// @formatter:on
	}

	@Test
	public void whereLocalDateRange_none() {
		// GIVEN
		final StringBuilder buf = new StringBuilder();

		// WHEN
		final int result = whereLocalDateRange(new BasicNodeDatumFilter(), "d.ts", "z.tz", buf);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No parameters without local dates")
			.isZero()
			;
		and.then(buf)
			.as("Nothing appended without local dates")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void prepareLocalDateRange_both() throws SQLException {
		// GIVEN
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setLocalStartDate(TEST_START);
		f.setLocalEndDate(TEST_END);

		// WHEN
		final int result = prepareLocalDateRange(f, stmt, 2);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Index of last parameter set")
			.isEqualTo(4)
			;
		InOrder ordered = Mockito.inOrder(stmt);
		then(stmt).should(ordered).setObject(3, TEST_START, Types.TIMESTAMP);
		then(stmt).should(ordered).setObject(4, TEST_END, Types.TIMESTAMP);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void prepareLocalDateRange_endOnly() throws SQLException {
		// GIVEN
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setLocalEndDate(TEST_END);

		// WHEN
		final int result = prepareLocalDateRange(f, stmt, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Index of last parameter set")
			.isEqualTo(1)
			;
		then(stmt).should().setObject(1, TEST_END, Types.TIMESTAMP);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

}

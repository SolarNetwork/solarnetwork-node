/* ==================================================================
 * PostgresAggregateSqlTests.java - 14 Nov 2026 8:40:12 pm
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

package net.solarnetwork.node.datumhistorian.jdbc.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.junit.Assume.assumeTrue;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the SQL of the aggregate setup, {@code postgres-init-agg.sql},
 * using the Postgres test database.
 *
 * <p>
 * The tests are skipped unless the
 * {@link AbstractPostgresHistorianTests#SCHEMAS first schema} has the aggregate
 * setup. Times are UTC unless noted.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class PostgresAggregateSqlTests extends AbstractPostgresHistorianTests {

	private static final String SCHEMA = SCHEMAS.get(0);

	private static final Long TEST_NODE_ID = 1L;

	@Before
	public void setup() {
		final String agg = jdbcTemplate.queryForObject("SELECT to_regclass(?)::TEXT", String.class,
				SCHEMA + ".dh_agg_hourly");
		assumeTrue("The %s schema does not have the aggregate setup.".formatted(SCHEMA), agg != null);
		jdbcTemplate.update("SET LOCAL TimeZone = 'UTC'");
	}

	private String sql(String sql) {
		return sql.replace("solarhist.", SCHEMA + ".");
	}

	/**
	 * Create a stream with one instantaneous property {@code i} and one
	 * accumulating property {@code val}.
	 */
	private int stream(String sourceId) {
		return jdbcTemplate.queryForObject(sql("""
				INSERT INTO solarhist.dh_datum_meta (node_id, source_id, names_i, names_a, type_ids)
				VALUES (?, ?, '{i}', '{val}', '{1}')
				RETURNING stream_id
				"""), Integer.class, TEST_NODE_ID, sourceId);
	}

	/**
	 * Store a datum, creating its partition if needed.
	 *
	 * @param val
	 *        the accumulating value, or {@code null} for none
	 */
	private void datum(int streamId, String ts, int i, Integer val) {
		jdbcTemplate.queryForList(sql("SELECT solarhist.dh_datum_ensure_partition(?::TIMESTAMPTZ)"), ts);
		jdbcTemplate.update(
				sql("""
						INSERT INTO solarhist.dh_datum (ts, stream_id, type_id, data_i, data_a)
						VALUES (?::TIMESTAMPTZ, ?, 1, ARRAY[?]::NUMERIC[], CASE WHEN ?::NUMERIC IS NOT NULL THEN ARRAY[?]::NUMERIC[] END)
						"""),
				ts, streamId, i, val, val);
	}

	/** Process the stale queue of one kind until it is empty. */
	private int process(String kind) {
		int count = 0;
		while ( !jdbcTemplate
				.queryForList(sql("SELECT * FROM solarhist.dh_process_one_agg_stale(?)"), kind)
				.isEmpty() ) {
			count++;
		}
		return count;
	}

	/** Process the stale queue: hours, then days, then months. */
	private void processAll() {
		process("h");
		process("d");
		process("M");
	}

	private List<Map<String, Object>> stale() {
		return jdbcTemplate.queryForList(sql("""
				SELECT agg_kind, to_char(ts_start AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"') AS ts
				FROM solarhist.dh_agg_stale
				ORDER BY agg_kind, ts_start
				"""));
	}

	private List<Map<String, Object>> agg(String table, int streamId) {
		return jdbcTemplate.queryForList(sql("""
				SELECT to_char(ts_start AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"') AS ts
					, data_i::TEXT AS data_i, stat_i::TEXT AS stat_i
					, data_a::TEXT AS data_a, read_a::TEXT AS read_a
				FROM solarhist.%s
				WHERE stream_id = ?
				ORDER BY ts_start
				""".formatted(table)), streamId);
	}

	private Map<String, Object> diff(int streamId, String start, String end) {
		return jdbcTemplate.queryForMap(sql("""
				SELECT to_char(d.ts_start AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"') AS ts_start
					, to_char(d.ts_end AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"') AS ts_end
					, d.read_a::TEXT AS read_a
				FROM (
					SELECT (solarhist.dh_diff_datum(r ORDER BY r.ts)).*
					FROM solarhist.dh_find_datum_diff_rows(?, ?::TIMESTAMPTZ, ?::TIMESTAMPTZ) r
				) d
				"""), streamId, start, end);
	}

	/**
	 * The datum of SolarNet's {@code sample-raw-data-10-net537.csv}, where the
	 * first datum after an hour has no accumulating value.
	 */
	private int net537Stream(String lastTs) {
		final int sid = stream("A");
		datum(sid, "2023-02-06T17:55:00Z", 1, 100);
		datum(sid, "2023-02-06T18:15:00Z", 2, 101);
		datum(sid, "2023-02-06T18:45:00Z", 3, 102);
		datum(sid, "2023-02-06T19:01:00Z", 4, null);
		datum(sid, lastTs, 5, 104);
		return sid;
	}

	/**
	 * A stream whose datum have no accumulating value from 2 to 25 January
	 * 2026, with no datum at all between 5 and 25 January.
	 *
	 * <p>
	 * From 26 January the latest datum more than 14 days earlier is the one on
	 * 5 January, without an accumulating value, so the reading of 1 January is
	 * found only by the search before that "way-back" datum (NET-538).
	 * </p>
	 */
	private int wayBackStream() {
		final int sid = stream("A");
		datum(sid, "2026-01-01T00:00:00Z", 1, 100);
		datum(sid, "2026-01-02T00:00:00Z", 2, null);
		datum(sid, "2026-01-05T00:00:00Z", 3, null);
		datum(sid, "2026-01-25T00:00:00Z", 4, null);
		datum(sid, "2026-01-26T00:30:00Z", 5, 150);
		return sid;
	}

	@Test
	public void hourly_nextDatumWithoutAccumulating_withinClockTolerance() {
		// GIVEN
		final int sid = net537Stream("2023-02-06T19:05:00Z");

		// WHEN
		processAll();

		// THEN
		// @formatter:off
		then(agg("dh_agg_hourly", sid))
			.as("The hour before 19:00 takes its share of 18:45 to 19:05, skipping 19:01 (NET-537)")
			.extracting("ts", "data_i", "stat_i", "data_a", "read_a")
			.containsExactly(
				  tuple("2023-02-06T17:00Z", "{1}", "{{1,1,1}}", "{0.25}", "{{0,100,100}}")
				, tuple("2023-02-06T18:00Z", "{2.5}", "{{2,2,3}}", "{3.25}", "{{2,100,102}}")
				, tuple("2023-02-06T19:00Z", "{4.5}", "{{2,4,5}}", "{0.5}", "{{2,102,104}}")
			)
			;
		// @formatter:on
	}

	@Test
	public void hourly_nextDatumWithoutAccumulating_beyondClockTolerance() {
		// GIVEN
		final int sid = net537Stream("2023-02-06T20:05:00Z");

		// WHEN
		processAll();

		// THEN
		// @formatter:off
		then(agg("dh_agg_hourly", sid))
			.as("A difference between readings more than an hour apart is not shared out")
			.extracting("ts", "data_a", "read_a")
			.containsExactly(
				  tuple("2023-02-06T17:00Z", "{0.25}", "{{0,100,100}}")
				, tuple("2023-02-06T18:00Z", "{1.75}", "{{2,100,102}}")
				, tuple("2023-02-06T19:00Z", "{0}", "{{0,102,102}}")
				, tuple("2023-02-06T20:00Z", "{0}", "{{2,102,104}}")
			)
			;
		// @formatter:on
	}

	@Test
	public void stale_insertExactlyOnHour() {
		// GIVEN
		final int sid = stream("A");
		datum(sid, "2026-01-01T09:50:00Z", 1, 100);
		datum(sid, "2026-01-01T11:10:00Z", 1, 120);
		jdbcTemplate.update(sql("DELETE FROM solarhist.dh_agg_stale"));

		// WHEN
		datum(sid, "2026-01-01T10:00:00Z", 1, 101);

		// THEN
		// @formatter:off
		then(stale())
			.as("Previous datum hour, the hour before (datum on the hour), own hour, next datum hour")
			.extracting("agg_kind", "ts")
			.containsExactly(
				  tuple("h", "2026-01-01T09:00Z")
				, tuple("h", "2026-01-01T10:00Z")
				, tuple("h", "2026-01-01T11:00Z")
			)
			;
		// @formatter:on
	}

	@Test
	public void stale_delete() {
		// GIVEN
		final int sid = stream("A");
		datum(sid, "2026-01-01T09:50:00Z", 1, 100);
		datum(sid, "2026-01-01T10:30:00Z", 1, 110);
		datum(sid, "2026-01-01T12:10:00Z", 1, 120);
		processAll();

		// WHEN
		jdbcTemplate.update(
				sql("DELETE FROM solarhist.dh_datum WHERE stream_id = ? AND ts = ?::TIMESTAMPTZ"), sid,
				"2026-01-01T10:30:00Z");

		// THEN
		// @formatter:off
		then(stale())
			.as("Previous datum hour, own hour, next datum hour")
			.extracting("agg_kind", "ts")
			.containsExactly(
				  tuple("h", "2026-01-01T09:00Z")
				, tuple("h", "2026-01-01T10:00Z")
				, tuple("h", "2026-01-01T12:00Z")
			)
			;

		processAll();
		then(agg("dh_agg_hourly", sid))
			.as("The hour with no datum left is deleted")
			.extracting("ts", "read_a")
			.containsExactly(
				  tuple("2026-01-01T09:00Z", "{{0,100,100}}")
				, tuple("2026-01-01T12:00Z", "{{20,100,120}}")
			)
			;
		// @formatter:on
	}

	@Test
	public void daily_timeZoneWithDaylightSavingEnd() {
		// GIVEN
		jdbcTemplate.update(
				sql("""
						INSERT INTO solarhist.dh_node_time_zone (node_id, time_zone) VALUES (?, 'Pacific/Auckland')
						"""),
				TEST_NODE_ID);
		final int sid = stream("A");
		// one datum at half past every hour, from 09:30 on 5 April to 13:30 on 6 April; the local
		// day of 6 April 2025 is from 11:00 on 5 April to 12:00 on 6 April, 25 hours
		final Instant start = Instant.parse("2025-04-05T09:30:00Z");
		for ( int h = 0; h <= 28; h++ ) {
			datum(sid, start.plus(h, ChronoUnit.HOURS).toString(), 1, 1000 + h * 10);
		}

		// WHEN
		processAll();

		// THEN
		// @formatter:off
		then(agg("dh_agg_daily", sid))
			.as("Days start at local midnight; the day daylight saving ends has 25 hours")
			.extracting("ts", "stat_i", "data_a", "read_a")
			.containsExactly(
				  tuple("2025-04-04T11:00Z", "{{2,1,1}}", "{15}", "{{10,1000,1010}}")
				, tuple("2025-04-05T11:00Z", "{{25,1,1}}", "{250}", "{{250,1010,1260}}")
				, tuple("2025-04-06T12:00Z", "{{2,1,1}}", "{15}", "{{20,1260,1280}}")
			)
			;
		then(agg("dh_agg_monthly", sid))
			.as("Months start at local midnight")
			.extracting("ts", "stat_i", "data_a", "read_a")
			.containsExactly(
				  tuple("2025-03-31T11:00Z", "{{29,1,1}}", "{280}", "{{280,1000,1280}}")
			)
			;
		// @formatter:on
	}

	@Test
	public void daily_timeZoneChange() {
		// GIVEN
		jdbcTemplate.update(
				sql("""
						INSERT INTO solarhist.dh_node_time_zone (node_id, time_zone) VALUES (?, 'Pacific/Auckland')
						"""),
				TEST_NODE_ID);
		final int sid = stream("A");
		datum(sid, "2026-01-01T20:00:00Z", 1, 100);
		datum(sid, "2026-01-01T20:30:00Z", 1, 110);
		processAll();

		// WHEN
		jdbcTemplate.update(
				sql("UPDATE solarhist.dh_node_time_zone SET time_zone = 'UTC' WHERE node_id = ?"),
				TEST_NODE_ID);
		processAll();

		// THEN
		// @formatter:off
		then(agg("dh_agg_daily", sid))
			.as("The day is computed again in the new zone, and the old one deleted")
			.extracting("ts", "read_a")
			.containsExactly(
				  tuple("2026-01-01T00:00Z", "{{10,100,110}}")
			)
			;
		then(agg("dh_agg_monthly", sid))
			.as("The month is computed again in the new zone, and the old one deleted")
			.extracting("ts", "read_a")
			.containsExactly(
				  tuple("2026-01-01T00:00Z", "{{10,100,110}}")
			)
			;
		// @formatter:on
	}

	@Test
	public void hourly_retentionGuard() {
		// GIVEN
		final int sid = stream("A");
		datum(sid, "2026-01-01T10:30:00Z", 1, 100);
		// an hour kept from a dropped partition, older than the oldest partition
		final String hour = jdbcTemplate.queryForObject(
				sql("""
						SELECT to_char((solarhist.dh_datum_oldest_partition_start() - INTERVAL 'PT1H') AT TIME ZONE 'UTC'
							, 'YYYY-MM-DD"T"HH24:MI"Z"')
						"""),
				String.class);
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_agg_hourly (stream_id, ts_start, data_a, read_a)
				VALUES (?, ?::TIMESTAMPTZ, '{1}', '{{1,1,2}}')
				"""), sid, hour);
		jdbcTemplate.update(sql("DELETE FROM solarhist.dh_agg_stale"));
		jdbcTemplate.update(
				sql("""
						INSERT INTO solarhist.dh_agg_stale (stream_id, ts_start, agg_kind) VALUES (?, ?::TIMESTAMPTZ, 'h')
						"""),
				sid, hour);

		// WHEN
		final int count = process("h");

		// THEN
		// @formatter:off
		then(count).as("The stale hour is taken off the queue").isOne();
		then(stale()).as("Nothing else is marked stale").isEmpty();
		then(agg("dh_agg_hourly", sid))
			.as("The kept hour is not computed again")
			.extracting("ts", "data_a", "read_a")
			.containsExactly(
				  tuple(hour, "{1}", "{{1,1,2}}")
			)
			;
		// @formatter:on
	}

	@Test
	public void readingDifference_raw() {
		// GIVEN
		final int sid = stream("A");
		datum(sid, "2026-01-01T05:00:00Z", 1, 150);
		datum(sid, "2026-01-01T05:30:00Z", 1, 155);
		datum(sid, "2026-01-01T06:00:00Z", 1, 160);

		// WHEN
		Map<String, Object> result = diff(sid, "2026-01-01T05:10:00Z", "2026-01-01T06:00:00Z");

		// THEN
		// @formatter:off
		then(result)
			.as("The reading at or before the start, and at the end")
			.containsEntry("ts_start", "2026-01-01T05:00Z")
			.containsEntry("ts_end", "2026-01-01T06:00Z")
			.containsEntry("read_a", "{{10,150,160}}")
			;
		// @formatter:on
	}

	@Test
	public void readingDifference_hourlyFallback() {
		// GIVEN
		final int sid = stream("A");
		// hourly rows kept from dropped raw datum, then raw datum
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_agg_hourly (stream_id, ts_start, data_a, read_a) VALUES
					  (?, '2026-01-01T00:00:00Z', '{10}', '{{10,100,110}}')
					, (?, '2026-01-01T01:00:00Z', '{10}', '{{10,110,120}}')
				"""), sid, sid);
		datum(sid, "2026-01-01T05:00:00Z", 1, 150);
		datum(sid, "2026-01-01T06:00:00Z", 1, 160);

		// WHEN
		Map<String, Object> afterHours = diff(sid, "2026-01-01T02:00:00Z", "2026-01-01T06:00:00Z");
		Map<String, Object> beforeHours = diff(sid, "2025-12-31T23:30:00Z", "2026-01-01T06:00:00Z");
		Map<String, Object> withinHours = diff(sid, "2025-12-31T23:30:00Z", "2026-01-01T02:00:00Z");

		// THEN
		// @formatter:off
		then(afterHours)
			.as("No raw datum at or before the start: the ending reading of the hour before")
			.containsEntry("ts_start", "2026-01-01T02:00Z")
			.containsEntry("ts_end", "2026-01-01T06:00Z")
			.containsEntry("read_a", "{{40,120,160}}")
			;
		then(beforeHours)
			.as("Nothing at or before the start: the starting reading of the first hour after")
			.containsEntry("ts_start", "2026-01-01T00:00Z")
			.containsEntry("ts_end", "2026-01-01T06:00Z")
			.containsEntry("read_a", "{{60,100,160}}")
			;
		then(withinHours)
			.as("Both ends from hourly rows")
			.containsEntry("ts_start", "2026-01-01T00:00Z")
			.containsEntry("ts_end", "2026-01-01T02:00Z")
			.containsEntry("read_a", "{{20,100,120}}")
			;
		// @formatter:on
	}

	@Test
	public void hourly_wayBackDatumWithoutAccumulating() {
		// GIVEN
		final int sid = wayBackStream();

		// WHEN
		processAll();

		// THEN
		// @formatter:off
		then(agg("dh_agg_hourly", sid))
			.as("The hour after the gap reads from 1 January, before the way-back datum (NET-538)")
			.filteredOn("ts", "2026-01-26T00:00Z")
			.extracting("read_a")
			.containsExactly("{{50,100,150}}")
			;
		// @formatter:on
	}

	@Test
	public void readingDifference_wayBackDatumWithoutAccumulating() {
		// GIVEN
		final int sid = wayBackStream();

		// WHEN
		Map<String, Object> result = diff(sid, "2026-01-26T00:15:00Z", "2026-01-26T00:30:00Z");

		// THEN
		// @formatter:off
		then(result)
			.as("The reading before the start is from 1 January, before the way-back datum (NET-538)")
			.containsEntry("ts_start", "2026-01-01T00:00Z")
			.containsEntry("ts_end", "2026-01-26T00:30Z")
			.containsEntry("read_a", "{{50,100,150}}")
			;
		// @formatter:on
	}

	@Test
	public void around_wayBackDatumWithoutAccumulating_notRequired() {
		// GIVEN
		final int sid = wayBackStream();

		// WHEN
		List<Map<String, Object>> result = jdbcTemplate.queryForList(sql("""
				SELECT to_char(ts AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"') AS ts
				FROM solarhist.dh_find_datum_around_ts(?, ?::TIMESTAMPTZ, 'P1M', FALSE, FALSE) d
				ORDER BY d.ts
				"""), sid, "2026-01-20T00:00:00Z");

		// THEN
		// @formatter:off
		then(result)
			.as("The datum before, 15 days earlier, has no accumulating value, but none is required (NET-538)")
			.extracting("ts")
			.containsExactly("2026-01-05T00:00Z", "2026-01-25T00:00Z")
			;
		// @formatter:on
	}

	@Test
	public void nullArguments() {
		// GIVEN
		final int sid = wayBackStream();

		// WHEN
		// the row count with a NULL in argument position n, or with none for n = 0
		List<Map<String, Object>> around = jdbcTemplate.queryForList(sql("""
				SELECT n, (SELECT count(*) FROM solarhist.dh_find_datum_around_ts(
					  CASE WHEN n = 1 THEN NULL ELSE ?::INTEGER END
					, CASE WHEN n = 2 THEN NULL ELSE '2026-01-26T00:15:00Z'::TIMESTAMPTZ END
					, CASE WHEN n = 3 THEN NULL ELSE 'P1Y'::INTERVAL END
					, CASE WHEN n = 4 THEN NULL ELSE TRUE END
					, CASE WHEN n = 5 THEN NULL ELSE FALSE END)) AS count
				FROM generate_series(0, 5) n
				ORDER BY n
				"""), sid);
		List<Map<String, Object>> slot = jdbcTemplate.queryForList(sql("""
				SELECT n, (SELECT count(*) FROM solarhist.dh_find_datum_for_time_slot(
					  CASE WHEN n = 1 THEN NULL ELSE ?::INTEGER END
					, CASE WHEN n = 2 THEN NULL ELSE '2026-01-26T00:00:00Z'::TIMESTAMPTZ END
					, CASE WHEN n = 3 THEN NULL ELSE '2026-01-26T01:00:00Z'::TIMESTAMPTZ END
					, CASE WHEN n = 4 THEN NULL ELSE 'P1Y'::INTERVAL END)) AS count
				FROM generate_series(0, 4) n
				ORDER BY n
				"""), sid);

		// THEN
		// @formatter:off
		then(around)
			.as("Any NULL argument gives no rows, as for a STRICT function")
			.extracting("count")
			.containsExactly(2L, 0L, 0L, 0L, 0L, 0L)
			;
		then(slot)
			.as("Any NULL argument gives no rows, as for a STRICT function")
			.extracting("count")
			.containsExactly(5L, 0L, 0L, 0L, 0L)
			;
		// @formatter:on
	}

}

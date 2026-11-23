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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.PreparedStatementCallback;

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
	 * Get the hourly reading for a time, from
	 * {@code dh_find_hourly_reading_near_ts()}.
	 */
	private List<Map<String, Object>> hourlyReading(int streamId, String ts, String tolerance,
			String nextTs) {
		return jdbcTemplate.queryForList(sql("""
				SELECT to_char(r.ts AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"') AS ts
					, r.data_a::TEXT AS data_a
				FROM solarhist.dh_find_hourly_reading_near_ts(?, ?::TIMESTAMPTZ, ?::INTERVAL, ?::TIMESTAMPTZ) r
				"""), streamId, ts, tolerance, nextTs);
	}

	/**
	 * Skip a test when the schema has an aggregate setup from before the
	 * hourly reading function.
	 */
	private void assumeHourlyReadingFunction() {
		final Boolean found = jdbcTemplate.queryForObject("SELECT to_regprocedure(?) IS NOT NULL",
				Boolean.class, SCHEMA + ".dh_find_hourly_reading_near_ts(integer"
						+ ",timestamp with time zone,interval,timestamp with time zone)");
		assumeTrue("The %s schema does not have the hourly reading function.".formatted(SCHEMA),
				Boolean.TRUE.equals(found));
	}

	/**
	 * Test if the hourly table is partitioned, which it is not in an aggregate
	 * setup from before the yearly partitions.
	 */
	private boolean hourlyPartitioned() {
		return "p".equals(jdbcTemplate.queryForObject(
				"SELECT relkind::TEXT FROM pg_class WHERE oid = ?::regclass", String.class,
				SCHEMA + ".dh_agg_hourly"));
	}

	/**
	 * Skip a test when the schema has an aggregate setup from before the yearly
	 * hourly partitions.
	 */
	private void assumeHourlyPartitioned() {
		assumeTrue("The %s schema's hourly table is not partitioned.".formatted(SCHEMA),
				hourlyPartitioned());
	}

	/**
	 * Create the yearly hourly partition that holds a time, if needed, before
	 * hourly rows are inserted directly.
	 */
	private void hourlyPartition(String ts) {
		if ( hourlyPartitioned() ) {
			jdbcTemplate.queryForList(
					sql("SELECT solarhist.dh_agg_hourly_ensure_partition(?::TIMESTAMPTZ)"), ts);
		}
	}

	/** Get the name, bounds and owner of each hourly partition. */
	private List<Map<String, Object>> hourlyPartitions() {
		return jdbcTemplate.queryForList("""
				SELECT c.relname::TEXT AS name, pg_get_expr(c.relpartbound, c.oid) AS bounds
					, pg_get_userbyid(c.relowner)::TEXT AS owner
				FROM pg_inherits i
				INNER JOIN pg_class c ON c.oid = i.inhrelid
				WHERE i.inhparent = ?::regclass
				ORDER BY c.relname
				""", SCHEMA + ".dh_agg_hourly");
	}

	/** Get the partition and hour of each hourly row of a stream. */
	private List<Map<String, Object>> hourlyRows(int streamId) {
		return jdbcTemplate.queryForList(sql("""
				SELECT (SELECT c.relname::TEXT FROM pg_class c WHERE c.oid = h.tableoid) AS part
					, to_char(h.ts_start AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"') AS ts
				FROM solarhist.dh_agg_hourly h
				WHERE h.stream_id = ?
				ORDER BY h.ts_start
				"""), streamId);
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
		hourlyPartition(hour);
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
		hourlyPartition("2026-01-01T00:00:00Z");
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
	public void readingDifference_hourlyOnly() {
		// GIVEN
		final int sid = stream("A");
		// hourly rows kept from dropped raw datum, and no raw datum at all
		hourlyPartition("2026-01-01T00:00:00Z");
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_agg_hourly (stream_id, ts_start, data_a, read_a) VALUES
					  (?, '2026-01-01T00:00:00Z', '{10}', '{{10,100,110}}')
					, (?, '2026-01-01T01:00:00Z', '{10}', '{{10,110,120}}')
					, (?, '2026-01-01T02:00:00Z', '{10}', '{{10,120,130}}')
				"""), sid, sid, sid);

		// WHEN
		Map<String, Object> result = diff(sid, "2025-12-31T23:30:00Z", "2026-01-01T02:30:00Z");

		// THEN
		// @formatter:off
		then(result)
			.as("""
				Without raw datum, the starting reading of the first hour after the start, and the \
				ending reading of the last complete hour before the end""")
			.containsEntry("ts_start", "2026-01-01T00:00Z")
			.containsEntry("ts_end", "2026-01-01T02:00Z")
			.containsEntry("read_a", "{{20,100,120}}")
			;
		// @formatter:on
	}

	@Test
	public void readingDifference_rawBeforeHourEnd() {
		// GIVEN
		final int sid = stream("A");
		hourlyPartition("2026-01-01T00:00:00Z");
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_agg_hourly (stream_id, ts_start, data_a, read_a) VALUES
					  (?, '2026-01-01T00:00:00Z', '{10}', '{{10,100,110}}')
				"""), sid);
		datum(sid, "2026-01-01T00:20:00Z", 1, 105);
		datum(sid, "2026-01-01T06:00:00Z", 1, 160);

		// WHEN
		Map<String, Object> result = diff(sid, "2025-12-31T23:30:00Z", "2026-01-01T06:00:00Z");

		// THEN
		// @formatter:off
		then(result)
			.as("""
				Nothing at or before the start, and the first hour after it ends after the first raw \
				datum after it: that raw datum""")
			.containsEntry("ts_start", "2026-01-01T00:20Z")
			.containsEntry("ts_end", "2026-01-01T06:00Z")
			.containsEntry("read_a", "{{55,105,160}}")
			;
		// @formatter:on
	}

	@Test
	public void hourlyReading_before() {
		assumeHourlyReadingFunction();

		// GIVEN
		final int sid = stream("A");
		hourlyPartition("2026-01-01T00:00:00Z");
		// two hours with readings, two without, and a later one with readings
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_agg_hourly (stream_id, ts_start, data_a, read_a) VALUES
					  (?, '2026-01-01T00:00:00Z', '{10}', '{{10,100,110}}')
					, (?, '2026-01-01T01:00:00Z', '{10}', '{{10,110,120}}')
					, (?, '2026-01-01T02:00:00Z', '{10}', NULL)
					, (?, '2026-01-01T03:00:00Z', '{10}', NULL)
					, (?, '2026-01-01T05:00:00Z', '{10}', '{{10,150,160}}')
				"""), sid, sid, sid, sid, sid);

		// WHEN
		final String none = "infinity";
		List<Map<String, Object>> atHourEnd = hourlyReading(sid, "2026-01-01T02:00:00Z", "P1Y", none);
		List<Map<String, Object>> inHour = hourlyReading(sid, "2026-01-01T01:59:00Z", "P1Y", none);
		List<Map<String, Object>> noReadings = hourlyReading(sid, "2026-01-01T04:30:00Z", "P1Y", none);
		List<Map<String, Object>> atTolerance = hourlyReading(sid, "2026-01-01T03:00:00Z", "PT1H", none);
		List<Map<String, Object>> pastTolerance = hourlyReading(sid, "2026-01-01T03:00:00Z", "PT59M",
				none);

		// THEN
		// @formatter:off
		then(atHourEnd)
			.as("The ending reading of the hour that ends at the time, at its end")
			.extracting("ts", "data_a")
			.containsExactly(tuple("2026-01-01T02:00Z", "{120}"))
			;
		then(inHour)
			.as("The hour the time is in is not complete: the hour before it")
			.extracting("ts", "data_a")
			.containsExactly(tuple("2026-01-01T01:00Z", "{110}"))
			;
		then(noReadings)
			.as("Hours without readings are skipped, and an hour before is chosen over one after")
			.extracting("ts", "data_a")
			.containsExactly(tuple("2026-01-01T02:00Z", "{120}"))
			;
		then(atTolerance)
			.as("An hour that ends the whole tolerance before the time is included")
			.extracting("ts", "data_a")
			.containsExactly(tuple("2026-01-01T02:00Z", "{120}"))
			;
		then(pastTolerance)
			.as("Nothing with readings within the tolerance, before or after")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void hourlyReading_after() {
		assumeHourlyReadingFunction();

		// GIVEN
		final int sid = stream("A");
		hourlyPartition("2026-01-01T00:00:00Z");
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_agg_hourly (stream_id, ts_start, data_a, read_a) VALUES
					  (?, '2026-01-01T00:00:00Z', '{10}', '{{10,100,110}}')
					, (?, '2026-01-01T01:00:00Z', '{10}', '{{10,110,120}}')
				"""), sid, sid);

		// WHEN
		final String ts = "2025-12-31T23:30:00Z";
		List<Map<String, Object>> first = hourlyReading(sid, ts, "P1Y", "infinity");
		List<Map<String, Object>> atHourStart = hourlyReading(sid, "2026-01-01T00:00:00Z", "P1Y",
				"infinity");
		List<Map<String, Object>> nextAtHourEnd = hourlyReading(sid, ts, "P1Y", "2026-01-01T01:00:00Z");
		List<Map<String, Object>> nextAfterHourEnd = hourlyReading(sid, ts, "P1Y",
				"2026-01-01T01:00:01Z");
		List<Map<String, Object>> atTolerance = hourlyReading(sid, ts, "PT30M", "infinity");
		List<Map<String, Object>> pastTolerance = hourlyReading(sid, ts, "PT29M", "infinity");

		// THEN
		// @formatter:off
		then(first)
			.as("No hour before: the starting reading of the first hour after, at its start")
			.extracting("ts", "data_a")
			.containsExactly(tuple("2026-01-01T00:00Z", "{100}"))
			;
		then(atHourStart)
			.as("An hour that starts at the time")
			.extracting("ts", "data_a")
			.containsExactly(tuple("2026-01-01T00:00Z", "{100}"))
			;
		then(nextAtHourEnd)
			.as("No hour that ends before the next raw datum")
			.isEmpty()
			;
		then(nextAfterHourEnd)
			.as("The hour ends before the next raw datum")
			.extracting("ts", "data_a")
			.containsExactly(tuple("2026-01-01T00:00Z", "{100}"))
			;
		then(atTolerance)
			.as("An hour that starts the whole tolerance after the time is included")
			.extracting("ts", "data_a")
			.containsExactly(tuple("2026-01-01T00:00Z", "{100}"))
			;
		then(pastTolerance)
			.as("No hour within the tolerance")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void hourlyReading_nullArguments() {
		assumeHourlyReadingFunction();

		// GIVEN
		final int sid = stream("A");
		hourlyPartition("2026-01-01T00:00:00Z");
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_agg_hourly (stream_id, ts_start, data_a, read_a) VALUES
					  (?, '2026-01-01T00:00:00Z', '{10}', '{{10,100,110}}')
				"""), sid);

		// WHEN
		// the row count with a NULL in argument position n, or with none for n = 0
		List<Map<String, Object>> result = jdbcTemplate.queryForList(sql("""
				SELECT n, (SELECT count(*) FROM solarhist.dh_find_hourly_reading_near_ts(
					  CASE WHEN n = 1 THEN NULL ELSE ?::INTEGER END
					, CASE WHEN n = 2 THEN NULL ELSE '2026-01-01T02:00:00Z'::TIMESTAMPTZ END
					, CASE WHEN n = 3 THEN NULL ELSE 'P1Y'::INTERVAL END
					, CASE WHEN n = 4 THEN NULL ELSE 'infinity'::TIMESTAMPTZ END)) AS count
				FROM generate_series(0, 4) n
				ORDER BY n
				"""), sid);

		// THEN
		// @formatter:off
		then(result)
			.as("Any NULL argument gives no rows; infinity, not NULL, means no next raw datum")
			.extracting("count")
			.containsExactly(1L, 0L, 0L, 0L, 0L)
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

	/*
	 * ==================================================================
	 * Hourly partitions
	 * ==================================================================
	 */

	private static final String BOUNDS_2031 = "FOR VALUES FROM ('2031-01-01 00:00:00+00') TO ('2032-01-01 00:00:00+00')";

	@Test
	public void partition_createdOnFirstUse() {
		assumeHourlyPartitioned();

		// GIVEN
		then(hourlyPartitions()).as("No 2031 partition to start with").extracting("name")
				.doesNotContain("dh_agg_hourly_2031");
		final int sid = stream("A");
		datum(sid, "2031-03-01T10:15:00Z", 1, 100);
		datum(sid, "2031-03-01T10:45:00Z", 1, 110);

		// WHEN
		process("h");

		// THEN
		// @formatter:off
		then(hourlyPartitions())
			.as("The partition of the hour's UTC year is created")
			.filteredOn("name", "dh_agg_hourly_2031")
			.extracting("bounds")
			.containsExactly(BOUNDS_2031)
			;
		then(hourlyRows(sid))
			.as("The hour is stored in it")
			.extracting("part", "ts")
			.containsExactly(
				  tuple("dh_agg_hourly_2031", "2031-03-01T10:00Z")
			)
			;
		// @formatter:on
	}

	/**
	 * A new partition is owned by the table's owner. The tests' role owns the
	 * table, so nothing more is done here; a partition created by another role,
	 * such as the superuser the {@code pg_cron} job runs as, cannot be tested
	 * without creating roles.
	 */
	@Test
	public void partition_ownedByTableOwner() {
		assumeHourlyPartitioned();

		// GIVEN
		then(hourlyPartitions()).as("No 2031 partition to start with").extracting("name")
				.doesNotContain("dh_agg_hourly_2031");
		final String tableOwner = jdbcTemplate.queryForObject(
				"SELECT pg_get_userbyid(relowner)::TEXT FROM pg_class WHERE oid = ?::regclass",
				String.class, SCHEMA + ".dh_agg_hourly");

		// WHEN
		final List<String> warnings = jdbcTemplate.execute(
				sql("SELECT solarhist.dh_agg_hourly_ensure_partition(?::TIMESTAMPTZ)"),
				(PreparedStatementCallback<List<String>>) stmt -> {
					stmt.setString(1, "2031-06-01T00:00:00Z");
					stmt.execute();
					final List<String> messages = new ArrayList<>();
					for ( SQLWarning w = stmt.getWarnings(); w != null; w = w.getNextWarning() ) {
						messages.add(w.getMessage());
					}
					return messages;
				});

		// THEN
		// @formatter:off
		then(hourlyPartitions())
			.as("The partition is owned by the table's owner")
			.filteredOn("name", "dh_agg_hourly_2031")
			.extracting("owner")
			.containsExactly(tableOwner)
			;
		then(warnings)
			.as("No warning that the partition has another owner")
			.isEmpty()
			;
		// @formatter:on
	}

	/**
	 * Get the tablespace of a relation, or {@code null} for the database
	 * default.
	 */
	private String tablespaceOf(String relation) {
		return jdbcTemplate.queryForObject("""
				SELECT s.spcname::TEXT
				FROM pg_class c
				LEFT OUTER JOIN pg_tablespace s ON s.oid = c.reltablespace
				WHERE c.oid = ?::regclass
				""", String.class, relation);
	}

	/**
	 * New partitions are created in the tablespace of their table, as
	 * {@code CREATE TABLE ... PARTITION OF} would do, for both the datum and
	 * the hourly table. Skipped unless the test database has a tablespace other
	 * than the default ones that the tests' role can create tables in.
	 */
	@Test
	public void partition_inTableTablespace() {
		assumeHourlyPartitioned();
		final List<String> spaces = jdbcTemplate.queryForList("""
				SELECT spcname::TEXT FROM pg_tablespace
				WHERE spcname NOT IN ('pg_default', 'pg_global')
					AND oid <> (SELECT dattablespace FROM pg_database WHERE datname = current_database())
					AND has_tablespace_privilege(spcname, 'CREATE')
				ORDER BY spcname
				LIMIT 1
				""", String.class);
		assumeTrue("No tablespace the tests' role can create tables in.", !spaces.isEmpty());
		final String space = spaces.get(0);

		// GIVEN
		then(hourlyPartitions()).as("No 2031 partition to start with").extracting("name")
				.doesNotContain("dh_agg_hourly_2031");
		// only sets the tablespace of new partitions: nothing is moved
		final String spaceIdent = jdbcTemplate.queryForObject("SELECT quote_ident(?)", String.class,
				space);
		jdbcTemplate.update(
				sql("ALTER TABLE ONLY solarhist.dh_datum SET TABLESPACE %s").formatted(spaceIdent));
		jdbcTemplate.update(
				sql("ALTER TABLE ONLY solarhist.dh_agg_hourly SET TABLESPACE %s").formatted(spaceIdent));

		// WHEN
		jdbcTemplate.queryForList(sql("SELECT solarhist.dh_datum_ensure_partition(?::TIMESTAMPTZ)"),
				"2031-06-01T00:00:00Z");
		hourlyPartition("2031-06-01T00:00:00Z");

		// THEN
		// @formatter:off
		then(tablespaceOf(SCHEMA + ".dh_datum_2031_06"))
			.as("The new datum partition is in the datum table's tablespace")
			.isEqualTo(space)
			;
		then(tablespaceOf(SCHEMA + ".dh_agg_hourly_2031"))
			.as("The new hourly partition is in the hourly table's tablespace")
			.isEqualTo(space)
			;
		// @formatter:on
	}

	/**
	 * Without a tablespace on the table, new partitions are created in the
	 * database default, as before.
	 */
	@Test
	public void partition_inDefaultTablespace() {
		assumeHourlyPartitioned();
		assumeTrue("The %s.dh_datum table has a tablespace.".formatted(SCHEMA),
				tablespaceOf(SCHEMA + ".dh_datum") == null);
		assumeTrue("The %s.dh_agg_hourly table has a tablespace.".formatted(SCHEMA),
				tablespaceOf(SCHEMA + ".dh_agg_hourly") == null);

		// GIVEN
		then(hourlyPartitions()).as("No 2031 partition to start with").extracting("name")
				.doesNotContain("dh_agg_hourly_2031");

		// WHEN
		jdbcTemplate.queryForList(sql("SELECT solarhist.dh_datum_ensure_partition(?::TIMESTAMPTZ)"),
				"2031-06-01T00:00:00Z");
		hourlyPartition("2031-06-01T00:00:00Z");

		// THEN
		// @formatter:off
		then(tablespaceOf(SCHEMA + ".dh_datum_2031_06"))
			.as("The new datum partition is in the database default tablespace")
			.isNull()
			;
		then(tablespaceOf(SCHEMA + ".dh_agg_hourly_2031"))
			.as("The new hourly partition is in the database default tablespace")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void partition_boundsInUtc() {
		assumeHourlyPartitioned();

		// GIVEN
		jdbcTemplate.update("SET LOCAL TimeZone = 'Pacific/Auckland'");

		// WHEN
		// 1 January 2031 at 05:00 in Auckland is still 2030 in UTC
		hourlyPartition("2031-01-01T05:00:00+13:00");
		hourlyPartition("2031-01-01T14:00:00+13:00");

		// THEN
		jdbcTemplate.update("SET LOCAL TimeZone = 'UTC'");
		// @formatter:off
		then(hourlyPartitions())
			.as("Partitions are by UTC year, whatever the session's zone")
			.filteredOn(p -> p.get("name").toString().matches("dh_agg_hourly_203[01]"))
			.extracting("name", "bounds")
			.containsExactly(
				  tuple("dh_agg_hourly_2030", "FOR VALUES FROM ('2030-01-01 00:00:00+00') TO ('2031-01-01 00:00:00+00')")
				, tuple("dh_agg_hourly_2031", BOUNDS_2031)
			)
			;
		// @formatter:on
	}

	@Test
	public void partition_secondYear() {
		assumeHourlyPartitioned();

		// GIVEN
		final int sid = stream("A");
		datum(sid, "2031-06-01T10:15:00Z", 1, 100);
		datum(sid, "2031-06-01T10:45:00Z", 1, 110);
		process("h");

		// WHEN
		datum(sid, "2032-06-01T10:15:00Z", 1, 200);
		datum(sid, "2032-06-01T10:45:00Z", 1, 210);
		process("h");

		// THEN
		// @formatter:off
		then(hourlyPartitions())
			.as("Each year has a partition of its own")
			.extracting("name")
			.contains("dh_agg_hourly_2031", "dh_agg_hourly_2032")
			;
		then(hourlyRows(sid))
			.as("Each hour is stored in the partition of its year")
			.extracting("part", "ts")
			.containsExactly(
				  tuple("dh_agg_hourly_2031", "2031-06-01T10:00Z")
				, tuple("dh_agg_hourly_2032", "2032-06-01T10:00Z")
			)
			;
		// @formatter:on
	}

	@Test
	public void partition_lateDatumForEarlierYear() {
		assumeHourlyPartitioned();

		// GIVEN
		final int sid = stream("A");
		datum(sid, "2032-01-10T10:15:00Z", 1, 200);
		datum(sid, "2032-01-10T10:45:00Z", 1, 210);
		process("h");
		then(hourlyPartitions()).as("No 2031 partition before the late datum").extracting("name")
				.doesNotContain("dh_agg_hourly_2031");

		// WHEN
		datum(sid, "2031-06-01T10:30:00Z", 1, 100);
		process("h");

		// THEN
		// @formatter:off
		then(hourlyPartitions())
			.as("The late datum's hour creates the partition of its year")
			.filteredOn("name", "dh_agg_hourly_2031")
			.extracting("bounds")
			.containsExactly(BOUNDS_2031)
			;
		then(hourlyRows(sid))
			.as("The late hour is stored in the earlier year's partition")
			.extracting("part", "ts")
			.containsExactly(
				  tuple("dh_agg_hourly_2031", "2031-06-01T10:00Z")
				, tuple("dh_agg_hourly_2032", "2032-01-10T10:00Z")
			)
			;
		// @formatter:on
	}

	@Test
	public void partition_notCreatedForSkippedHour() {
		assumeHourlyPartitioned();

		// GIVEN
		final int sid = stream("A");
		datum(sid, "2031-01-01T00:30:00Z", 1, 100);
		// an hour before the oldest raw partition, in a year without an hourly partition
		final String hour = jdbcTemplate.queryForObject(
				sql("""
						SELECT to_char((solarhist.dh_datum_oldest_partition_start() - INTERVAL 'PT1H') AT TIME ZONE 'UTC'
							, 'YYYY-MM-DD"T"HH24:MI"Z"')
						"""),
				String.class);
		final String part = "dh_agg_hourly_" + hour.substring(0, 4);
		then(hourlyPartitions()).as("No partition for the year of the hour").extracting("name")
				.doesNotContain(part);
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
		then(hourlyPartitions())
			.as("The hour skipped by the retention guard creates no partition")
			.extracting("name")
			.doesNotContain(part)
			;
		// @formatter:on
	}

	@Test
	public void daily_acrossYearBoundary() {
		assumeHourlyPartitioned();

		// GIVEN
		jdbcTemplate.update(
				sql("""
						INSERT INTO solarhist.dh_node_time_zone (node_id, time_zone) VALUES (?, 'Pacific/Auckland')
						"""),
				TEST_NODE_ID);
		final int sid = stream("A");
		// one datum at half past every hour, from 09:30 on 31 December 2031 to 12:30 on
		// 1 January 2032; the local day of 1 January 2032 is from 11:00 on 31 December to
		// 11:00 on 1 January, 13 hours in the 2031 partition and 11 in the 2032 one
		final Instant start = Instant.parse("2031-12-31T09:30:00Z");
		for ( int h = 0; h <= 27; h++ ) {
			datum(sid, start.plus(h, ChronoUnit.HOURS).toString(), 1, 1000 + h * 10);
		}

		// WHEN
		processAll();

		// THEN
		// @formatter:off
		then(hourlyRows(sid))
			.as("The hours are stored in the partitions of their UTC years")
			.extracting("part")
			.containsOnly("dh_agg_hourly_2031", "dh_agg_hourly_2032")
			.containsSequence("dh_agg_hourly_2031", "dh_agg_hourly_2032")
			.hasSize(28)
			;
		then(hourlyRows(sid))
			.as("15 hours in 2031, from 09:00 to 23:00")
			.filteredOn("part", "dh_agg_hourly_2031")
			.hasSize(15)
			;
		then(agg("dh_agg_daily", sid))
			.as("The local day across the year boundary rolls up the hours of both partitions")
			.extracting("ts", "stat_i", "data_a", "read_a")
			.containsExactly(
				  tuple("2031-12-30T11:00Z", "{{2,1,1}}", "{15}", "{{10,1000,1010}}")
				, tuple("2031-12-31T11:00Z", "{{24,1,1}}", "{240}", "{{240,1010,1250}}")
				, tuple("2032-01-01T11:00Z", "{{2,1,1}}", "{15}", "{{20,1250,1270}}")
			)
			;
		then(agg("dh_agg_monthly", sid))
			.as("The local months on each side of the year boundary")
			.extracting("ts", "stat_i", "data_a", "read_a")
			.containsExactly(
				  tuple("2031-11-30T11:00Z", "{{2,1,1}}", "{15}", "{{10,1000,1010}}")
				, tuple("2031-12-31T11:00Z", "{{26,1,1}}", "{255}", "{{260,1010,1270}}")
			)
			;
		// @formatter:on
	}

	/*
	 * The concurrent tests use connections of their own, outside the test
	 * transaction, so they commit and then drop the partition they create.
	 */

	private static final String FUTURE_TS = "2190-06-01T00:00:00Z";
	private static final String FUTURE_PART = "dh_agg_hourly_2190";

	private static void ensureHourlyPartition(Connection con, String ts) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(
				"SELECT %s.dh_agg_hourly_ensure_partition(?::TIMESTAMPTZ)".formatted(SCHEMA))) {
			stmt.setString(1, ts);
			stmt.execute();
		}
	}

	private static int backendPid(Connection con) throws SQLException {
		try (Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT pg_backend_pid()")) {
			rs.next();
			return rs.getInt(1);
		}
	}

	/** Count the committed hourly partitions with a given name. */
	private static int hourlyPartitionCount(Connection con, String name) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("""
				SELECT count(*)
				FROM pg_inherits i
				INNER JOIN pg_class c ON c.oid = i.inhrelid
				WHERE i.inhparent = ?::regclass AND c.relname = ?
				""")) {
			stmt.setString(1, SCHEMA + ".dh_agg_hourly");
			stmt.setString(2, name);
			try (ResultSet rs = stmt.executeQuery()) {
				rs.next();
				return rs.getInt(1);
			}
		}
	}

	/** Wait until a backend waits for a lock, for up to 10 seconds. */
	private static void awaitLockWait(Connection con, int pid) throws Exception {
		final long end = System.currentTimeMillis() + 10_000L;
		try (PreparedStatement stmt = con
				.prepareStatement("SELECT wait_event_type = 'Lock' FROM pg_stat_activity WHERE pid = ?")) {
			stmt.setInt(1, pid);
			while ( System.currentTimeMillis() < end ) {
				try (ResultSet rs = stmt.executeQuery()) {
					if ( rs.next() && rs.getBoolean(1) ) {
						return;
					}
				}
				Thread.sleep(20L);
			}
		}
		throw new AssertionError("Backend %d did not wait for a lock.".formatted(pid));
	}

	private static void dropFuturePartition() throws SQLException {
		try (Connection con = dataSource().getConnection(); Statement stmt = con.createStatement()) {
			stmt.execute("DROP TABLE IF EXISTS %s.%s".formatted(SCHEMA, FUTURE_PART));
		}
	}

	/**
	 * Create the partition in two transactions at once: the second waits for
	 * the first, and then finds the partition the first committed.
	 */
	@Test
	public void partition_concurrentFirstUse() throws Exception {
		assumeHourlyPartitioned();
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		try (Connection first = dataSource().getConnection();
				Connection second = dataSource().getConnection();
				Connection observer = dataSource().getConnection()) {
			// GIVEN
			then(hourlyPartitionCount(observer, FUTURE_PART)).as("No partition to start with").isZero();
			first.setAutoCommit(false);
			second.setAutoCommit(false);
			final int secondPid = backendPid(second);

			// WHEN
			ensureHourlyPartition(first, FUTURE_TS);
			final Future<?> secondCall = executor.submit(() -> {
				ensureHourlyPartition(second, FUTURE_TS);
				return null;
			});
			awaitLockWait(observer, secondPid);
			first.commit();
			secondCall.get(10L, TimeUnit.SECONDS);
			second.commit();

			// THEN
			then(hourlyPartitionCount(observer, FUTURE_PART))
					.as("The second caller finds the first one's partition, without an error").isOne();
		} finally {
			executor.shutdownNow();
			dropFuturePartition();
		}
	}

	/**
	 * Create the partition in two transactions at once, rolling back the first:
	 * the second waits for the first, and then creates the partition itself.
	 */
	@Test
	public void partition_concurrentFirstUse_firstRolledBack() throws Exception {
		assumeHourlyPartitioned();
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		try (Connection first = dataSource().getConnection();
				Connection second = dataSource().getConnection();
				Connection observer = dataSource().getConnection()) {
			// GIVEN
			then(hourlyPartitionCount(observer, FUTURE_PART)).as("No partition to start with").isZero();
			first.setAutoCommit(false);
			second.setAutoCommit(false);
			final int secondPid = backendPid(second);

			// WHEN
			ensureHourlyPartition(first, FUTURE_TS);
			final Future<?> secondCall = executor.submit(() -> {
				ensureHourlyPartition(second, FUTURE_TS);
				return null;
			});
			awaitLockWait(observer, secondPid);
			first.rollback();
			secondCall.get(10L, TimeUnit.SECONDS);
			final int beforeCommit = hourlyPartitionCount(observer, FUTURE_PART);
			second.commit();

			// THEN
			then(beforeCommit).as("The first caller's partition is rolled back").isZero();
			then(hourlyPartitionCount(observer, FUTURE_PART))
					.as("The second caller creates the partition after waiting").isOne();
		} finally {
			executor.shutdownNow();
			dropFuturePartition();
		}
	}

	/*
	 * ==================================================================
	 * Sub-hour slots
	 * ==================================================================
	 */

	/**
	 * Skip a test when the schema has an aggregate setup from before the
	 * sub-hour slot functions.
	 */
	private void assumeSlotFunctions() {
		final Boolean slots = jdbcTemplate.queryForObject(
				"SELECT to_regprocedure(?) IS NOT NULL AND to_regprocedure(?) IS NOT NULL",
				Boolean.class,
				SCHEMA + ".dh_rollup_datum_for_time_span_slots(integer"
						+ ",timestamp with time zone,timestamp with time zone,integer,interval)",
				SCHEMA + ".dh_count_datum_time_span_slots(integer"
						+ ",timestamp with time zone,timestamp with time zone,integer)");
		assumeTrue("The %s schema does not have the sub-hour slot functions.".formatted(SCHEMA),
				Boolean.TRUE.equals(slots));
	}

	/**
	 * Get the slot aggregates of a stream.
	 */
	private List<Map<String, Object>> slots(int streamId, String start, String end, int secs) {
		return jdbcTemplate.queryForList(sql("""
				SELECT to_char(ts_start AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"') AS ts
					, data_i::TEXT AS data_i, stat_i::TEXT AS stat_i
					, data_a::TEXT AS data_a, read_a::TEXT AS read_a
				FROM solarhist.dh_rollup_datum_for_time_span_slots(?, ?::TIMESTAMPTZ, ?::TIMESTAMPTZ, ?)
				ORDER BY ts_start
				"""), streamId, start, end, secs);
	}

	private long slotCount(int streamId, String start, String end, int secs) {
		return jdbcTemplate.queryForObject(sql("""
				SELECT solarhist.dh_count_datum_time_span_slots(?, ?::TIMESTAMPTZ, ?::TIMESTAMPTZ, ?)
				"""), Long.class, streamId, start, end, secs);
	}

	/**
	 * The datum of SolarNet's {@code test-datum-31.txt}: an hour of datum at
	 * irregular times, with a datum before and after it.
	 */
	private static final String[][] TEST_DATUM_31 = new String[][] {
			{ "2020-06-01T11:59:10Z", "1.0", "2.0", "100" },
			{ "2020-06-01T12:00:30Z", "1.1", "2.1", "105" },
			{ "2020-06-01T12:02:00Z", "1.2", "2.2", "110" },
			{ "2020-06-01T12:04:00Z", "1.3", "2.3", "115" },
			{ "2020-06-01T12:05:10Z", "1.4", "2.4", "120" },
			{ "2020-06-01T12:07:30Z", "1.5", "2.5", "125" },
			{ "2020-06-01T12:09:50Z", "1.6", "2.6", "130" },
			{ "2020-06-01T12:10:30Z", "1.7", "2.7", "135" },
			{ "2020-06-01T12:13:00Z", "1.8", "2.8", "140" },
			{ "2020-06-01T12:15:50Z", "1.9", "2.9", "145" },
			{ "2020-06-01T12:16:30Z", "1.0", "2.0", "150" },
			{ "2020-06-01T12:19:00Z", "1.1", "2.1", "155" },
			{ "2020-06-01T12:20:10Z", "1.2", "2.2", "160" },
			{ "2020-06-01T12:21:00Z", "1.3", "2.3", "165" },
			{ "2020-06-01T12:22:00Z", "1.4", "2.4", "170" },
			{ "2020-06-01T12:24:40Z", "1.5", "2.5", "175" },
			{ "2020-06-01T12:28:00Z", "1.6", "2.6", "180" },
			{ "2020-06-01T12:29:50Z", "1.7", "2.7", "185" },
			{ "2020-06-01T12:30:10Z", "1.8", "2.8", "190" },
			{ "2020-06-01T12:33:20Z", "1.9", "2.9", "195" },
			{ "2020-06-01T12:33:30Z", "1.0", "2.0", "200" },
			{ "2020-06-01T12:35:00Z", "1.1", "2.1", "205" },
			{ "2020-06-01T12:38:10Z", "1.2", "2.2", "210" },
			{ "2020-06-01T12:40:00Z", "1.3", "2.3", "215" },
			{ "2020-06-01T12:41:00Z", "1.4", "2.4", "220" },
			{ "2020-06-01T12:42:00Z", "1.5", "2.5", "225" },
			{ "2020-06-01T12:43:40Z", "1.6", "2.6", "230" },
			{ "2020-06-01T12:45:20Z", "1.7", "2.7", "235" },
			{ "2020-06-01T12:47:00Z", "1.8", "2.8", "240" },
			{ "2020-06-01T12:48:50Z", "1.9", "2.9", "245" },
			{ "2020-06-01T12:49:20Z", "1.0", "2.0", "250" },
			{ "2020-06-01T12:51:00Z", "1.1", "2.1", "255" },
			{ "2020-06-01T12:52:10Z", "1.2", "2.2", "260" },
			{ "2020-06-01T12:53:00Z", "1.3", "2.3", "265" },
			{ "2020-06-01T12:54:30Z", "1.4", "2.4", "270" },
			{ "2020-06-01T12:55:00Z", "1.5", "2.5", "275" },
			{ "2020-06-01T12:56:40Z", "1.6", "2.6", "280" },
			{ "2020-06-01T12:57:20Z", "1.7", "2.7", "285" },
			{ "2020-06-01T12:58:50Z", "1.8", "2.8", "290" },
			{ "2020-06-01T12:59:30Z", "1.9", "2.9", "295" },
			{ "2020-06-01T13:02:20Z", "2.0", "3.0", "300" } };

	@Test
	public void slots_imperfectHour_10min() {
		assumeSlotFunctions();

		// GIVEN
		final int sid = jdbcTemplate.queryForObject(sql("""
				INSERT INTO solarhist.dh_datum_meta (node_id, source_id, names_i, names_a, type_ids)
				VALUES (?, 'a', '{x,y}', '{w}', '{1}')
				RETURNING stream_id
				"""), Integer.class, TEST_NODE_ID);
		jdbcTemplate.queryForList(sql("SELECT solarhist.dh_datum_ensure_partition(?::TIMESTAMPTZ)"),
				"2020-06-01T12:00:00Z");
		for ( String[] d : TEST_DATUM_31 ) {
			jdbcTemplate.update(sql("""
					INSERT INTO solarhist.dh_datum (ts, stream_id, type_id, data_i, data_a)
					VALUES (?::TIMESTAMPTZ, ?, 1, ARRAY[?::NUMERIC, ?::NUMERIC], ARRAY[?::NUMERIC])
					"""), d[0], sid, d[1], d[2], d[3]);
		}

		// WHEN
		List<Map<String, Object>> result = slots(sid, "2020-06-01T12:00:00Z", "2020-06-01T13:00:00Z",
				600);

		// THEN
		// @formatter:off
		then(result)
			.as("The values SolarNet's DbDatumRollupSlotsTests.imperfectHour_10min() expects")
			.extracting("ts", "data_i", "stat_i", "data_a", "read_a")
			.containsExactly(
				  tuple("2020-06-01T12:00Z", "{1.35,2.35}", "{{6,1.1,1.6},{6,2.1,2.6}}", "{28.125}", null)
				, tuple("2020-06-01T12:10Z", "{1.5,2.5}", "{{5,1.0,1.9},{5,2.0,2.9}}", "{28.0357142857142857143}", null)
				, tuple("2020-06-01T12:20Z", "{1.45,2.45}", "{{6,1.2,1.7},{6,2.2,2.7}}", "{28.2142857142857142857}", null)
				, tuple("2020-06-01T12:30Z", "{1.4,2.4}", "{{5,1.0,1.9},{5,2.0,2.9}}", "{27.5}", null)
				, tuple("2020-06-01T12:40Z", "{1.525,2.525}", "{{8,1.0,1.9},{8,2.0,2.9}}", "{37}", null)
				, tuple("2020-06-01T12:50Z", "{1.5,2.5}", "{{9,1.1,1.9},{9,2.1,2.9}}", "{43.88235294117647058825}", null)
			)
			;
		then(slotCount(sid, "2020-06-01T12:00:00Z", "2020-06-01T13:00:00Z", 600))
			.as("Slot count")
			.isEqualTo(6L)
			;
		// @formatter:on
	}

	@Test
	public void slots_gapAcrossSlots() {
		assumeSlotFunctions();

		// GIVEN
		final int sid = stream("A");
		datum(sid, "2025-01-01T10:05:00Z", 1, 100);
		datum(sid, "2025-01-01T10:25:00Z", 2, 120);
		datum(sid, "2025-01-01T13:15:00Z", 3, 290);
		datum(sid, "2025-01-01T13:45:00Z", 4, 320);

		// WHEN
		List<Map<String, Object>> result = slots(sid, "2025-01-01T10:00:00Z", "2025-01-01T14:00:00Z",
				1800);

		// THEN
		// @formatter:off
		then(result)
			.as("Only slots with datum; the difference across the gap split at the start of the later slot, not ignored")
			.extracting("ts", "data_i", "stat_i", "data_a", "read_a")
			.containsExactly(
				  tuple("2025-01-01T10:00Z", "{1.5}", "{{2,1,2}}", "{175.0000000000000000006}", null)
				, tuple("2025-01-01T13:00Z", "{3}", "{{1,3,3}}", "{29.9999999999999999994}", null)
				, tuple("2025-01-01T13:30Z", "{4}", "{{1,4,4}}", "{15}", null)
			)
			;
		then(slotCount(sid, "2025-01-01T10:00:00Z", "2025-01-01T14:00:00Z", 1800))
			.as("Slot count")
			.isEqualTo(3L)
			;
		// @formatter:on
	}

	@Test
	public void slots_spanNotAligned() {
		assumeSlotFunctions();

		// GIVEN
		final int sid = stream("A");
		datum(sid, "2025-01-01T10:00:00Z", 1, 100);
		datum(sid, "2025-01-01T10:06:00Z", 2, 106);
		datum(sid, "2025-01-01T10:12:00Z", 3, 112);
		datum(sid, "2025-01-01T10:18:00Z", 4, 118);

		// WHEN
		List<Map<String, Object>> result = slots(sid, "2025-01-01T10:05:00Z", "2025-01-01T10:15:00Z",
				600);

		// THEN
		// @formatter:off
		then(result)
			.as("""
				Slots of the datum within the span, starting on slot boundaries; as in SolarNet, the \
				datum before and after the span found for the clock accumulation count in the \
				statistics of the slots they are in, and the accumulation covers whole slots""")
			.extracting("ts", "data_i", "stat_i", "data_a")
			.containsExactly(
				  tuple("2025-01-01T10:00Z", "{1.5}", "{{2,1,2}}", "{10.00000000000000000002}")
				, tuple("2025-01-01T10:10Z", "{3.5}", "{{2,3,4}}", "{7.99999999999999999998}")
			)
			;
		// @formatter:on
	}

	@Test
	public void slotCount_lastSecondOfSlot() {
		assumeSlotFunctions();

		// GIVEN
		final int sid = stream("A");
		datum(sid, "2025-01-01T10:09:59.5Z", 1, 100);
		datum(sid, "2025-01-01T10:10:30Z", 2, 101);

		// WHEN
		List<Map<String, Object>> result = slots(sid, "2025-01-01T10:00:00Z", "2025-01-01T11:00:00Z",
				600);
		long count = slotCount(sid, "2025-01-01T10:00:00Z", "2025-01-01T11:00:00Z", 600);

		// THEN
		// @formatter:off
		then(result)
			.as("Each datum in its own slot")
			.extracting("ts")
			.containsExactly("2025-01-01T10:00Z", "2025-01-01T10:10Z")
			;
		then(count)
			.as("The count rounds a time up to a whole second, so counts the first datum in the next slot, as SolarNet does")
			.isEqualTo(1L)
			;
		// @formatter:on
	}

	@Test
	public void slotSeconds() {
		assumeSlotFunctions();

		// WHEN
		List<Map<String, Object>> result = jdbcTemplate.queryForList(sql("""
				SELECT s, solarhist.dh_slot_seconds(s) AS secs
				FROM unnest(ARRAY[0, 59, 60, 300, 420, 600, 900, 1200, 1800, 3600]) s
				ORDER BY s
				"""));

		// THEN
		// @formatter:off
		then(result)
			.as("Slot lengths between 60 and 1800 that divide 1800, otherwise 600")
			.extracting("secs")
			.containsExactly(600, 600, 60, 300, 600, 600, 900, 600, 1800, 600)
			;
		// @formatter:on
	}

	@Test
	public void slots_nullArguments() {
		assumeSlotFunctions();

		// GIVEN
		final int sid = stream("A");
		datum(sid, "2025-01-01T10:05:00Z", 1, 100);

		// WHEN
		List<Map<String, Object>> result = jdbcTemplate.queryForList(sql("""
				SELECT n, (SELECT count(*) FROM solarhist.dh_rollup_datum_for_time_span_slots(
					  CASE WHEN n = 1 THEN NULL ELSE ?::INTEGER END
					, CASE WHEN n = 2 THEN NULL ELSE '2025-01-01T10:00:00Z'::TIMESTAMPTZ END
					, CASE WHEN n = 3 THEN NULL ELSE '2025-01-01T11:00:00Z'::TIMESTAMPTZ END
					, 600
					, CASE WHEN n = 4 THEN NULL ELSE 'PT1H'::INTERVAL END)) AS count
				FROM generate_series(0, 4) n
				ORDER BY n
				"""), sid);

		// THEN
		// @formatter:off
		then(result)
			.as("A NULL stream, start, end or tolerance gives no rows")
			.extracting("count")
			.containsExactly(1L, 0L, 0L, 0L, 0L)
			;
		// @formatter:on
	}

}

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

import static net.solarnetwork.node.test.NodeTestUtils.utf8StringResource;
import static org.assertj.core.api.BDDAssertions.catchThrowable;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

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
	 * Get a tablespace other than the database default that the tests' role can
	 * create tables in, skipping the test when there is none.
	 */
	private String assumeTablespace() {
		final List<String> spaces = jdbcTemplate.queryForList("""
				SELECT spcname::TEXT FROM pg_tablespace
				WHERE spcname NOT IN ('pg_default', 'pg_global')
					AND oid <> (SELECT dattablespace FROM pg_database WHERE datname = current_database())
					AND has_tablespace_privilege(spcname, 'CREATE')
				ORDER BY spcname
				LIMIT 1
				""", String.class);
		assumeTrue("No tablespace the tests' role can create tables in.", !spaces.isEmpty());
		return spaces.get(0);
	}

	/**
	 * Set the tablespace of a table, which only sets the tablespace of its new
	 * partitions: nothing is moved.
	 */
	private void tableTablespace(String table, String space) {
		final String spaceIdent = jdbcTemplate.queryForObject("SELECT quote_ident(?)", String.class,
				space);
		final String alter = "ALTER TABLE ONLY %s.%s SET TABLESPACE %s";
		jdbcTemplate.update(alter.formatted(SCHEMA, table, spaceIdent));
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
	 * NET-539: the datum before a slot only when it is needed
	 * ==================================================================
	 */

	/**
	 * Create a stream with {@link #stream(String)} and store datum of
	 * {@code [ts, i, val]}, with an empty {@code val} for no accumulating
	 * value.
	 */
	private int stream(String sourceId, String[][] datum) {
		final int sid = stream(sourceId);
		for ( String[] d : datum ) {
			datum(sid, d[0], Integer.parseInt(d[1]), d[2].isEmpty() ? null : Integer.valueOf(d[2]));
		}
		return sid;
	}

	/**
	 * The times of the datum {@code dh_find_datum_for_time_slot()} returns for
	 * a slot, with the hourly rollup's read tolerance.
	 */
	private List<String> slotDatum(int streamId, String start, String end) {
		return jdbcTemplate.queryForList(sql("""
				SELECT to_char(d.ts AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"') AS ts
				FROM solarhist.dh_find_datum_for_time_slot(?, ?::TIMESTAMPTZ, ?::TIMESTAMPTZ, 'P1Y') d
				ORDER BY d.ts
				"""), String.class, streamId, start, end);
	}

	/** Compute an hour with {@code dh_rollup_datum_for_time_span()}. */
	private List<Map<String, Object>> rollupHour(int streamId, String hour) {
		return jdbcTemplate.queryForList(sql("""
				SELECT data_i::TEXT AS data_i, stat_i::TEXT AS stat_i
					, data_a::TEXT AS data_a, read_a::TEXT AS read_a
				FROM solarhist.dh_rollup_datum_for_time_span(?, ?::TIMESTAMPTZ, ?::TIMESTAMPTZ + INTERVAL 'PT1H')
				"""), streamId, hour, hour);
	}

	/**
	 * The datum of SolarNet's {@code sample-raw-data-12-net539.csv}: the last
	 * datum with an accumulating value is 2 days before the hour of 18:00 on 6
	 * February 2023, and the datum since then have none.
	 */
	private static final String[][] NET539_12 = new String[][] {
			{ "2023-02-04T10:00:00Z", "1", "100" },
			{ "2023-02-05T10:00:00Z", "2", "" },
			{ "2023-02-06T17:50:00Z", "3", "" },
			{ "2023-02-06T18:10:00Z", "4", "" },
			{ "2023-02-06T18:40:00Z", "5", "" },
			{ "2023-02-06T20:30:00Z", "6", "" } };

	/**
	 * The datum of SolarNet's {@code sample-raw-data-13-net539.csv}: as
	 * {@link #NET539_12}, but the datum exactly at the end of the hour has an
	 * accumulating value.
	 */
	private static final String[][] NET539_13 = new String[][] {
			{ "2023-02-04T10:00:00Z", "1", "100" },
			{ "2023-02-05T10:00:00Z", "2", "" },
			{ "2023-02-06T17:50:00Z", "3", "" },
			{ "2023-02-06T18:10:00Z", "4", "" },
			{ "2023-02-06T18:40:00Z", "5", "" },
			{ "2023-02-06T19:00:00Z", "6", "110" } };

	/**
	 * The datum of SolarNet's {@code sample-raw-data-14-net539.csv}: as
	 * {@link #NET539_12}, but the next datum after the hour has an
	 * accumulating value, within an hour of the last datum in the hour.
	 */
	private static final String[][] NET539_14 = new String[][] {
			{ "2023-02-04T10:00:00Z", "1", "100" },
			{ "2023-02-05T10:00:00Z", "2", "" },
			{ "2023-02-06T17:50:00Z", "3", "" },
			{ "2023-02-06T18:10:00Z", "4", "" },
			{ "2023-02-06T18:40:00Z", "5", "" },
			{ "2023-02-06T19:20:00Z", "6", "106" } };

	/**
	 * The datum of SolarNet's {@code sample-raw-data-15-net539.csv}: no datum
	 * from 18:00 to 18:20 has an accumulating value, and the datum before and
	 * after that range have.
	 */
	private static final String[][] NET539_15 = new String[][] {
			{ "2023-02-06T17:45:00Z", "1", "100" },
			{ "2023-02-06T18:05:00Z", "2", "" },
			{ "2023-02-06T18:17:00Z", "3", "" },
			{ "2023-02-06T18:25:00Z", "4", "108" } };

	/**
	 * The datum of SolarNet's {@code sample-raw-data-16-net539.csv}: the hour
	 * has accumulating values; no datum within the 14 days before it has, nor
	 * the latest datum before those 14 days, but one 5 days before that does.
	 */
	private static final String[][] NET539_16 = new String[][] {
			{ "2023-01-17T18:00:00Z", "1", "100" },
			{ "2023-01-22T18:00:00Z", "2", "" },
			{ "2023-01-27T18:00:00Z", "3", "" },
			{ "2023-02-05T18:00:00Z", "4", "" },
			{ "2023-02-06T18:10:00Z", "5", "110" },
			{ "2023-02-06T18:40:00Z", "6", "115" } };

	private static final String NET539_HOUR = "2023-02-06T18:00:00Z";

	private static final String NET539_HOUR_END = "2023-02-06T19:00:00Z";

	@Test
	public void slot_noAccumulatingInSlot_onlySlotDatum() {
		// GIVEN
		final int sid = stream("A", NET539_12);

		// WHEN
		List<String> result = slotDatum(sid, NET539_HOUR, NET539_HOUR_END);

		// THEN
		// @formatter:off
		then(result)
			.as("""
				Only the datum in the hour: nothing in it can be differenced against the datum \
				before, so neither that nor the datum after it are returned (NET-539)""")
			.containsExactly("2023-02-06T18:10Z", "2023-02-06T18:40Z")
			;
		// @formatter:on
	}

	@Test
	public void hourly_noAccumulatingInSlot_noAccumulating() {
		// GIVEN
		final int sid = stream("A", NET539_12);

		// WHEN
		List<Map<String, Object>> result = rollupHour(sid, NET539_HOUR);

		// THEN
		// @formatter:off
		then(result)
			.as("""
				No accumulation or reading, rather than a zero difference to the reading of the \
				datum before carried forward (NET-539)""")
			.extracting("data_i", "stat_i", "data_a", "read_a")
			.containsExactly(tuple("{4.5}", "{{2,4,5}}", null, null))
			;
		// @formatter:on
	}

	@Test
	public void hourly_noAccumulatingInSlot_stalePipeline() {
		// GIVEN
		final int sid = stream("A", NET539_12);

		// WHEN
		processAll();

		// THEN
		// @formatter:off
		then(agg("dh_agg_hourly", sid))
			.as("""
				Only the hour of the datum with an accumulating value has one; the hours after it \
				have none, rather than a zero difference to its reading carried forward; and the \
				hour before 10:00 on 5 February, whose only datum is at its end, has no row at all \
				(NET-539)""")
			.extracting("ts", "data_i", "data_a", "read_a")
			.containsExactly(
				  tuple("2023-02-04T10:00Z", "{1}", "{0}", "{{0,100,100}}")
				, tuple("2023-02-05T10:00Z", "{2}", null, null)
				, tuple("2023-02-06T17:00Z", "{3}", null, null)
				, tuple("2023-02-06T18:00Z", "{4.5}", null, null)
				, tuple("2023-02-06T20:00Z", "{6}", null, null)
			)
			;
		// @formatter:on
	}

	@Test
	public void hourly_accumulatingAtSlotEnd_keepsPriorDatum() {
		// GIVEN
		final int sid = stream("A", NET539_13);

		// WHEN
		List<Map<String, Object>> result = rollupHour(sid, NET539_HOUR);

		// THEN
		// @formatter:off
		then(result)
			.as("""
				The datum at the end of the hour is differenced against the datum before (110 - \
				100), see NET-469; no clock accumulation, as they are more than an hour apart""")
			.extracting("data_i", "stat_i", "data_a", "read_a")
			.containsExactly(tuple("{4.5}", "{{2,4,5}}", "{0}", "{{10,100,110}}"))
			;
		// @formatter:on
	}

	@Test
	public void hourly_nextAccumulatingAfterSlot_keepsPriorDatum() {
		// GIVEN
		final int sid = stream("A", NET539_14);

		// WHEN
		List<String> rows = slotDatum(sid, NET539_HOUR, NET539_HOUR_END);
		List<Map<String, Object>> result = rollupHour(sid, NET539_HOUR);

		// THEN
		// @formatter:off
		then(rows)
			.as("The datum before and after, as the clock accumulation between them crosses the hour")
			.containsExactly("2023-02-04T10:00Z", "2023-02-05T10:00Z", "2023-02-06T17:50Z",
					"2023-02-06T18:10Z", "2023-02-06T18:40Z", "2023-02-06T19:20Z")
			;
		then(result)
			.as("""
				The reading of the datum before, with no difference; no clock accumulation, as the \
				datum before and after are more than an hour apart""")
			.extracting("data_i", "data_a", "read_a")
			.containsExactly(tuple("{4.5}", "{0}", "{{0,100,100}}"))
			;
		// @formatter:on
	}

	@Test
	public void slots_priorAndNextStraddleRange_keepsPriorDatum() {
		assumeSlotFunctions();

		// GIVEN
		final int sid = stream("A", NET539_15);

		// WHEN
		List<Map<String, Object>> result = slots(sid, NET539_HOUR, "2023-02-06T18:20:00Z", 900);

		// THEN
		// @formatter:off
		then(result)
			.as("""
				The second slot has the hour's whole share of the accumulation from the datum \
				before, as it holds the first datum of the hour with an accumulating value, as in \
				SolarNet: (108 - 100) * (18:25 - 18:00) / (18:25 - 17:45)""")
			.extracting("ts", "data_a")
			.containsExactly(
				  tuple("2023-02-06T18:00Z", "{0}")
				, tuple("2023-02-06T18:15Z", "{5}")
			)
			;
		// @formatter:on
	}

	@Test
	public void slots_noAccumulatingInSpan_priorDatumNotAveraged() {
		assumeSlotFunctions();

		// GIVEN
		final int sid = stream("A", new String[][] {
				{ "2025-01-01T10:01:00Z", "1", "100" },
				{ "2025-01-01T10:07:00Z", "2", "" },
				{ "2025-01-01T10:12:00Z", "3", "" } });

		// WHEN
		List<Map<String, Object>> result = slots(sid, "2025-01-01T10:05:00Z", "2025-01-01T10:15:00Z",
				600);

		// THEN
		// @formatter:off
		then(result)
			.as("""
				The slot of the datum before the span starts before the span, so is not returned, \
				and that datum is counted in the statistics of no slot returned (NET-539). The slot \
				returned has no accumulation: the property's only value in the hour is in the slot \
				before, as for the 15 minute rows""")
			.extracting("ts", "data_i", "stat_i", "data_a")
			.containsExactly(
				  tuple("2025-01-01T10:10Z", "{3}", "{{1,3,3}}", null)
			)
			;
		// @formatter:on
	}

	@Test
	public void hourly_net538_wayBackDatumWithoutAccumulating_usesPriorDatumBeforeIt() {
		// GIVEN
		final int sid = stream("A", NET539_16);

		// WHEN
		List<Map<String, Object>> result = rollupHour(sid, NET539_HOUR);

		// THEN
		// @formatter:off
		then(result)
			.as("""
				Accumulation within the hour (115 - 110); the reading from the datum before the \
				way-back datum (115 - 100), see NET-538""")
			.extracting("data_i", "stat_i", "data_a", "read_a")
			.containsExactly(tuple("{5.5}", "{{2,5,6}}", "{5}", "{{15,100,115}}"))
			;
		// @formatter:on
	}

	/*
	 * ==================================================================
	 * NET-544: day and month readings from the first and last row with them
	 * ==================================================================
	 */

	/**
	 * Create a stream with one instantaneous property {@code i} and two
	 * accumulating properties {@code a} and {@code b}.
	 */
	private int stream2(String sourceId) {
		return jdbcTemplate.queryForObject(sql("""
				INSERT INTO solarhist.dh_datum_meta (node_id, source_id, names_i, names_a, type_ids)
				VALUES (?, ?, '{i}', '{a,b}', '{1}')
				RETURNING stream_id
				"""), Integer.class, TEST_NODE_ID, sourceId);
	}

	/**
	 * Store a datum of a {@link #stream2(String)} stream, dropping trailing
	 * NULL accumulating values as {@code dh_store_datum()} does.
	 */
	private void datum2(int streamId, String ts, int i, Integer a, Integer b) {
		jdbcTemplate.queryForList(sql("SELECT solarhist.dh_datum_ensure_partition(?::TIMESTAMPTZ)"), ts);
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_datum (ts, stream_id, type_id, data_i, data_a)
				VALUES (?::TIMESTAMPTZ, ?, 1, ARRAY[?]::NUMERIC[], CASE
					WHEN ?::NUMERIC IS NOT NULL THEN ARRAY[?, ?]::NUMERIC[]
					WHEN ?::NUMERIC IS NOT NULL THEN ARRAY[?]::NUMERIC[]
					END)
				"""), ts, streamId, i, b, a, b, a, a);
	}

	/**
	 * Compute a day or month with {@code dh_rollup_agg_data_for_time_span()}.
	 */
	private List<Map<String, Object>> rollupAgg(int streamId, String start, String end, String kind) {
		return jdbcTemplate.queryForList(sql("""
				SELECT data_a::TEXT AS data_a, read_a::TEXT AS read_a
				FROM solarhist.dh_rollup_agg_data_for_time_span(?, ?::TIMESTAMPTZ, ?::TIMESTAMPTZ, ?)
				"""), streamId, start, end, kind);
	}

	@Test
	public void daily_readingsFromFirstAndLastHourWithThem() {
		// GIVEN
		final int sid = stream2("A");
		// b is not in the first and last hours' datum: an accumulation of 0 and no readings
		hourlyPartition("2026-01-01T00:00:00Z");
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_agg_hourly (stream_id, ts_start, data_a, read_a) VALUES
					  (?, '2026-01-01T00:00:00Z', '{10,0}', '{{10,100,110},{0,NULL,NULL}}')
					, (?, '2026-01-01T01:00:00Z', '{10,5}', '{{10,110,120},{5,500,505}}')
					, (?, '2026-01-01T02:00:00Z', '{10,5}', '{{10,120,130},{5,505,510}}')
					, (?, '2026-01-01T03:00:00Z', '{10,0}', '{{10,130,140},{0,NULL,NULL}}')
				"""), sid, sid, sid, sid);

		// WHEN
		List<Map<String, Object>> result = rollupAgg(sid, "2026-01-01T00:00:00Z",
				"2026-01-02T00:00:00Z", "h");

		// THEN
		// @formatter:off
		then(result)
			.as("""
				The start and end readings of b are those of the first and last hour that has \
				them, not NULL (NET-544)""")
			.extracting("data_a", "read_a")
			.containsExactly(tuple("{40,10}", "{{40,100,140},{10,500,510}}"))
			;
		// @formatter:on
	}

	@Test
	public void daily_readingsPreferRowsWithAccumulation() {
		// GIVEN
		final int sid = stream2("A");
		// the first and last hours have readings of b but no accumulation of it
		hourlyPartition("2026-01-01T00:00:00Z");
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_agg_hourly (stream_id, ts_start, data_a, read_a) VALUES
					  (?, '2026-01-01T00:00:00Z', '{10,NULL}', '{{10,100,110},{NULL,400,400}}')
					, (?, '2026-01-01T01:00:00Z', '{10,5}', '{{10,110,120},{5,500,505}}')
					, (?, '2026-01-01T02:00:00Z', '{10,NULL}', '{{10,120,130},{NULL,600,600}}')
				"""), sid, sid, sid);

		// WHEN
		List<Map<String, Object>> result = rollupAgg(sid, "2026-01-01T00:00:00Z",
				"2026-01-02T00:00:00Z", "h");

		// THEN
		// @formatter:off
		then(result)
			.as("Rows with an accumulation are taken first for the readings, as before NET-544")
			.extracting("data_a", "read_a")
			.containsExactly(tuple("{30,5}", "{{30,100,130},{5,500,505}}"))
			;
		// @formatter:on
	}

	@Test
	public void dailyMonthly_propertyNotInFirstHourOrDay() {
		// GIVEN
		final int sid = stream2("A");
		// 1 February has no b at all; on 2 February b is in the datum of 01:00 only
		datum2(sid, "2026-02-01T00:10:00Z", 1, 100, null);
		datum2(sid, "2026-02-01T00:40:00Z", 1, 110, null);
		datum2(sid, "2026-02-02T00:10:00Z", 1, 200, null);
		datum2(sid, "2026-02-02T01:10:00Z", 1, 210, 500);
		datum2(sid, "2026-02-02T01:40:00Z", 1, 220, 505);
		datum2(sid, "2026-02-02T02:10:00Z", 1, 230, null);

		// WHEN
		processAll();

		// THEN
		// @formatter:off
		then(agg("dh_agg_hourly", sid))
			.as("Hours without b have an accumulation of 0 for it and no readings")
			.extracting("ts", "read_a")
			.containsExactly(
				  tuple("2026-02-01T00:00Z", "{{10,100,110},{0,NULL,NULL}}")
				, tuple("2026-02-02T00:00Z", "{{90,110,200},{0,NULL,NULL}}")
				, tuple("2026-02-02T01:00Z", "{{20,200,220},{5,500,505}}")
				, tuple("2026-02-02T02:00Z", "{{10,220,230},{0,505,505}}")
			)
			;
		then(agg("dh_agg_daily", sid))
			.as("The day of 2 February starts with the reading of b of 01:00 (NET-544)")
			.extracting("ts", "read_a")
			.containsExactly(
				  tuple("2026-02-01T00:00Z", "{{10,100,110},{0,NULL,NULL}}")
				, tuple("2026-02-02T00:00Z", "{{120,110,230},{5,500,505}}")
			)
			;
		then(agg("dh_agg_monthly", sid))
			.as("The month starts with the reading of b of 2 February, its first day that has one (NET-544)")
			.extracting("ts", "read_a")
			.containsExactly(
				  tuple("2026-02-01T00:00Z", "{{130,100,230},{5,500,505}}")
			)
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
	 * New partitions are created in the tablespace of their table, as
	 * {@code CREATE TABLE ... PARTITION OF} would do, for both the datum and
	 * the hourly table. Skipped unless the test database has a tablespace other
	 * than the default ones that the tests' role can create tables in.
	 */
	@Test
	public void partition_inTableTablespace() {
		assumeHourlyPartitioned();
		final String space = assumeTablespace();

		// GIVEN
		// @formatter:off
		then(hourlyPartitions())
			.as("No 2031 partition to start with")
			.extracting("name")
			.doesNotContain("dh_agg_hourly_2031")
			;
		// @formatter:on
		tableTablespace("dh_datum", space);
		tableTablespace("dh_agg_hourly", space);

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
		// @formatter:off
		then(hourlyPartitions())
			.as("No 2031 partition to start with")
			.extracting("name")
			.doesNotContain("dh_agg_hourly_2031")
			;
		// @formatter:on

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
			.as("""
				Only slots with datum; nothing across the gap longer than the clock tolerance, as \
				for the hourly rollup, and the difference in the last hour split at the start of \
				the later datum's slot""")
			.extracting("ts", "data_i", "stat_i", "data_a", "read_a")
			.containsExactly(
				  tuple("2025-01-01T10:00Z", "{1.5}", "{{2,1,2}}", "{20}", null)
				, tuple("2025-01-01T13:00Z", "{3}", "{{1,3,3}}", "{15}", null)
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
				Only the slot starting within the span, computed from all its datum, as in \
				SolarNet: the slot holding the start of the span starts before it, so is not \
				returned, and the datum after the end of the span in the last slot count in its \
				statistics and accumulation""")
			.extracting("ts", "data_i", "stat_i", "data_a")
			.containsExactly(
				  tuple("2025-01-01T10:10Z", "{3.5}", "{{2,3,4}}", "{7.99999999999999999998}")
			)
			;
		then(slotCount(sid, "2025-01-01T10:05:00Z", "2025-01-01T10:15:00Z", 600))
			.as("Slot count")
			.isEqualTo(1L)
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
			.as("The count puts each datum in the slot the rollup does, as SolarNet's does")
			.isEqualTo(2L)
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
					, CASE WHEN n = 4 THEN NULL ELSE 600 END
					, CASE WHEN n = 5 THEN NULL ELSE 'PT1H'::INTERVAL END)) AS count
				FROM generate_series(0, 5) n
				ORDER BY n
				"""), sid);
		List<Map<String, Object>> counts = jdbcTemplate.queryForList(sql("""
				SELECT n, solarhist.dh_count_datum_time_span_slots(
					  CASE WHEN n = 1 THEN NULL ELSE ?::INTEGER END
					, CASE WHEN n = 2 THEN NULL ELSE '2025-01-01T10:00:00Z'::TIMESTAMPTZ END
					, CASE WHEN n = 3 THEN NULL ELSE '2025-01-01T11:00:00Z'::TIMESTAMPTZ END
					, CASE WHEN n = 4 THEN NULL ELSE 600 END) AS count
				FROM generate_series(0, 4) n
				ORDER BY n
				"""), sid);

		// THEN
		// @formatter:off
		then(result)
			.as("A NULL stream, start, end, slot length or tolerance gives no rows")
			.extracting("count")
			.containsExactly(1L, 0L, 0L, 0L, 0L, 0L)
			;
		then(counts)
			.as("A NULL stream, start, end or slot length gives a count of zero")
			.extracting("count")
			.containsExactly(1L, 0L, 0L, 0L, 0L)
			;
		// @formatter:on
	}

	/**
	 * The clock accumulation of each hour of a span that has an hourly rollup,
	 * as text.
	 */
	private List<String> hourAccumulations(int streamId, String start, String end) {
		return jdbcTemplate.queryForList(sql("""
				SELECT h.data_a::TEXT
				FROM generate_series(?::TIMESTAMPTZ, ?::TIMESTAMPTZ - INTERVAL 'PT1H', INTERVAL 'PT1H') AS s(hour)
				INNER JOIN LATERAL solarhist.dh_rollup_datum_for_time_span(?, s.hour, s.hour + INTERVAL 'PT1H') h ON TRUE
				ORDER BY s.hour
				"""), String.class, start, end, streamId);
	}

	@Test
	public void slots_gapLongerThanTolerance_noAccumulation() {
		assumeSlotFunctions();

		// GIVEN
		// two datum 110 minutes apart, longer than the 1 hour clock tolerance
		final int sid = stream("A");
		datum(sid, "2020-06-01T12:05:00Z", 1, 100);
		datum(sid, "2020-06-01T13:55:00Z", 1, 210);

		// WHEN
		List<Map<String, Object>> result = slots(sid, "2020-06-01T12:00:00Z", "2020-06-01T14:00:00Z",
				600);
		List<String> hours = hourAccumulations(sid, "2020-06-01T12:00:00Z", "2020-06-01T14:00:00Z");

		// THEN
		// @formatter:off
		then(result)
			.as("""
				One slot for each datum, neither with accumulation, as the gap between the datum \
				exceeds the tolerance, as SolarNet's DbDatumRollupSlots_Net540Tests expect""")
			.extracting("ts", "data_a")
			.containsExactly(
				  tuple("2020-06-01T12:00Z", "{0}")
				, tuple("2020-06-01T13:50Z", "{0}")
			)
			;
		then(hours)
			.as("Slots add up to each hour's rollup")
			.containsExactly("{0}", "{0}")
			;
		then(slotCount(sid, "2020-06-01T12:00:00Z", "2020-06-01T14:00:00Z", 600))
			.as("Slot count")
			.isEqualTo(2L)
			;
		// @formatter:on
	}

	@Test
	public void slots_gapAcrossHour_splitAtHour() {
		assumeSlotFunctions();

		// GIVEN
		final int sid = stream("A");
		datum(sid, "2020-06-01T12:50:00Z", 1, 100);
		datum(sid, "2020-06-01T13:10:00Z", 1, 120);

		// WHEN
		List<Map<String, Object>> result = slots(sid, "2020-06-01T12:00:00Z", "2020-06-01T14:00:00Z",
				600);
		List<String> hours = hourAccumulations(sid, "2020-06-01T12:00:00Z", "2020-06-01T14:00:00Z");

		// THEN
		// @formatter:off
		then(result)
			.as("""
				The difference is split at the hour: (120 - 100) * (13:00 - 12:50) / (13:10 - 12:50) \
				in the slot of the datum before the hour, and the rest in the slot of the first datum \
				in the hour after, as SolarNet's DbDatumRollupSlots_Net540Tests expect""")
			.extracting("ts", "data_a")
			.containsExactly(
				  tuple("2020-06-01T12:50Z", "{10}")
				, tuple("2020-06-01T13:10Z", "{10}")
			)
			;
		then(hours)
			.as("Slots add up to each hour's rollup")
			.containsExactly("{10}", "{10}")
			;
		then(slotCount(sid, "2020-06-01T12:00:00Z", "2020-06-01T14:00:00Z", 600))
			.as("Slot count")
			.isEqualTo(2L)
			;
		// @formatter:on
	}

	@Test
	public void slots_gapAcrossHour_spanFromHour_sameSlot() {
		assumeSlotFunctions();

		// GIVEN
		final int sid = stream("A");
		datum(sid, "2020-06-01T12:50:00Z", 1, 100);
		datum(sid, "2020-06-01T13:10:00Z", 1, 120);

		// WHEN
		List<Map<String, Object>> result = slots(sid, "2020-06-01T13:00:00Z", "2020-06-01T14:00:00Z",
				600);

		// THEN
		// @formatter:off
		then(result)
			.as("""
				One slot, of the datum in the span, with the hour's share of the difference from \
				the datum before the span, as when the span starts before that datum: \
				(120 - 100) * (13:10 - 13:00) / (13:10 - 12:50), as SolarNet's \
				DbDatumRollupSlots_Net540Tests expect""")
			.extracting("ts", "data_a")
			.containsExactly(
				  tuple("2020-06-01T13:10Z", "{10}")
			)
			;
		then(slotCount(sid, "2020-06-01T13:00:00Z", "2020-06-01T14:00:00Z", 600))
			.as("Slot count")
			.isEqualTo(1L)
			;
		// @formatter:on
	}

	@Test
	public void slots_datumWithoutAccumulatingInSlot_keepsShareOfDifference() {
		assumeSlotFunctions();

		// GIVEN
		// the last datum in the first slot has no accumulating value
		final int sid = stream("A");
		datum(sid, "2020-06-01T12:02:00Z", 1, 100);
		datum(sid, "2020-06-01T12:07:00Z", 1, null);
		datum(sid, "2020-06-01T12:12:00Z", 1, 110);

		// WHEN
		List<Map<String, Object>> result = slots(sid, "2020-06-01T12:00:00Z", "2020-06-01T13:00:00Z",
				600);
		List<String> hours = hourAccumulations(sid, "2020-06-01T12:00:00Z", "2020-06-01T13:00:00Z");

		// THEN
		// @formatter:off
		then(result)
			.as("""
				The first slot has its share of the difference to the datum in the next slot, past \
				the datum without an accumulating value: (110 - 100) * (12:10 - 12:02) / \
				(12:12 - 12:02), as SolarNet's DbDatumRollupSlots_Net540Tests expect""")
			.extracting("ts", "data_i", "stat_i", "data_a")
			.containsExactly(
				  tuple("2020-06-01T12:00Z", "{1}", "{{2,1,1}}", "{8}")
				, tuple("2020-06-01T12:10Z", "{1}", "{{1,1,1}}", "{2}")
			)
			;
		then(hours)
			.as("Slots add up to the hour's rollup")
			.containsExactly("{10}")
			;
		// @formatter:on
	}

	@Test
	public void slots_firstDatumAtHour_rowInLastSlotOfHourBefore() {
		assumeSlotFunctions();

		// GIVEN
		// no datum before the hour; the first is exactly at the start of the hour
		final int sid = stream("A");
		datum(sid, "2020-06-01T12:00:00Z", 1, 100);
		datum(sid, "2020-06-01T12:10:00Z", 1, 110);

		// WHEN
		List<Map<String, Object>> result = slots(sid, "2020-06-01T11:00:00Z", "2020-06-01T13:00:00Z",
				600);
		List<String> hours = hourAccumulations(sid, "2020-06-01T11:00:00Z", "2020-06-01T13:00:00Z");

		// THEN
		// @formatter:off
		then(result)
			.as("""
				The hour before the datum has a row in its last slot, without instantaneous values \
				or accumulation, as it has an hourly rollup; then one slot for each datum, the \
				first with (110 - 100), as SolarNet's DbDatumRollupSlots_Net540Tests expect""")
			.extracting("ts", "data_i", "stat_i", "data_a")
			.containsExactly(
				  tuple("2020-06-01T11:50Z", null, null, "{0}")
				, tuple("2020-06-01T12:00Z", "{1}", "{{1,1,1}}", "{10}")
				, tuple("2020-06-01T12:10Z", "{1}", "{{1,1,1}}", "{0}")
			)
			;
		then(hours)
			.as("Slots add up to each hour's rollup")
			.containsExactly("{0}", "{10}")
			;
		then(slotCount(sid, "2020-06-01T11:00:00Z", "2020-06-01T13:00:00Z", 600))
			.as("Slot count, with the row of the hour before")
			.isEqualTo(3L)
			;
		// @formatter:on
	}

	@Test
	public void slots_invalidSlotSeconds_use600() {
		assumeSlotFunctions();

		// GIVEN
		final int sid = stream("A");
		datum(sid, "2025-01-01T10:00:00Z", 1, 100);
		datum(sid, "2025-01-01T10:06:00Z", 2, 106);
		datum(sid, "2025-01-01T10:12:00Z", 3, 112);
		datum(sid, "2025-01-01T10:18:00Z", 4, 118);
		final List<Map<String, Object>> expected = slots(sid, "2025-01-01T10:00:00Z",
				"2025-01-01T11:00:00Z", 600);

		for ( int secs : new int[] { 0, 59, 420, 3600 } ) {
			// WHEN
			List<Map<String, Object>> result = slots(sid, "2025-01-01T10:00:00Z", "2025-01-01T11:00:00Z",
					secs);
			long count = slotCount(sid, "2025-01-01T10:00:00Z", "2025-01-01T11:00:00Z", secs);

			// THEN
			// @formatter:off
			then(result)
				.as("The 10 minute slots for an invalid slot length of %d seconds, as in SolarNet", secs)
				.isEqualTo(expected)
				.extracting("ts")
				.containsExactly("2025-01-01T10:00Z", "2025-01-01T10:10Z")
				;
			then(count)
				.as("The 10 minute slot count for an invalid slot length of %d seconds", secs)
				.isEqualTo(2L)
				;
			// @formatter:on
		}
	}

	@Test
	public void slots_fifteenMinutes_equalFifteenMinuteSlots() {
		assumeSlotFunctions();
		assume15m();

		// GIVEN
		generatedStreams();
		generatedHours("UTC");

		// WHEN
		Map<String, Object> result = jdbcTemplate.queryForMap(sql("""
				WITH f AS (
					SELECT h.k, x.ts_start, x.data_i::TEXT AS data_i, x.stat_i::TEXT AS stat_i
						, x.data_a::TEXT AS data_a, x.data_s::TEXT AS data_s, x.data_t::TEXT AS data_t
					FROM t_hours h
						, solarhist.dh_rollup_datum_15m_for_hour(h.sid, h.hour, h.hour + INTERVAL 'PT1H') q
						, unnest(q.slots) x
				)
				, o AS (
					SELECT h.k, r.ts_start, r.data_i::TEXT AS data_i, r.stat_i::TEXT AS stat_i
						, r.data_a::TEXT AS data_a, r.data_s::TEXT AS data_s, r.data_t::TEXT AS data_t
						, r.read_a
					FROM t_hours h
						, solarhist.dh_rollup_datum_for_time_span_slots(h.sid, h.hour, h.hour + INTERVAL 'PT1H', 900) r
				)
				SELECT (SELECT count(DISTINCT k) FROM t_hours) AS streams
					, (SELECT count(*) FROM f) AS slots
					, (SELECT count(*) FROM o) AS rows
					, (SELECT count(*) FROM f FULL OUTER JOIN o USING (k, ts_start)
						WHERE f.k IS NULL OR o.k IS NULL) AS unpaired
					, (SELECT count(*) FROM f INNER JOIN o USING (k, ts_start)
						WHERE (f.data_i, f.stat_i, f.data_s, f.data_t)
							IS DISTINCT FROM (o.data_i, o.stat_i, o.data_s, o.data_t)) AS other_differ
					, (SELECT count(*) FROM o WHERE o.read_a IS NOT NULL) AS with_readings
					, (SELECT array_agg(DISTINCT f.k ORDER BY f.k)::TEXT FROM f INNER JOIN o USING (k, ts_start)
						WHERE f.data_a IS DISTINCT FROM o.data_a) AS a_differ_streams
				"""));

		// THEN
		// @formatter:off
		then(count(result, "streams"))
			.as("All generated streams compared")
			.isEqualTo(24L)
			;
		then(count(result, "slots"))
			.as("Many 15 minute slots")
			.isGreaterThan(4000L)
			;
		then(count(result, "rows"))
			.as("The function at 900 seconds returns a row for each 15 minute slot of each UTC hour")
			.isEqualTo(count(result, "slots"))
			;
		then(count(result, "unpaired"))
			.as("Every row is a 15 minute slot, and every slot a row")
			.isZero()
			;
		then(count(result, "other_differ"))
			.as("The same means, statistics, status and tags as the 15 minute slots, as text")
			.isZero()
			;
		then(count(result, "with_readings"))
			.as("The function returns no readings")
			.isZero()
			;
		then(result.get("a_differ_streams"))
			.as("""
				The same accumulation as the 15 minute slots, as text, except in the streams whose \
				datum lack a property part of the time (17, 20 and 21): the function differences a \
				datum against the nearest datum with any accumulating property, where the hour \
				differences each property against the nearest datum that has it""")
			.isEqualTo("{17,20,21}")
			;
		// @formatter:on
	}

	/*
	 * ==================================================================
	 * 15 minute aggregates
	 * ==================================================================
	 */

	/**
	 * Skip a test when the schema has an aggregate setup from before the 15
	 * minute aggregate table.
	 */
	private void assume15m() {
		final Boolean found = jdbcTemplate.queryForObject(
				"SELECT to_regclass(?) IS NOT NULL AND to_regprocedure(?) IS NOT NULL", Boolean.class,
				SCHEMA + ".dh_agg_15m", SCHEMA
						+ ".dh_rollup_datum_15m_for_hour(integer,timestamp with time zone,timestamp with time zone,interval,interval)");
		assumeTrue("The %s schema does not have the 15 minute aggregate table.".formatted(SCHEMA),
				Boolean.TRUE.equals(found));
	}

	/**
	 * The session time zones the generated streams' hours are taken in: their
	 * offsets are a whole hour, a half hour and a quarter hour, so the hours
	 * start at :00, :30 and :15 past a UTC hour.
	 */
	private static final List<String> GENERATED_ZONES = List.of("UTC", "Asia/Kolkata",
			"Asia/Kathmandu");

	/**
	 * Load the 24 generated streams of {@code net540-generated-streams.sql},
	 * into the temporary table {@code t_stream}.
	 */
	private void generatedStreams() {
		jdbcTemplate.execute(sql(utf8StringResource("net540-generated-streams.sql", getClass())));
	}

	/**
	 * Create the temporary table {@code t_hours} of every hour of each
	 * generated stream's datum, plus two hours either side, as hours of a
	 * session time zone, which stays set.
	 */
	private void generatedHours(String zone) {
		jdbcTemplate.queryForObject("SELECT set_config('TimeZone', ?, TRUE)", String.class, zone);
		jdbcTemplate.execute("DROP TABLE IF EXISTS pg_temp.t_hours");
		jdbcTemplate.execute(sql("""
				CREATE TEMP TABLE t_hours ON COMMIT DROP AS
				SELECT s.k, s.sid, h AS hour
				FROM t_stream s
				INNER JOIN LATERAL (
					SELECT date_trunc('hour', min(ts)) - INTERVAL 'PT2H' AS a
						, date_trunc('hour', max(ts)) + INTERVAL 'PT2H' AS b
					FROM solarhist.dh_datum WHERE stream_id = s.sid
				) r ON TRUE
				INNER JOIN LATERAL generate_series(r.a, r.b, INTERVAL 'PT1H') h ON TRUE
				"""));
	}

	private static long count(Map<String, Object> row, String key) {
		return ((Number) row.get(key)).longValue();
	}

	/**
	 * Store a datum every 5 minutes: {@code i} from 100 and {@code val} from 0,
	 * going up by 1 and 10 with each datum. So each 15 minute slot of an hour
	 * has three datum and an accumulation of 30.
	 */
	private void datumEvery5m(int streamId, String start, int count) {
		jdbcTemplate.queryForList(sql("SELECT solarhist.dh_datum_ensure_partition(?::TIMESTAMPTZ)"),
				start);
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_datum (ts, stream_id, type_id, data_i, data_a)
				SELECT ?::TIMESTAMPTZ + n * INTERVAL 'PT5M', ?, 1, ARRAY[100 + n]::NUMERIC[], ARRAY[10 * n]::NUMERIC[]
				FROM generate_series(0, ? - 1) n
				"""), start, streamId, count);
	}

	/** Get the hour and slots of an hour, as text. */
	private List<Map<String, Object>> rollup15m(int streamId, String hour) {
		return jdbcTemplate.queryForList(sql("""
				SELECT 'hour' AS slot, q.data_i::TEXT AS data_i, q.stat_i::TEXT AS stat_i
					, q.data_a::TEXT AS data_a, q.read_a::TEXT AS read_a
				FROM solarhist.dh_rollup_datum_15m_for_hour(?, ?::TIMESTAMPTZ, ?::TIMESTAMPTZ + INTERVAL 'PT1H') q
				UNION ALL
				SELECT to_char(x.ts_start AT TIME ZONE 'UTC', 'HH24:MI'), x.data_i::TEXT, x.stat_i::TEXT
					, x.data_a::TEXT, x.read_a::TEXT
				FROM solarhist.dh_rollup_datum_15m_for_hour(?, ?::TIMESTAMPTZ, ?::TIMESTAMPTZ + INTERVAL 'PT1H') q
					, unnest(q.slots) x
				"""), streamId, hour, hour, streamId, hour, hour);
	}

	/** Get the partition, start and row location of each 15 minute row. */
	private Map<String, String> fifteenLocations(int streamId) {
		final Map<String, String> result = new LinkedHashMap<>(8);
		for ( Map<String, Object> row : fifteenRows(streamId) ) {
			result.put(row.get("ts").toString(), row.get("ctid").toString());
		}
		return result;
	}

	/** Get the partition, start and row location of each 15 minute row. */
	private List<Map<String, Object>> fifteenRows(int streamId) {
		return jdbcTemplate.queryForList(sql("""
				SELECT (SELECT c.relname::TEXT FROM pg_class c WHERE c.oid = f.tableoid) AS part
					, to_char(f.ts_start AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"') AS ts
					, f.ctid::TEXT AS ctid
				FROM solarhist.dh_agg_15m f
				WHERE f.stream_id = ?
				ORDER BY f.ts_start
				"""), streamId);
	}

	private String hourlyLocation(int streamId, String hour) {
		return jdbcTemplate.queryForObject(sql("""
				SELECT ctid::TEXT FROM solarhist.dh_agg_hourly
				WHERE stream_id = ? AND ts_start = ?::TIMESTAMPTZ
				"""), String.class, streamId, hour);
	}

	/** Get the name, bounds and owner of each partition of a table. */
	private List<Map<String, Object>> partitions(String table) {
		return jdbcTemplate.queryForList("""
				SELECT c.relname::TEXT AS name, pg_get_expr(c.relpartbound, c.oid) AS bounds
					, pg_get_userbyid(c.relowner)::TEXT AS owner
				FROM pg_inherits i
				INNER JOIN pg_class c ON c.oid = i.inhrelid
				WHERE i.inhparent = ?::regclass
				ORDER BY c.relname
				""", SCHEMA + "." + table);
	}

	private void ensurePartition(String table, String ts) {
		jdbcTemplate.queryForList(sql("SELECT solarhist.dh_agg_ensure_partition(?, ?::TIMESTAMPTZ)"),
				table, ts);
	}

	/** The hour of the stale processing tests. */
	private static final String HOUR_15M = "2020-06-01T10:00:00Z";

	@Test
	public void fifteen_hourRowEqualsHourlyRollup() {
		assume15m();

		// GIVEN
		generatedStreams();

		for ( String zone : GENERATED_ZONES ) {
			generatedHours(zone);

			// WHEN
			Map<String, Object> result = jdbcTemplate.queryForMap(sql("""
					WITH o AS (
						SELECT h.k, h.hour, q.*
						FROM t_hours h
							, solarhist.dh_rollup_datum_for_time_span(h.sid, h.hour, h.hour + INTERVAL 'PT1H') q
					)
					, n AS (
						SELECT h.k, h.hour, q.*
						FROM t_hours h
							, solarhist.dh_rollup_datum_15m_for_hour(h.sid, h.hour, h.hour + INTERVAL 'PT1H') q
					)
					SELECT (SELECT count(DISTINCT k) FROM t_hours) AS streams
						, (SELECT count(*) FROM t_hours) AS hours
						, (SELECT count(*) FROM o) AS hourly_rows
						, (SELECT count(*) FROM n) AS rows
						, (SELECT count(*) FROM o FULL OUTER JOIN n USING (k, hour)
							WHERE o.k IS NULL OR n.k IS NULL
								OR (o.stream_id, o.ts_start, o.data_i, o.data_a, o.data_s, o.data_t, o.stat_i, o.read_a)::TEXT
								IS DISTINCT FROM
								(n.stream_id, n.ts_start, n.data_i, n.data_a, n.data_s, n.data_t, n.stat_i, n.read_a)::TEXT
						) AS differ
					"""));

			// THEN
			// @formatter:off
			then(count(result, "streams"))
				.as("All generated streams compared, in %s", zone)
				.isEqualTo(24L)
				;
			then(count(result, "hourly_rows"))
				.as("Many hours have an hourly row, in %s", zone)
				.isGreaterThan(1000L)
				;
			then(count(result, "hours") - count(result, "hourly_rows"))
				.as("Some hours have no hourly row, in %s", zone)
				.isPositive()
				;
			then(count(result, "rows"))
				.as("Same number of hour rows as the hourly rollup, in %s", zone)
				.isEqualTo(count(result, "hourly_rows"))
				;
			then(count(result, "differ"))
				.as("No hour row differs from the hourly rollup, as text, and no hour has a row in only one, in %s", zone)
				.isZero()
				;
			// @formatter:on
		}
	}

	@Test
	public void fifteen_slotsAddUpToHour() {
		assume15m();

		// GIVEN
		generatedStreams();

		for ( String zone : GENERATED_ZONES ) {
			generatedHours(zone);

			// WHEN
			Map<String, Object> result = jdbcTemplate.queryForMap(sql("""
					WITH n AS (
						SELECT h.k, h.sid, h.hour, q.*
						FROM t_hours h
							, solarhist.dh_rollup_datum_15m_for_hour(h.sid, h.hour, h.hour + INTERVAL 'PT1H') q
					)
					, s AS (
						SELECT n.k, n.sid, n.hour, x.*
						FROM n, unnest(n.slots) x
					)
					, p AS (
						SELECT n.k, n.hour, n.data_a, n.stat_i, i.idx
						FROM n, generate_series(1, 2) i(idx)
					)
					-- hour rows with no datum in their hour
					, nd AS (
						SELECT n.*
						FROM n
						WHERE NOT EXISTS (
								SELECT 1 FROM solarhist.dh_datum d
								WHERE d.stream_id = n.sid
									AND d.ts >= n.hour AND d.ts < n.hour + INTERVAL 'PT1H')
					)
					, sa AS (
						SELECT s.k, s.hour, i.idx
							, sum(s.data_a[i.idx]) AS a
							, sum(s.stat_i[i.idx][1]) AS c
							, min(s.stat_i[i.idx][2]) AS mn
							, max(s.stat_i[i.idx][3]) AS mx
						FROM s, generate_series(1, 2) i(idx)
						GROUP BY s.k, s.hour, i.idx
					)
					SELECT (SELECT count(*) FROM n) AS rows
						, (SELECT count(*) FROM s) AS slot_rows
						, (SELECT count(*) FROM n WHERE n.slots IS NULL) AS rows_without_slots
						, (SELECT count(*) FROM s WHERE s.stream_id <> s.sid) AS slot_stream_wrong
						, (SELECT count(*) FROM s
							WHERE s.ts_start < s.hour OR s.ts_start >= s.hour + INTERVAL 'PT1H'
								OR s.ts_start <> date_bin(INTERVAL 'PT15M', s.ts_start, TIMESTAMPTZ '2000-01-01 00:00:00+00')
						) AS slot_misplaced
						, (SELECT count(*) - count(DISTINCT (s.k, s.ts_start)) FROM s) AS slot_duplicated
						, (SELECT count(*) FROM n
							WHERE n.slots <> (SELECT array_agg(x ORDER BY x.ts_start) FROM unnest(n.slots) x)
						) AS slots_unordered
						, (SELECT count(*) FROM nd) AS rows_without_datum
						, (SELECT count(*) FROM nd
							WHERE cardinality(nd.slots) = 1
								AND (nd.slots[1]).ts_start = nd.hour + INTERVAL 'PT45M'
						) AS rows_without_datum_last_slot
						, (SELECT count(*) FROM p LEFT OUTER JOIN sa USING (k, hour, idx)
							WHERE COALESCE(p.data_a[p.idx], 0) <> COALESCE(sa.a, 0)
						) AS data_a_differ
						, (SELECT count(*) FROM p LEFT OUTER JOIN sa USING (k, hour, idx)
							WHERE p.stat_i[p.idx][1] IS DISTINCT FROM sa.c
								OR p.stat_i[p.idx][2] IS DISTINCT FROM sa.mn
								OR p.stat_i[p.idx][3] IS DISTINCT FROM sa.mx
						) AS stat_i_differ
						, (SELECT count(*) FROM n
							WHERE n.data_t IS DISTINCT FROM (
								SELECT array_agg(DISTINCT t ORDER BY t)
								FROM s, unnest(s.data_t) t
								WHERE s.k = n.k AND s.hour = n.hour)
						) AS data_t_differ
					"""));

			// THEN
			// @formatter:off
			then(count(result, "rows"))
				.as("Many hours have a row, in %s", zone)
				.isGreaterThan(1000L)
				;
			then(count(result, "slot_rows"))
				.as("More slot rows than hour rows, in %s", zone)
				.isGreaterThan(count(result, "rows"))
				;
			then(count(result, "rows_without_slots"))
				.as("Every hour row has slot rows, in %s", zone)
				.isZero()
				;
			then(count(result, "slot_stream_wrong"))
				.as("Every slot row has the hour's stream, in %s", zone)
				.isZero()
				;
			then(count(result, "slot_misplaced"))
				.as("Every slot row starts within its hour, on a 15 minute boundary, in %s", zone)
				.isZero()
				;
			then(count(result, "slot_duplicated"))
				.as("No slot row is repeated, in %s", zone)
				.isZero()
				;
			then(count(result, "slots_unordered"))
				.as("Slot rows are in time order, in %s", zone)
				.isZero()
				;
			// fifteen_hourWithoutDatum() checks such an hour exactly
			then(count(result, "rows_without_datum_last_slot"))
				.as("An hour row without datum has one slot row, the last slot of the hour, in %s", zone)
				.isEqualTo(count(result, "rows_without_datum"))
				;
			then(count(result, "data_a_differ"))
				.as("The slots' accumulation of each property adds up to the hour's exactly, in %s", zone)
				.isZero()
				;
			then(count(result, "stat_i_differ"))
				.as("The slots' counts add up to the hour's, and their minimum and maximum are the hour's, in %s", zone)
				.isZero()
				;
			then(count(result, "data_t_differ"))
				.as("The slots' tags together are the hour's, in %s", zone)
				.isZero()
				;
			// @formatter:on
		}
	}

	@Test
	public void fifteen_slotsRolledUpEqualHour() {
		assume15m();
		assumeHourlyPartitioned();

		// GIVEN
		generatedStreams();
		hourlyPartition("2030-03-04T00:00:00Z");

		for ( String zone : GENERATED_ZONES ) {
			generatedHours(zone);

			// the slot rows stand in the hourly table, so the function that rolls hours up to days
			// can roll them up to their hours
			jdbcTemplate.update(sql("DELETE FROM solarhist.dh_agg_hourly"));
			int slotCount = jdbcTemplate.update(sql("""
					INSERT INTO solarhist.dh_agg_hourly (stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a)
					SELECT x.stream_id, x.ts_start, x.data_i, x.data_a, x.data_s, x.data_t, x.stat_i, x.read_a
					FROM t_hours h
						, solarhist.dh_rollup_datum_15m_for_hour(h.sid, h.hour, h.hour + INTERVAL 'PT1H') q
						, unnest(q.slots) x
					"""));

			// WHEN
			Map<String, Object> result = jdbcTemplate.queryForMap(sql("""
					WITH n AS (
						SELECT h.k, h.hour, q.*
						FROM t_hours h
							, solarhist.dh_rollup_datum_15m_for_hour(h.sid, h.hour, h.hour + INTERVAL 'PT1H') q
					)
					, u AS (
						SELECT h.k, h.hour, q.*
						FROM t_hours h
							, solarhist.dh_rollup_agg_data_for_time_span(h.sid, h.hour, h.hour + INTERVAL 'PT1H', 'h') q
					)
					SELECT count(*) AS rows
						, count(*) FILTER (WHERE n.k IS NULL OR u.k IS NULL) AS presence_differ
						, count(*) FILTER (WHERE n.data_a IS DISTINCT FROM u.data_a) AS data_a_differ
						, count(*) FILTER (WHERE n.read_a IS DISTINCT FROM u.read_a) AS read_a_differ
						, count(*) FILTER (WHERE n.stat_i IS DISTINCT FROM u.stat_i) AS stat_i_differ
						, count(*) FILTER (WHERE n.data_t IS DISTINCT FROM u.data_t) AS data_t_differ
						, COALESCE(max(x.rel), 0)::FLOAT8 AS data_i_max_rel_diff
					FROM n
					FULL OUTER JOIN u USING (k, hour)
					LEFT OUTER JOIN LATERAL (
						SELECT max(abs(a - b) / NULLIF(abs(a), 0)) AS rel
						FROM unnest(n.data_i, u.data_i) AS x(a, b)
					) x ON TRUE
					"""));

			// THEN
			// the hour's status is the most frequent value of its datum, which the most frequent
			// value of its slots need not be, so data_s is not compared
			// @formatter:off
			then(slotCount)
				.as("Slot rows rolled up, in %s", zone)
				.isGreaterThan(1000)
				;
			then(count(result, "rows"))
				.as("Many hours have a row, in %s", zone)
				.isGreaterThan(1000L)
				;
			then(count(result, "presence_differ"))
				.as("The slots roll up to a row for exactly the hours that have one, in %s", zone)
				.isZero()
				;
			then(count(result, "data_a_differ"))
				.as("The slots roll up to the hour's accumulation, in %s", zone)
				.isZero()
				;
			then(count(result, "read_a_differ"))
				.as("The slots roll up to the hour's reading difference, start and end, in %s", zone)
				.isZero()
				;
			then(count(result, "stat_i_differ"))
				.as("The slots roll up to the hour's counts, minimums and maximums, in %s", zone)
				.isZero()
				;
			then(count(result, "data_t_differ"))
				.as("The slots roll up to the hour's tags, in %s", zone)
				.isZero()
				;
			then((Double) result.get("data_i_max_rel_diff"))
				.as("The slots roll up to the hour's averages, within rounding, in %s", zone)
				.isLessThan(1e-12)
				;
			// @formatter:on
		}
	}

	@Test
	public void fifteen_propertyWithoutValueInSlot() {
		assume15m();

		// GIVEN
		final int sid = jdbcTemplate.queryForObject(sql("""
				INSERT INTO solarhist.dh_datum_meta (node_id, source_id, names_i, names_a, type_ids)
				VALUES (?, ?, '{w}', '{wh,x}', '{1}')
				RETURNING stream_id
				"""), Integer.class, TEST_NODE_ID, "A");
		// a datum every 5 minutes from 10:00 to 11:00, without wh from 10:30 to 10:40, never
		// with x
		jdbcTemplate.queryForList(sql("SELECT solarhist.dh_datum_ensure_partition(?::TIMESTAMPTZ)"),
				HOUR_15M);
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_datum (ts, stream_id, type_id, data_i, data_a)
				SELECT ?::TIMESTAMPTZ + n * INTERVAL 'PT5M', ?, 1
					, ARRAY[100 + n]::NUMERIC[]
					, CASE
						WHEN n BETWEEN 6 AND 8 THEN ARRAY[NULL, NULL]::NUMERIC[]
						ELSE ARRAY[10 * n, NULL]::NUMERIC[]
						END
				FROM generate_series(0, 12) n
				"""), HOUR_15M, sid);

		// WHEN
		List<Map<String, Object>> result = rollup15m(sid, HOUR_15M);

		// THEN
		// the wh accumulation from 10:25 to 10:45 belongs to the slot of the 10:25 datum, as no
		// datum between has wh; the readings follow the datum
		// @formatter:off
		then(result)
			.as("""
				The hour accumulates wh, and has zero for x. A slot whose datum lack wh has NULL \
				for it, as another slot of the hour has wh; every slot has zero for x, as no slot \
				of the hour has x""")
			.extracting("slot", "data_i", "data_a", "read_a")
			.containsExactly(
				  tuple("hour", "{105.5}", "{120,0}", "{{120,0,120},{0,NULL,NULL}}")
				, tuple("10:00", "{101}", "{30,0}", "{{20,0,20},{0,NULL,NULL}}")
				, tuple("10:15", "{104}", "{60,0}", "{{30,20,50},{0,NULL,NULL}}")
				, tuple("10:30", "{107}", "{NULL,0}", "{{0,NULL,NULL},{0,NULL,NULL}}")
				, tuple("10:45", "{110}", "{30,0}", "{{70,50,120},{0,NULL,NULL}}")
			)
			;
		// @formatter:on
	}

	@Test
	public void fifteen_hourWithoutDatum() {
		assume15m();

		// GIVEN
		final int sid = stream("A");
		// no datum in the hour, but one exactly at its end: the hour has a row for the reading
		// from the datum before the hour, which is too long before for any clock accumulation
		datum(sid, "2020-06-01T09:58:00Z", 1, 10);
		datum(sid, "2020-06-01T11:00:00Z", 2, 50);

		// WHEN
		List<Map<String, Object>> result = rollup15m(sid, HOUR_15M);

		// THEN
		// @formatter:off
		then(result)
			.as("""
				The hour has no instantaneous values, zero accumulation, and the reading across \
				it; its one slot row is its last slot, with the hour's values""")
			.extracting("slot", "data_i", "stat_i", "data_a", "read_a")
			.containsExactly(
				  tuple("hour", null, null, "{0}", "{{40,10,50}}")
				, tuple("10:45", null, null, "{0}", "{{40,10,50}}")
			)
			;
		// @formatter:on
	}

	@Test
	public void fifteen_nullArguments() {
		assume15m();

		// GIVEN
		final int sid = stream("A");
		datum(sid, "2020-06-01T10:05:00Z", 1, 100);

		// WHEN
		List<Map<String, Object>> result = jdbcTemplate.queryForList(sql("""
				SELECT n, (SELECT count(*) FROM solarhist.dh_rollup_datum_15m_for_hour(
					  CASE WHEN n = 1 THEN NULL ELSE ?::INTEGER END
					, CASE WHEN n = 2 THEN NULL ELSE '2020-06-01T10:00:00Z'::TIMESTAMPTZ END
					, CASE WHEN n = 3 THEN NULL ELSE '2020-06-01T11:00:00Z'::TIMESTAMPTZ END)) AS count
				FROM generate_series(0, 3) n
				ORDER BY n
				"""), sid);

		// THEN
		// @formatter:off
		then(result)
			.as("A NULL stream, start or end gives no rows")
			.extracting("count")
			.containsExactly(1L, 0L, 0L, 0L)
			;
		// @formatter:on
	}

	@Test
	public void fifteen_slotsWritten() {
		assume15m();

		// GIVEN
		final int sid = stream("A");
		datumEvery5m(sid, HOUR_15M, 15);

		// WHEN
		process("h");

		// THEN
		// @formatter:off
		then(agg("dh_agg_hourly", sid))
			.as("Hours stored: the hour, and the next for the datum exactly at its end")
			.extracting("ts", "data_a", "read_a")
			.containsExactly(
				  tuple("2020-06-01T10:00Z", "{120}", "{{120,0,120}}")
				, tuple("2020-06-01T11:00Z", "{20}", "{{20,120,140}}")
			)
			;
		then(agg("dh_agg_15m", sid))
			.as("""
				Every slot of each hour stored, in order: averages, counts, minimums and maximums \
				by slot; accumulation from each datum to the next in the slot of the earlier one; \
				each datum's reading difference in its own slot, the datum at the end of the hour \
				in the last""")
			.extracting("ts", "data_i", "stat_i", "data_a", "read_a")
			.containsExactly(
				  tuple("2020-06-01T10:00Z", "{101}", "{{3,100,102}}", "{30}", "{{20,0,20}}")
				, tuple("2020-06-01T10:15Z", "{104}", "{{3,103,105}}", "{30}", "{{30,20,50}}")
				, tuple("2020-06-01T10:30Z", "{107}", "{{3,106,108}}", "{30}", "{{30,50,80}}")
				, tuple("2020-06-01T10:45Z", "{110}", "{{3,109,111}}", "{30}", "{{40,80,120}}")
				, tuple("2020-06-01T11:00Z", "{113}", "{{3,112,114}}", "{20}", "{{20,120,140}}")
			)
			;
		// @formatter:on
	}

	@Test
	public void fifteen_slotsWritten_nodeTimeZone() {
		assume15m();

		// GIVEN
		jdbcTemplate.update(
				sql("""
						INSERT INTO solarhist.dh_node_time_zone (node_id, time_zone) VALUES (?, 'Asia/Kathmandu')
						"""),
				TEST_NODE_ID);
		jdbcTemplate.update("SET LOCAL TimeZone = 'Asia/Kathmandu'");
		final int sid = stream("A");
		datumEvery5m(sid, HOUR_15M, 12);

		// WHEN
		processAll();

		// THEN
		jdbcTemplate.update("SET LOCAL TimeZone = 'UTC'");
		// @formatter:off
		then(agg("dh_agg_15m", sid))
			.as("Slots are quarters of the UTC hour, whatever the node's and session's zones")
			.extracting("ts", "data_a")
			.containsExactly(
				  tuple("2020-06-01T10:00Z", "{30}")
				, tuple("2020-06-01T10:15Z", "{30}")
				, tuple("2020-06-01T10:30Z", "{30}")
				, tuple("2020-06-01T10:45Z", "{20}")
			)
			;
		then(agg("dh_agg_daily", sid))
			.as("The day is in the node's zone")
			.extracting("ts", "data_a")
			.containsExactly(tuple("2020-05-31T18:15Z", "{110}"))
			;
		// @formatter:on
	}

	@Test
	public void fifteen_unchangedSlotsNotRewritten() {
		assume15m();

		// GIVEN
		final int sid = stream("A");
		datumEvery5m(sid, HOUR_15M, 15);
		process("h");
		final Map<String, String> locations = fifteenLocations(sid);
		final String hourLocation = hourlyLocation(sid, HOUR_15M);

		// WHEN
		// change the average of the second slot only
		jdbcTemplate.update(sql("""
				UPDATE solarhist.dh_datum SET data_i = ARRAY[200]::NUMERIC[]
				WHERE stream_id = ? AND ts = ?::TIMESTAMPTZ
				"""), sid, "2020-06-01T10:20:00Z");
		process("h");

		// THEN
		// @formatter:off
		then(fifteenLocations(sid))
			.as("Same slots stored")
			.containsOnlyKeys(locations.keySet())
			.as("Unchanged slot rows not rewritten")
			.containsEntry("2020-06-01T10:00Z", locations.get("2020-06-01T10:00Z"))
			.containsEntry("2020-06-01T10:30Z", locations.get("2020-06-01T10:30Z"))
			.containsEntry("2020-06-01T10:45Z", locations.get("2020-06-01T10:45Z"))
			.containsEntry("2020-06-01T11:00Z", locations.get("2020-06-01T11:00Z"))
			.as("Changed slot row rewritten")
			.doesNotContainEntry("2020-06-01T10:15Z", locations.get("2020-06-01T10:15Z"))
			;
		then(agg("dh_agg_15m", sid))
			.as("Changed slot average stored")
			.extracting("data_i")
			.containsExactly("{101}", "{136}", "{107}", "{110}", "{113}")
			;
		then(hourlyLocation(sid, HOUR_15M))
			.as("Hour row rewritten, whether changed or not, as before")
			.isNotEqualTo(hourLocation)
			;
		// @formatter:on
	}

	@Test
	public void fifteen_unchangedHourSlotsNotRewritten() {
		assume15m();

		// GIVEN
		final int sid = stream("A");
		datumEvery5m(sid, HOUR_15M, 15);
		process("h");
		final Map<String, String> locations = fifteenLocations(sid);
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_agg_stale (stream_id, ts_start, agg_kind)
				VALUES (?, ?::TIMESTAMPTZ, 'h'), (?, ?::TIMESTAMPTZ + INTERVAL 'PT1H', 'h')
				"""), sid, HOUR_15M, sid, HOUR_15M);

		// WHEN
		final int count = process("h");

		// THEN
		// @formatter:off
		then(count)
			.as("Both hours processed again")
			.isEqualTo(2)
			;
		then(fifteenLocations(sid))
			.as("No slot row rewritten when the hours are processed again unchanged")
			.isEqualTo(locations)
			.as("All slots stored")
			.hasSize(5)
			;
		// @formatter:on
	}

	@Test
	public void fifteen_lateDatum() {
		assume15m();

		// GIVEN
		final int sid = stream("A");
		datumEvery5m(sid, HOUR_15M, 12);
		process("h");
		final Map<String, String> locations = fifteenLocations(sid);

		// WHEN
		// a late datum between those of 10:05 and 10:10, with a reading between theirs
		datum(sid, "2020-06-01T10:07:00Z", 500, 15);
		process("h");

		// THEN
		// @formatter:off
		then(agg("dh_agg_hourly", sid))
			.as("The hour has the late datum")
			.extracting("ts", "stat_i", "data_a", "read_a")
			.containsExactly(
				  tuple("2020-06-01T10:00Z", "{{13,100,500}}", "{110}", "{{110,0,110}}")
			)
			;
		then(agg("dh_agg_15m", sid))
			.as("Only the late datum's slot changes")
			.extracting("ts", "stat_i", "data_a", "read_a")
			.containsExactly(
				  tuple("2020-06-01T10:00Z", "{{4,100,500}}", "{30}", "{{20,0,20}}")
				, tuple("2020-06-01T10:15Z", "{{3,103,105}}", "{30}", "{{30,20,50}}")
				, tuple("2020-06-01T10:30Z", "{{3,106,108}}", "{30}", "{{30,50,80}}")
				, tuple("2020-06-01T10:45Z", "{{3,109,111}}", "{20}", "{{30,80,110}}")
			)
			;
		then(fifteenLocations(sid))
			.as("The other slot rows are not rewritten")
			.containsEntry("2020-06-01T10:15Z", locations.get("2020-06-01T10:15Z"))
			.containsEntry("2020-06-01T10:30Z", locations.get("2020-06-01T10:30Z"))
			.containsEntry("2020-06-01T10:45Z", locations.get("2020-06-01T10:45Z"))
			.doesNotContainEntry("2020-06-01T10:00Z", locations.get("2020-06-01T10:00Z"))
			;
		// @formatter:on
	}

	@Test
	public void fifteen_deletedDatum() {
		assume15m();

		// GIVEN
		final int sid = stream("A");
		datumEvery5m(sid, HOUR_15M, 12);
		process("h");
		final Map<String, String> locations = fifteenLocations(sid);

		// WHEN
		jdbcTemplate.update(
				sql("DELETE FROM solarhist.dh_datum WHERE stream_id = ? AND ts = ?::TIMESTAMPTZ"), sid,
				"2020-06-01T10:20:00Z");
		process("h");

		// THEN
		// @formatter:off
		then(agg("dh_agg_15m", sid))
			.as("The deleted datum's slot has one datum less, and the same accumulation and readings")
			.extracting("ts", "data_i", "stat_i", "data_a", "read_a")
			.containsExactly(
				  tuple("2020-06-01T10:00Z", "{101}", "{{3,100,102}}", "{30}", "{{20,0,20}}")
				, tuple("2020-06-01T10:15Z", "{104}", "{{2,103,105}}", "{30}", "{{30,20,50}}")
				, tuple("2020-06-01T10:30Z", "{107}", "{{3,106,108}}", "{30}", "{{30,50,80}}")
				, tuple("2020-06-01T10:45Z", "{110}", "{{3,109,111}}", "{20}", "{{30,80,110}}")
			)
			;
		then(fifteenLocations(sid))
			.as("Only the deleted datum's slot row is rewritten")
			.containsEntry("2020-06-01T10:00Z", locations.get("2020-06-01T10:00Z"))
			.containsEntry("2020-06-01T10:30Z", locations.get("2020-06-01T10:30Z"))
			.containsEntry("2020-06-01T10:45Z", locations.get("2020-06-01T10:45Z"))
			.doesNotContainEntry("2020-06-01T10:15Z", locations.get("2020-06-01T10:15Z"))
			;
		// @formatter:on
	}

	@Test
	public void fifteen_slotWithoutDatumDeleted() {
		assume15m();

		// GIVEN
		final int sid = stream("A");
		datumEvery5m(sid, HOUR_15M, 13);
		process("h");

		// WHEN
		// remove the datum of the second slot
		jdbcTemplate.update(sql("""
				DELETE FROM solarhist.dh_datum
				WHERE stream_id = ? AND ts >= ?::TIMESTAMPTZ AND ts < ?::TIMESTAMPTZ
				"""), sid, "2020-06-01T10:15:00Z", "2020-06-01T10:30:00Z");
		process("h");

		// THEN
		// @formatter:off
		then(agg("dh_agg_15m", sid))
			.filteredOn(r -> r.get("ts").toString().startsWith("2020-06-01T10:"))
			.as("""
				Slot without datum deleted; the accumulation across it goes to the slot of the \
				datum before it, and the reading difference to the slot of the datum after it""")
			.extracting("ts", "data_a", "read_a")
			.containsExactly(
				  tuple("2020-06-01T10:00Z", "{60}", "{{20,0,20}}")
				, tuple("2020-06-01T10:30Z", "{30}", "{{60,20,80}}")
				, tuple("2020-06-01T10:45Z", "{30}", "{{40,80,120}}")
			)
			;
		// @formatter:on
	}

	@Test
	public void fifteen_hourWithoutRowSlotsDeleted() {
		assume15m();

		// GIVEN
		final int sid = stream("A");
		datumEvery5m(sid, HOUR_15M, 12);
		process("h");
		then(agg("dh_agg_15m", sid)).as("Slots stored to start with").hasSize(4);

		// WHEN
		jdbcTemplate.update(sql("DELETE FROM solarhist.dh_datum WHERE stream_id = ?"), sid);
		process("h");

		// THEN
		// @formatter:off
		then(agg("dh_agg_hourly", sid))
			.as("Hour without datum deleted")
			.isEmpty()
			;
		then(agg("dh_agg_15m", sid))
			.as("All slots of the hour deleted with it")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void fifteen_retentionGuard_slotsKept() {
		assume15m();

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
		ensurePartition("dh_agg_hourly", hour);
		ensurePartition("dh_agg_15m", hour);
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_agg_hourly (stream_id, ts_start, data_a, read_a)
				VALUES (?, ?::TIMESTAMPTZ, '{2}', '{{2,1,3}}')
				"""), sid, hour);
		jdbcTemplate.update(sql("""
				INSERT INTO solarhist.dh_agg_15m (stream_id, ts_start, data_a, read_a)
				VALUES (?, ?::TIMESTAMPTZ, '{1}', '{{1,1,2}}'), (?, ?::TIMESTAMPTZ + INTERVAL 'PT30M', '{1}', '{{1,2,3}}')
				"""), sid, hour, sid, hour);
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
			.containsExactly(tuple(hour, "{2}", "{{2,1,3}}"))
			;
		then(agg("dh_agg_15m", sid))
			.as("The kept hour's slots are not computed again, nor deleted")
			.extracting("data_a", "read_a")
			.containsExactly(tuple("{1}", "{{1,1,2}}"), tuple("{1}", "{{1,2,3}}"))
			;
		// @formatter:on
	}

	@Test
	public void fifteen_partition_notCreatedForSkippedHour() {
		assume15m();

		// GIVEN
		final int sid = stream("A");
		datum(sid, "2031-01-01T00:30:00Z", 1, 100);
		// an hour before the oldest raw partition, in a year without partitions
		final String hour = jdbcTemplate.queryForObject(
				sql("""
						SELECT to_char((solarhist.dh_datum_oldest_partition_start() - INTERVAL 'PT1H') AT TIME ZONE 'UTC'
							, 'YYYY-MM-DD"T"HH24:MI"Z"')
						"""),
				String.class);
		final String part = "dh_agg_15m_" + hour.substring(0, 4);
		then(partitions("dh_agg_15m")).as("No partition for the year of the hour").extracting("name")
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
		then(partitions("dh_agg_15m"))
			.as("The hour skipped by the retention guard creates no 15 minute partition")
			.extracting("name")
			.doesNotContain(part)
			;
		// @formatter:on
	}

	@Test
	public void fifteen_partition_createdOnFirstUse() {
		assume15m();

		// GIVEN
		then(partitions("dh_agg_15m")).as("No 2031 partition to start with").extracting("name")
				.doesNotContain("dh_agg_15m_2031");
		final int sid = stream("A");
		datum(sid, "2031-03-01T10:15:00Z", 1, 100);
		datum(sid, "2031-03-01T10:45:00Z", 1, 110);

		// WHEN
		process("h");

		// THEN
		// @formatter:off
		then(partitions("dh_agg_15m"))
			.as("The 15 minute partition of the hour's UTC year is created")
			.filteredOn("name", "dh_agg_15m_2031")
			.extracting("bounds")
			.containsExactly(BOUNDS_2031)
			;
		then(partitions("dh_agg_hourly"))
			.as("With the hourly partition of the same year")
			.filteredOn("name", "dh_agg_hourly_2031")
			.extracting("bounds")
			.containsExactly(BOUNDS_2031)
			;
		then(fifteenRows(sid))
			.as("The hour's slots are stored in it")
			.extracting("part", "ts")
			.containsExactly(
				  tuple("dh_agg_15m_2031", "2031-03-01T10:15Z")
				, tuple("dh_agg_15m_2031", "2031-03-01T10:45Z")
			)
			;
		// @formatter:on
	}

	/**
	 * As {@link #partition_ownedByTableOwner()}, for the 15 minute table.
	 */
	@Test
	public void fifteen_partition_ownedByTableOwner() {
		assume15m();

		// GIVEN
		then(partitions("dh_agg_15m")).as("No 2031 partition to start with").extracting("name")
				.doesNotContain("dh_agg_15m_2031");
		final String tableOwner = jdbcTemplate.queryForObject(
				"SELECT pg_get_userbyid(relowner)::TEXT FROM pg_class WHERE oid = ?::regclass",
				String.class, SCHEMA + ".dh_agg_15m");

		// WHEN
		final List<String> warnings = jdbcTemplate.execute(
				sql("SELECT solarhist.dh_agg_ensure_partition('dh_agg_15m', ?::TIMESTAMPTZ)"),
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
		then(partitions("dh_agg_15m"))
			.as("The partition is owned by the table's owner")
			.filteredOn("name", "dh_agg_15m_2031")
			.extracting("owner")
			.containsExactly(tableOwner)
			;
		then(warnings)
			.as("No warning that the partition has another owner")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void fifteen_partition_otherTableRefused() {
		assume15m();

		// WHEN
		final Throwable t = catchThrowable(() -> ensurePartition("dh_agg_daily", "2031-06-01T00:00:00Z"));

		// THEN
		// @formatter:off
		then(t)
			.as("Only the yearly partitioned aggregate tables are accepted")
			.isInstanceOf(DataAccessException.class)
			.hasMessageContaining("Not a yearly partitioned aggregate table: dh_agg_daily")
			;
		// @formatter:on
	}

	/**
	 * New 15 minute partitions are created in the tablespace of the 15 minute
	 * table, which need not be that of the hourly table. Skipped unless the test
	 * database has a tablespace other than the default ones that the tests'
	 * role can create tables in.
	 */
	@Test
	public void fifteen_partition_inTableTablespace() {
		assume15m();
		final String space = assumeTablespace();
		assumeTrue("The %s.dh_agg_hourly table has a tablespace.".formatted(SCHEMA),
				tablespaceOf(SCHEMA + ".dh_agg_hourly") == null);

		// GIVEN
		then(partitions("dh_agg_15m")).as("No 2031 partition to start with").extracting("name")
				.doesNotContain("dh_agg_15m_2031");
		tableTablespace("dh_agg_15m", space);

		// WHEN
		ensurePartition("dh_agg_hourly", "2031-06-01T00:00:00Z");
		ensurePartition("dh_agg_15m", "2031-06-01T00:00:00Z");

		// THEN
		// @formatter:off
		then(tablespaceOf(SCHEMA + ".dh_agg_15m_2031"))
			.as("The new 15 minute partition is in the 15 minute table's tablespace")
			.isEqualTo(space)
			;
		then(tablespaceOf(SCHEMA + ".dh_agg_hourly_2031"))
			.as("The new hourly partition is in the hourly table's, the database default")
			.isNull()
			;
		// @formatter:on
	}

	/**
	 * Without a tablespace on the 15 minute table, new 15 minute partitions are
	 * created in the database default, even when the hourly table has one, if
	 * the test database has a tablespace that the tests' role can use.
	 */
	@Test
	public void fifteen_partition_inDefaultTablespace() {
		assume15m();
		assumeTrue("The %s.dh_agg_15m table has a tablespace.".formatted(SCHEMA),
				tablespaceOf(SCHEMA + ".dh_agg_15m") == null);

		// GIVEN
		then(partitions("dh_agg_15m")).as("No 2031 partition to start with").extracting("name")
				.doesNotContain("dh_agg_15m_2031");
		final List<String> spaces = jdbcTemplate.queryForList("""
				SELECT spcname::TEXT FROM pg_tablespace
				WHERE spcname NOT IN ('pg_default', 'pg_global')
					AND oid <> (SELECT dattablespace FROM pg_database WHERE datname = current_database())
					AND has_tablespace_privilege(spcname, 'CREATE')
				ORDER BY spcname
				LIMIT 1
				""", String.class);
		if ( !spaces.isEmpty() ) {
			tableTablespace("dh_agg_hourly", spaces.get(0));
		}

		// WHEN
		ensurePartition("dh_agg_hourly", "2031-06-01T00:00:00Z");
		ensurePartition("dh_agg_15m", "2031-06-01T00:00:00Z");

		// THEN
		// @formatter:off
		then(tablespaceOf(SCHEMA + ".dh_agg_15m_2031"))
			.as("The new 15 minute partition is in the database default tablespace")
			.isNull()
			;
		then(tablespaceOf(SCHEMA + ".dh_agg_hourly_2031"))
			.as("The new hourly partition is in the hourly table's tablespace")
			.isEqualTo(spaces.isEmpty() ? null : spaces.get(0))
			;
		// @formatter:on
	}

	@Test
	public void fifteen_partition_acrossYearBoundary() {
		assume15m();

		// GIVEN
		final int sid = stream("A");
		// a datum every 5 minutes from 23:30 on 31 December 2031 to 00:30 on 1 January 2032
		jdbcTemplate.queryForList(sql("SELECT solarhist.dh_datum_ensure_partition(?::TIMESTAMPTZ)"),
				"2032-01-01T00:00:00Z");
		datumEvery5m(sid, "2031-12-31T23:30:00Z", 13);

		// WHEN
		process("h");

		// THEN
		// @formatter:off
		then(fifteenRows(sid))
			.as("Each slot is stored in the partition of its UTC year")
			.extracting("part", "ts")
			.containsExactly(
				  tuple("dh_agg_15m_2031", "2031-12-31T23:30Z")
				, tuple("dh_agg_15m_2031", "2031-12-31T23:45Z")
				, tuple("dh_agg_15m_2032", "2032-01-01T00:00Z")
				, tuple("dh_agg_15m_2032", "2032-01-01T00:15Z")
				, tuple("dh_agg_15m_2032", "2032-01-01T00:30Z")
			)
			;
		then(agg("dh_agg_15m", sid))
			.as("""
				The accumulation and reading difference to the datum at the year boundary go to \
				the last slot of 2031""")
			.extracting("ts", "data_a", "read_a")
			.containsExactly(
				  tuple("2031-12-31T23:30Z", "{30}", "{{20,0,20}}")
				, tuple("2031-12-31T23:45Z", "{30}", "{{40,20,60}}")
				, tuple("2032-01-01T00:00Z", "{30}", "{{20,60,80}}")
				, tuple("2032-01-01T00:15Z", "{30}", "{{30,80,110}}")
				, tuple("2032-01-01T00:30Z", "{0}", "{{10,110,120}}")
			)
			;
		then(hourlyRows(sid))
			.as("The hours are stored in the hourly partitions of their UTC years")
			.extracting("part", "ts")
			.containsExactly(
				  tuple("dh_agg_hourly_2031", "2031-12-31T23:00Z")
				, tuple("dh_agg_hourly_2032", "2032-01-01T00:00Z")
			)
			;
		// @formatter:on
	}

	/*
	 * The concurrent tests use connections of their own, outside the test
	 * transaction, so they commit and then delete what they stored and drop the
	 * partitions they create.
	 */

	private static final String FUTURE_15M_PART = "dh_agg_15m_2190";
	private static final String FUTURE_DATUM_PART = "dh_datum_2190_06";

	/** Count the committed partitions of a table with a given name. */
	private static int partitionCount(Connection con, String table, String name) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("""
				SELECT count(*)
				FROM pg_inherits i
				INNER JOIN pg_class c ON c.oid = i.inhrelid
				WHERE i.inhparent = ?::regclass AND c.relname = ?
				""")) {
			stmt.setString(1, SCHEMA + "." + table);
			stmt.setString(2, name);
			try (ResultSet rs = stmt.executeQuery()) {
				rs.next();
				return rs.getInt(1);
			}
		}
	}

	/** Process one stale hour, returning its stream ID or {@code null}. */
	private static Integer processOneHour(Connection con) throws SQLException {
		try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(
				"SELECT stream_id FROM %s.dh_process_one_agg_stale('h')".formatted(SCHEMA))) {
			return (rs.next() ? rs.getInt(1) : null);
		}
	}

	/**
	 * Wait until a backend waits for a lock, for up to 10 seconds, failing at
	 * once with the call's own error if the call it makes ends first.
	 */
	private static void awaitLockWait(Connection con, int pid, Future<?> call) throws Exception {
		final long end = System.currentTimeMillis() + 10_000L;
		try (PreparedStatement stmt = con
				.prepareStatement("SELECT wait_event_type = 'Lock' FROM pg_stat_activity WHERE pid = ?")) {
			stmt.setInt(1, pid);
			while ( System.currentTimeMillis() < end ) {
				if ( call.isDone() ) {
					call.get();
					throw new AssertionError("Backend %d ended without waiting for a lock.".formatted(pid));
				}
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

	/**
	 * Commit two streams with a stale hour each in 2190, a year without
	 * partitions.
	 */
	private static List<Integer> futureStreams(JdbcTemplate committed) {
		committed.queryForList(
				"SELECT %s.dh_datum_ensure_partition('2190-06-01T00:00:00Z')".formatted(SCHEMA));
		final List<Integer> result = new ArrayList<>(2);
		for ( String source : List.of("concurrent/A", "concurrent/B") ) {
			final Integer sid = committed.queryForObject("""
					INSERT INTO %s.dh_datum_meta (node_id, source_id, names_i, names_a, type_ids)
					VALUES (?, ?, '{i}', '{val}', '{1}')
					RETURNING stream_id
					""".formatted(SCHEMA), Integer.class, TEST_NODE_ID, source);
			result.add(sid);
			committed.update("""
					INSERT INTO %s.dh_datum (ts, stream_id, type_id, data_i, data_a)
					VALUES ('2190-06-01T10:15:00Z', ?, 1, '{1}', '{100}'), ('2190-06-01T10:45:00Z', ?, 1, '{2}', '{110}')
					""".formatted(SCHEMA), sid, sid);
		}
		return result;
	}

	/**
	 * Get a JDBC template that uses a connection of its own, in auto-commit
	 * mode, not the test transaction that a template of
	 * {@link #dataSource()} joins.
	 */
	private static JdbcTemplate committed(Connection con) throws SQLException {
		con.setAutoCommit(true);
		return new JdbcTemplate(new SingleConnectionDataSource(con, true));
	}

	/** Delete what the concurrent tests committed. */
	private static void deleteFutureStreams() throws SQLException {
		try (Connection con = dataSource().getConnection()) {
			deleteFutureStreams(committed(con));
		}
	}

	private static void deleteFutureStreams(JdbcTemplate committed) {
		final String sids = "SELECT stream_id FROM %s.dh_datum_meta WHERE node_id = %d AND source_id LIKE 'concurrent/%%'"
				.formatted(SCHEMA, TEST_NODE_ID);
		committed.update("DELETE FROM %s.dh_datum WHERE stream_id IN (%s)".formatted(SCHEMA, sids));
		for ( String table : List.of("dh_agg_stale", "dh_agg_15m", "dh_agg_hourly", "dh_agg_daily",
				"dh_agg_monthly", "dh_datum_meta") ) {
			committed.update("DELETE FROM %s.%s WHERE stream_id IN (%s)".formatted(SCHEMA, table, sids));
		}
		for ( String part : List.of(FUTURE_15M_PART, FUTURE_PART, FUTURE_DATUM_PART) ) {
			committed.update("DROP TABLE IF EXISTS %s.%s".formatted(SCHEMA, part));
		}
	}

	/**
	 * Process the first stale hours of a new year in two transactions at once:
	 * the second waits for the first to create the year's partitions, and then
	 * either finds them or, if the first rolled back, creates them itself.
	 */
	private void concurrentFirstUse(boolean commitFirst) throws Exception {
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		try (Connection first = dataSource().getConnection();
				Connection second = dataSource().getConnection();
				Connection observer = dataSource().getConnection()) {
			// GIVEN
			final List<Integer> sids = futureStreams(committed(observer));
			then(partitionCount(observer, "dh_agg_15m", FUTURE_15M_PART))
					.as("No 15 minute partition to start with").isZero();
			then(partitionCount(observer, "dh_agg_hourly", FUTURE_PART))
					.as("No hourly partition to start with").isZero();
			first.setAutoCommit(false);
			second.setAutoCommit(false);
			final int secondPid = backendPid(second);

			// WHEN
			final Integer firstSid = processOneHour(first);
			final Future<Integer> secondCall = executor.submit(() -> processOneHour(second));
			awaitLockWait(observer, secondPid, secondCall);
			if ( commitFirst ) {
				first.commit();
			} else {
				first.rollback();
			}
			final Integer secondSid = secondCall.get(10L, TimeUnit.SECONDS);
			second.commit();

			// THEN
			// @formatter:off
			then(List.of(firstSid, secondSid))
				.as("Each transaction processes the hour of one of the streams")
				.containsExactlyInAnyOrderElementsOf(sids)
				;
			then(partitionCount(observer, "dh_agg_hourly", FUTURE_PART))
				.as("One hourly partition, without an error")
				.isOne()
				;
			then(partitionCount(observer, "dh_agg_15m", FUTURE_15M_PART))
				.as("One 15 minute partition, without an error")
				.isOne()
				;
			try (PreparedStatement stmt = observer.prepareStatement("""
					SELECT (SELECT count(*) FROM %1$s.dh_agg_hourly WHERE stream_id = ANY(?))
						, (SELECT count(*) FROM %1$s.dh_agg_15m WHERE stream_id = ANY(?))
					""".formatted(SCHEMA))) {
				final java.sql.Array ids = observer.createArrayOf("integer", sids.toArray());
				stmt.setArray(1, ids);
				stmt.setArray(2, ids);
				try (ResultSet rs = stmt.executeQuery()) {
					rs.next();
					then(rs.getInt(1))
						.as("The hours of the transactions that committed are stored")
						.isEqualTo(commitFirst ? 2 : 1)
						;
					then(rs.getInt(2))
						.as("With their two slots each")
						.isEqualTo(commitFirst ? 4 : 2)
						;
				}
			}
			// @formatter:on
		} finally {
			executor.shutdownNow();
			deleteFutureStreams();
		}
	}

	@Test
	public void fifteen_partition_concurrentFirstUse() throws Exception {
		assume15m();
		concurrentFirstUse(true);
	}

	@Test
	public void fifteen_partition_concurrentFirstUse_firstRolledBack() throws Exception {
		assume15m();
		concurrentFirstUse(false);
	}

}

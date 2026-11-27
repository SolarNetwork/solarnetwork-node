/* ============================================================================
 * SQL script to add stored 15 minute, Hour, Day and Month aggregates to the
 * JDBC DatumHistorian. Run it after postgres-init.sql, in the same schema.
 *
 * Aggregate support is chosen when a historian schema is set up: run this
 * script then or not at all. Like postgres-init.sql it uses the solarhist
 * schema, and it is applied to another schema such as solaruhist in the same
 * way.
 *
 * This is a port of SolarNet's stale-queue design: the solardatm aggregate
 * tables and rollup functions, with integer stream IDs, without SolarNet's
 * Reset auxiliary records, audit and SolarFlux parts. SolarNet's query SQL can
 * therefore be ported to read these tables. The rollup functions follow
 * SolarNet's as of NET-539 and NET-544, so they give SolarNet's results for
 * the same datum.
 *
 * - The dh_agg_15m, dh_agg_hourly, dh_agg_daily and dh_agg_monthly tables have
 *   the same columns as SolarNet's agg_datm_15m, agg_datm_hourly,
 *   agg_datm_daily and agg_datm_monthly tables, except that data_s is a JSON
 *   array, like solarhist.dh_datum.data_s.
 * - The 15 minute rows of an hour are computed with the hour, from the same
 *   datum, when the hour is processed, so they add up to it.
 * - The dh_agg_15m and dh_agg_hourly tables are partitioned by UTC year and
 *   manage their own partitions: solarhist.dh_agg_ensure_partition() creates
 *   the partition of a year when the first hour in it is stored, in the
 *   table's tablespace, and gives it the table's owner whichever role stored
 *   the hour. Partitions are never dropped automatically. The daily and
 *   monthly tables are small enough not to be partitioned.
 * - A trigger on solarhist.dh_datum adds the hours a stored, changed or
 *   deleted datum affects to the dh_agg_stale queue.
 * - The solarhist.dh_process_agg_stale() procedure drains the queue: hours,
 *   then the days they are in, then the months. The postgres-init-agg-cron.sql
 *   script schedules it with pg_cron.
 * - Hours are UTC-aligned. Days and months start at midnight in the time zone
 *   of the stream's node, from the dh_node_time_zone table of postgres-init.sql,
 *   falling back to UTC. A trigger on that table has the days and months of a
 *   node computed again when its zone changes.
 * - Raw datum partitions may be dropped while the aggregates are kept: an hour
 *   that starts before the oldest solarhist.dh_datum partition is never
 *   recomputed. Drop a partition with DROP TABLE, or detach it without
 *   CONCURRENTLY: either waits for an hour being computed to commit.
 * - The historian reads 15 minute aggregates from dh_agg_15m, and combines
 *   pairs of its rows into 30 minute aggregates when queried. The other
 *   sub-hour aggregates (5 and 10 minute slots) are computed from raw datum
 *   when queried, by solarhist.dh_rollup_datum_for_time_span_slots(), and
 *   solarhist.dh_count_datum_time_span_slots() counts them, like SolarNet's
 *   functions of the same purpose. Slots are bucketed with date_bin(), so
 *   TimescaleDB is not needed.
 * - It replaces solarhist.dh_find_datum_diff_near_rows() from postgres-init.sql
 *   with a version that falls back to hourly readings where raw datum have
 *   been dropped, for Difference reading queries without aggregation.
 *
 * Requires the aggs_for_vecs extension, created by a superuser in a schema on
 * the search path of the roles using these functions (normally public):
 *
 * 	CREATE EXTENSION IF NOT EXISTS aggs_for_vecs;
 *
 * Run this as the same role as postgres-init.sql.
 * ============================================================================
 */

DO $$
BEGIN
	IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'aggs_for_vecs') THEN
		RAISE EXCEPTION 'The aggs_for_vecs extension is required.'
		USING HINT = 'As a superuser, run: CREATE EXTENSION aggs_for_vecs;';
	END IF;
END
$$;


/* ============================================================================
 * Tables
 * ============================================================================
 */

-- aggregate row type used by the rollup functions regardless of physical table; like the
-- SolarNet solardatm.agg_datm type
CREATE TYPE solarhist.dh_agg_datum AS (
	stream_id	INTEGER,
	ts_start	TIMESTAMP WITH TIME ZONE,
	data_i		NUMERIC[],
	data_a		NUMERIC[],
	data_s		JSONB,
	data_t		TEXT[],
	stat_i		NUMERIC[][],
	read_a		NUMERIC[][]
);

-- hourly aggregates, one row per stream and UTC hour that has datum; the same columns as the
-- SolarNet solardatm.agg_datm_hourly table:
--
-- data_i	mean of each instantaneous property
-- stat_i	[count, minimum, maximum] of each instantaneous property
-- data_a	clock accumulation of each accumulating property: the sum of the differences between
-- 			consecutive readings, a difference that crosses the start or end of the hour split
-- 			by time, and a difference between readings more than an hour apart ignored
-- read_a	[difference, start, end] of each accumulating property: the last reading at or
-- 			before the end of the hour, minus the last reading at or before its start
-- data_s	JSON array of the most frequent value of each status property
-- data_t	the distinct tags
--
-- The table is partitioned by UTC year, so that each table and index stays bounded as history
-- grows: only the current year's partition has the churn of the hours being computed again,
-- and the partitions of past years become static. The yearly partitions are created by
-- solarhist.dh_agg_ensure_partition() when an hour is first stored in them, and are
-- never dropped or detached automatically. Each is given the table's owner, although the
-- pg_cron job that creates it runs as a superuser. There is no DEFAULT partition, for the same
-- reason as solarhist.dh_datum has none.
CREATE TABLE solarhist.dh_agg_hourly (
	stream_id	INTEGER NOT NULL,			-- dh_datum_meta.stream_id
	ts_start	TIMESTAMP WITH TIME ZONE NOT NULL,
	data_i		NUMERIC[],
	data_a		NUMERIC[],
	data_s		JSONB,
	data_t		TEXT[],
	stat_i		NUMERIC[][],
	read_a		NUMERIC[][],
	CONSTRAINT dh_agg_hourly_pk PRIMARY KEY (stream_id, ts_start)
) PARTITION BY RANGE (ts_start);

-- 15 minute aggregates: for each row of dh_agg_hourly, one row per 15 minute slot of that UTC
-- hour that holds a datum or is given part of the hour's accumulation or readings, so one to
-- four rows; the same columns as dh_agg_hourly, and as the SolarNet solardatm.agg_datm_15m table
--
-- The rows of an hour are computed together with the hour, from the same datum, by
-- solarhist.dh_rollup_datum_15m_for_hour() when solarhist.dh_process_one_agg_stale() processes
-- the hour, so they add up to it. They are stored, changed and deleted only with their hour: an
-- unchanged row is not rewritten, and a slot the hour no longer has is deleted. Each slot is a
-- quarter of a UTC hour, so the time zone of the stream plays no part.
--
-- The table is partitioned by UTC year, like dh_agg_hourly and for the same reasons. Its
-- partitions are created by solarhist.dh_agg_ensure_partition() with those of dh_agg_hourly,
-- named like dh_agg_15m_2026, given the table's owner, and never dropped or detached
-- automatically. There is no DEFAULT partition.
CREATE TABLE solarhist.dh_agg_15m (
	stream_id	INTEGER NOT NULL,			-- dh_datum_meta.stream_id
	ts_start	TIMESTAMP WITH TIME ZONE NOT NULL,
	data_i		NUMERIC[],
	data_a		NUMERIC[],
	data_s		JSONB,
	data_t		TEXT[],
	stat_i		NUMERIC[][],
	read_a		NUMERIC[][],
	CONSTRAINT dh_agg_15m_pk PRIMARY KEY (stream_id, ts_start)
) PARTITION BY RANGE (ts_start);

-- daily aggregates, rolled up from the hourly rows, by day in the node's time zone
CREATE TABLE solarhist.dh_agg_daily (
	stream_id	INTEGER NOT NULL,			-- dh_datum_meta.stream_id
	ts_start	TIMESTAMP WITH TIME ZONE NOT NULL,
	data_i		NUMERIC[],
	data_a		NUMERIC[],
	data_s		JSONB,
	data_t		TEXT[],
	stat_i		NUMERIC[][],
	read_a		NUMERIC[][],
	CONSTRAINT dh_agg_daily_pk PRIMARY KEY (stream_id, ts_start)
);

-- monthly aggregates, rolled up from the daily rows, by month in the node's time zone
CREATE TABLE solarhist.dh_agg_monthly (
	stream_id	INTEGER NOT NULL,			-- dh_datum_meta.stream_id
	ts_start	TIMESTAMP WITH TIME ZONE NOT NULL,
	data_i		NUMERIC[],
	data_a		NUMERIC[],
	data_s		JSONB,
	data_t		TEXT[],
	stat_i		NUMERIC[][],
	read_a		NUMERIC[][],
	CONSTRAINT dh_agg_monthly_pk PRIMARY KEY (stream_id, ts_start)
);

-- queue of aggregates to compute; the same as the SolarNet solardatm.agg_stale_datm table
--
-- agg_kind is h for an hour, d for a day, M for a month; ts_start is the start of that period
CREATE TABLE solarhist.dh_agg_stale (
	stream_id	INTEGER NOT NULL,			-- dh_datum_meta.stream_id
	ts_start	TIMESTAMP WITH TIME ZONE NOT NULL,
	agg_kind	CHARACTER NOT NULL,
	created		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT dh_agg_stale_pk PRIMARY KEY (agg_kind, ts_start, stream_id)
);


/* ============================================================================
 * Helpers
 * ============================================================================
 */

/**
 * State transition function of the `solarhist.first()` aggregate: keep the first non-NULL
 * value.
 *
 * Copied from the SolarNet solarcommon.first_sfunc() function.
 */
CREATE OR REPLACE FUNCTION solarhist.first_sfunc(anyelement, anyelement)
	RETURNS anyelement LANGUAGE SQL IMMUTABLE STRICT AS
$$
	SELECT $1;
$$;

/**
 * Aggregate that returns the first non-NULL value in the group; use it with `ORDER BY`.
 *
 * Copied from the SolarNet solarcommon.first() aggregate.
 */
CREATE AGGREGATE solarhist.first(
	sfunc		= solarhist.first_sfunc,
	basetype	= anyelement,
	stype		= anyelement
);


/**
 * Transpose a two-dimensional array.
 *
 * Copied from the SolarNet solarcommon.array_transpose2d() function.
 *
 * @param arr the array to transpose, with two dimensions
 * @returns the transposed array
 */
CREATE OR REPLACE FUNCTION solarhist.array_transpose2d(arr anyarray)
	RETURNS anyarray LANGUAGE SQL IMMUTABLE AS
$$
	SELECT array_agg(
		(SELECT array_agg(arr[i][j] ORDER BY i) FROM generate_subscripts(arr, 1) AS i)
		ORDER BY j
	)
	FROM generate_subscripts(arr, 2) AS j
$$;


/**
 * Get the time zone of a stream: that of its node, from `solarhist.dh_node_time_zone`, or UTC.
 *
 * @param sid the stream ID
 * @returns the time zone name
 */
CREATE OR REPLACE FUNCTION solarhist.dh_stream_time_zone(sid INTEGER)
	RETURNS TEXT LANGUAGE SQL STABLE AS
$$
	SELECT COALESCE(
		(
			SELECT z.time_zone
			FROM solarhist.dh_datum_meta m
			INNER JOIN solarhist.dh_node_time_zone z ON z.node_id = m.node_id
			WHERE m.stream_id = sid
		),
		'UTC'
	)
$$;


/**
 * Create the yearly partition of `solarhist.dh_agg_hourly` or `solarhist.dh_agg_15m` that holds
 * a given date, if it does not already exist.
 *
 * Partitions cover one UTC year and are named after their table and year, like
 * `solarhist.dh_agg_hourly_2026` and `solarhist.dh_agg_15m_2026`. The bounds are computed in UTC,
 * so they do not depend on the session's `TimeZone`. As with
 * `solarhist.dh_datum_ensure_partition()`, a new partition is created as a table of its own and
 * then attached, because that does not block other transactions from reading and writing the
 * table while it happens. Also as there, a new partition is created in the tablespace of the
 * table it is a partition of, if that table has one; the two tables need not have the same
 * tablespace.
 *
 * A new partition is given the owner of its table, so it does not matter which role creates it.
 * The `pg_cron` job of postgres-init-agg-cron.sql runs as a superuser such as `postgres`, and the
 * table's owner could neither read a partition owned by that role directly nor alter the table
 * in a way that recurses to its partitions, as adding a column does. Nothing more is done when
 * the table's owner creates the partition. A role that can create a partition but not give it
 * away, such as a member of the owning role without the SET option, keeps it as its own, with a
 * warning, rather than failing. A partition needs no privileges of its own: reads and writes
 * through its table are checked against the table's privileges only.
 *
 * `solarhist.dh_process_one_agg_stale()` calls this for both tables, the hourly one first, for
 * each hour it stores, so the partitions of a year are created, and committed, with the first
 * hour stored in it, whether that year is the current one or a past one. One function serves
 * both tables so that they are partitioned in exactly the same way.
 *
 * @param tbl 	the table: `dh_agg_hourly` or `dh_agg_15m`, without the schema
 * @param ts_at the date to ensure a partition exists for
 * @see solarhist.dh_agg_hourly_ensure_partition()
 */
CREATE OR REPLACE FUNCTION solarhist.dh_agg_ensure_partition(tbl TEXT, ts_at TIMESTAMP WITH TIME ZONE)
	RETURNS VOID LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ts_start	CONSTANT TIMESTAMP WITH TIME ZONE := date_trunc('year', ts_at, 'UTC');
	ts_end		CONSTANT TIMESTAMP WITH TIME ZONE := date_add(ts_start, INTERVAL 'P1Y', 'UTC');
	part_name	CONSTANT TEXT := tbl || '_' || to_char(ts_start AT TIME ZONE 'UTC', 'YYYY');
	table_owner	OID;
	part_owner	OID;
	part_space	TEXT;
BEGIN
	IF tbl IS NULL OR tbl NOT IN ('dh_agg_hourly', 'dh_agg_15m') THEN
		RAISE EXCEPTION 'Not a yearly partitioned aggregate table: %', tbl
		USING ERRCODE = 'invalid_parameter_value',
			HINT = 'Use dh_agg_hourly or dh_agg_15m.';
	END IF;

	IF to_regclass('solarhist.' || part_name) IS NOT NULL THEN
		RETURN;
	END IF;

	-- let one caller at a time past here; this is the same lock ATTACH PARTITION takes
	EXECUTE format('LOCK TABLE ONLY solarhist.%I IN SHARE UPDATE EXCLUSIVE MODE', tbl);

	-- look again, as another caller could have created the partition while this one waited
	IF to_regclass('solarhist.' || part_name) IS NULL THEN
		-- use the tablespace of the table, like CREATE TABLE ... PARTITION OF does; NULL
		-- (the database default) unless the table was given one
		SELECT s.spcname FROM pg_class c JOIN pg_tablespace s ON s.oid = c.reltablespace
		WHERE c.oid = ('solarhist.' || tbl)::regclass
		INTO part_space;

		EXECUTE format(
			'CREATE TABLE solarhist.%I (LIKE solarhist.%I INCLUDING DEFAULTS INCLUDING CONSTRAINTS)%s'
			, part_name, tbl, CASE WHEN part_space IS NULL THEN '' ELSE format(' TABLESPACE %I', part_space) END);
		EXECUTE format(
			'ALTER TABLE solarhist.%I ATTACH PARTITION solarhist.%I FOR VALUES FROM (%L) TO (%L)'
			, tbl, part_name, ts_start, ts_end);

		-- give the partition the table's owner, when another role created it
		SELECT t.relowner, p.relowner
		FROM pg_class t, pg_class p
		WHERE t.oid = ('solarhist.' || tbl)::regclass
			AND p.oid = to_regclass('solarhist.' || part_name)
		INTO table_owner, part_owner;

		IF part_owner <> table_owner THEN
			BEGIN
				EXECUTE format('ALTER TABLE solarhist.%I OWNER TO %s', part_name, table_owner::regrole);
			EXCEPTION WHEN insufficient_privilege THEN
				RAISE WARNING 'Partition solarhist.% is owned by %, not by the table owner %: %'
					, part_name, part_owner::regrole, table_owner::regrole, SQLERRM;
			END;
		END IF;
	END IF;
END
$$;


/**
 * Create the yearly `solarhist.dh_agg_hourly` partition that holds a given date, if it does
 * not already exist: `solarhist.dh_agg_ensure_partition()` for that table.
 *
 * @param ts_at the date to ensure a partition exists for
 * @see solarhist.dh_agg_ensure_partition()
 */
CREATE OR REPLACE FUNCTION solarhist.dh_agg_hourly_ensure_partition(ts_at TIMESTAMP WITH TIME ZONE)
	RETURNS VOID LANGUAGE SQL VOLATILE AS
$$
	SELECT solarhist.dh_agg_ensure_partition('dh_agg_hourly', ts_at)
$$;


/**
 * Get the start of the oldest `solarhist.dh_datum` partition.
 *
 * Hours before this have no raw datum to compute them from, because their partitions have
 * been dropped (or never existed), so `solarhist.dh_process_one_agg_stale()` leaves them as
 * they are. A partition left pending detach, by an interrupted `DETACH PARTITION ...
 * CONCURRENTLY`, is not counted, as queries no longer read it.
 *
 * @returns the lower bound of the oldest partition, or `NULL` if there are no partitions
 */
CREATE OR REPLACE FUNCTION solarhist.dh_datum_oldest_partition_start()
	RETURNS TIMESTAMP WITH TIME ZONE LANGUAGE SQL STABLE AS
$$
	-- bounds look like: FOR VALUES FROM ('2026-11-01 13:00:00+13') TO ('2026-12-01 13:00:00+13')
	SELECT min(substring(pg_get_expr(c.relpartbound, c.oid) FROM $re$FROM \('([^']+)'\)$re$)::TIMESTAMP WITH TIME ZONE)
	FROM pg_inherits i
	INNER JOIN pg_class c ON c.oid = i.inhrelid
	WHERE i.inhparent = 'solarhist.dh_datum'::regclass
		AND NOT i.inhdetachpending
$$;


/* ============================================================================
 * Stale marking
 * ============================================================================
 */

/**
 * Calculate the hours that must be computed again after a datum has changed.
 *
 * This returns up to four hours: that of the previous datum, that of the changed datum, the
 * hour before if the datum is exactly on the hour and has a previous datum, and that of the
 * next datum (or the hour before it, if the next datum is exactly on the hour). Call it after
 * the change, so the changed datum itself is not taken as its own neighbour. Ported from the
 * SolarNet solardatm.calc_stale_datm() function.
 *
 * @param sid 		the stream ID of the datum that has changed (inserted, updated, deleted)
 * @param ts_in		the time of the datum that has changed
 * @param tolerance the maximum time to look before and after for neighbouring datum
 * @returns the stream ID and start of each hour
 */
CREATE OR REPLACE FUNCTION solarhist.dh_calc_stale_datum(
		sid 		INTEGER,
		ts_in 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT INTERVAL 'P3M'
	) RETURNS TABLE (
		stream_id	INTEGER,
		ts_start 	TIMESTAMP WITH TIME ZONE
	) LANGUAGE SQL STABLE ROWS 4 AS
$$
	WITH prev_datum_hour AS (
		SELECT date_trunc('hour', d.ts, 'UTC') AS ts
		FROM solarhist.dh_datum d
		WHERE d.stream_id = sid
			AND d.ts < ts_in
			AND d.ts > ts_in - tolerance
		ORDER BY d.stream_id, d.ts DESC
		LIMIT 1
	)
	, b AS (
		-- previous datum hour
		(
			SELECT ts FROM prev_datum_hour
		)
		-- this hour
		UNION ALL
		(
			SELECT date_trunc('hour', ts_in, 'UTC') AS ts
		)
		-- previous hour, if the datum is exactly on the hour and there is a previous datum
		UNION ALL
		(
			SELECT date_trunc('hour', ts_in, 'UTC') - INTERVAL 'PT1H' AS ts
			FROM prev_datum_hour
			WHERE date_trunc('hour', ts_in, 'UTC') = ts_in
		)
		-- next datum hour, or the hour before it if the next datum is exactly on the hour
		UNION ALL
		(
			SELECT CASE WHEN date_trunc('hour', d.ts, 'UTC') = d.ts
				THEN d.ts - INTERVAL 'PT1H'
				ELSE date_trunc('hour', d.ts, 'UTC')
				END AS ts
			FROM solarhist.dh_datum d
			WHERE d.stream_id = sid
				AND d.ts > ts_in
				AND d.ts < ts_in + tolerance
			ORDER BY d.stream_id, d.ts
			LIMIT 1
		)
	)
	SELECT DISTINCT ON (ts) sid, ts FROM b
$$;


/**
 * Trigger function that adds the hours affected by a stored, changed or deleted datum to the
 * `solarhist.dh_agg_stale` queue.
 *
 * It takes a shared advisory lock on the stream until the transaction ends. The stale
 * processing takes the same lock exclusively before computing an hour of the stream, so an
 * hour is never computed while a datum that marked it stale is still uncommitted; otherwise
 * that datum would be missed and its stale row already gone.
 *
 * The lock cannot deadlock with the processing as long as each datum is stored in a
 * transaction of its own, as `solarhist.dh_store_datum()` requires: the shared lock is the
 * last lock such a transaction takes before it commits, apart from its inserts into the queue,
 * which `ON CONFLICT DO NOTHING` makes without waiting on the processing's row lock.
 */
CREATE OR REPLACE FUNCTION solarhist.dh_datum_mark_stale()
	RETURNS TRIGGER LANGUAGE plpgsql VOLATILE AS
$$
BEGIN
	IF TG_OP IN ('INSERT', 'UPDATE') THEN
		PERFORM pg_advisory_xact_lock_shared(hashtext('solarhist.dh_agg_stale'), NEW.stream_id);

		INSERT INTO solarhist.dh_agg_stale (stream_id, ts_start, agg_kind)
		SELECT s.stream_id, s.ts_start, 'h'
		FROM solarhist.dh_calc_stale_datum(NEW.stream_id, NEW.ts) s
		ON CONFLICT (agg_kind, ts_start, stream_id) DO NOTHING;
	END IF;

	IF TG_OP = 'DELETE' OR (TG_OP = 'UPDATE'
			AND (OLD.stream_id, OLD.ts) IS DISTINCT FROM (NEW.stream_id, NEW.ts)) THEN
		PERFORM pg_advisory_xact_lock_shared(hashtext('solarhist.dh_agg_stale'), OLD.stream_id);

		INSERT INTO solarhist.dh_agg_stale (stream_id, ts_start, agg_kind)
		SELECT s.stream_id, s.ts_start, 'h'
		FROM solarhist.dh_calc_stale_datum(OLD.stream_id, OLD.ts) s
		ON CONFLICT (agg_kind, ts_start, stream_id) DO NOTHING;
	END IF;

	RETURN NULL;
END
$$;

-- a row trigger on the partitioned table is cloned onto every partition, including those
-- solarhist.dh_datum_ensure_partition() attaches later; dropping or detaching a partition
-- fires nothing, so its aggregates are kept
CREATE TRIGGER dh_datum_mark_stale
	AFTER INSERT OR UPDATE OR DELETE ON solarhist.dh_datum
	FOR EACH ROW EXECUTE FUNCTION solarhist.dh_datum_mark_stale();


/**
 * Trigger function that marks the days of a node's streams stale when its time zone is added,
 * changed or removed, so they are computed again from the hourly rows in the new zone. Days
 * and months in the old zone are deleted as the new ones are computed. An invalid zone is
 * rejected.
 */
CREATE OR REPLACE FUNCTION solarhist.dh_node_time_zone_mark_stale()
	RETURNS TRIGGER LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	node	BIGINT	:= CASE TG_OP WHEN 'DELETE' THEN OLD.node_id ELSE NEW.node_id END;
	tz		TEXT	:= CASE TG_OP WHEN 'DELETE' THEN 'UTC' ELSE NEW.time_zone END;
BEGIN
	-- reject an invalid zone
	PERFORM CURRENT_TIMESTAMP AT TIME ZONE tz;

	IF TG_OP = 'UPDATE' AND OLD.time_zone = NEW.time_zone THEN
		RETURN NULL;
	END IF;

	INSERT INTO solarhist.dh_agg_stale (stream_id, ts_start, agg_kind)
	SELECT DISTINCT h.stream_id, date_trunc('day', h.ts_start AT TIME ZONE tz) AT TIME ZONE tz, 'd'
	FROM solarhist.dh_datum_meta m
	INNER JOIN solarhist.dh_agg_hourly h ON h.stream_id = m.stream_id
	WHERE m.node_id = node
	ON CONFLICT (agg_kind, ts_start, stream_id) DO NOTHING;

	RETURN NULL;
END
$$;

CREATE TRIGGER dh_node_time_zone_mark_stale
	AFTER INSERT OR UPDATE OR DELETE ON solarhist.dh_node_time_zone
	FOR EACH ROW EXECUTE FUNCTION solarhist.dh_node_time_zone_mark_stale();


/* ============================================================================
 * Hourly rollup
 * ============================================================================
 */

/**
 * Find the datum needed to compute the aggregate of a time slot: those within it, plus the
 * datum with accumulating values immediately before it and immediately after it.
 *
 * The datum before supplies the starting reading and the share of the clock accumulation
 * that crosses the start of the slot. It is looked for within 14 days requiring accumulating
 * values, and then up to the full tolerance without requiring them, because the latter can
 * use an index-only scan. If the datum found that way has no accumulating values, the latest
 * datum that has them within the 14 days before it, and within the tolerance, is used instead
 * (NET-538).
 *
 * The datum after supplies only the share of the clock accumulation that crosses the end of
 * the slot, which is ignored when the two readings are more than an hour apart, so it is
 * looked for no more than an hour past the end, requiring accumulating values (NET-537).
 *
 * The datum before is looked for only when something returned can be differenced against it:
 * a datum with accumulating values in the slot, counting one exactly at the end, or the datum
 * after the slot. Otherwise only the datum in the slot are returned, as when the search finds
 * nothing (NET-539). The search reads every row back to the datum it finds, so for a stream
 * that declares accumulating properties but has posted none for 14 days it read up to 28 days
 * of rows on every call, and found nothing that changed the result. SolarNet's function also
 * looks for it when a Reset record is in the slot; the node has no Reset records.
 *
 * A datum exactly at the end of the slot is included, and then no datum after it is looked
 * for. A datum exactly at the start of the slot with accumulating values is the starting
 * reading, and then no datum before it is looked for. If the slot has no datum, nothing is
 * returned.
 *
 * Ported from the SolarNet solardatm.find_datm_for_time_slot() function, as of NET-539,
 * without Reset records. Without them SolarNet's target_agg parameter has no effect, so it is
 * left out.
 *
 * As with SolarNet's function from NET-538, it is not declared STRICT, so the planner can
 * inline it into the calling query and prune the `solarhist.dh_datum` partitions while
 * planning. A STRICT set-returning SQL function is planned again on every call without its
 * argument values, considering every partition: with 131 partitions that took 2 ms a call,
 * against 0.1 ms inlined. A NULL argument still gives no rows.
 *
 * @param sid 		the stream ID
 * @param start_ts	the start of the slot (inclusive)
 * @param end_ts 	the end of the slot (exclusive, but see above)
 * @param tolerance the maximum time to look back for the datum before the slot
 * @returns the datum
 */
CREATE OR REPLACE FUNCTION solarhist.dh_find_datum_for_time_slot(
		sid 		INTEGER,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT INTERVAL 'P3M'
	) RETURNS SETOF solarhist.dh_datum_rec LANGUAGE SQL STABLE ROWS 200 AS
$$
	-- find if the stream has accumulating properties at all, to avoid a costly scan
	WITH meta AS (
		SELECT COALESCE(CARDINALITY(names_a) = 0, TRUE) AS has_no_a
		FROM solarhist.dh_datum_meta
		WHERE stream_id = sid
	)
	-- the first and last datum within the slot, with an inclusive end so that a datum exactly
	-- at the end is found (NET-469); a single row of NULL values if there are none
	, drange AS (
		SELECT *
		FROM (
			-- force a result row
			VALUES (TRUE)
		) AS c(t)
		LEFT OUTER JOIN (
			SELECT ts, COALESCE(CARDINALITY(data_a) > 0, FALSE)
			FROM solarhist.dh_datum
			WHERE stream_id = sid
				AND ts >= start_ts
				AND ts <= end_ts
			ORDER BY ts
			LIMIT 1
		) AS l(min_ts, min_has_a) ON TRUE
		LEFT OUTER JOIN (
			SELECT ts
			FROM solarhist.dh_datum
			WHERE stream_id = sid
				AND ts >= start_ts
				AND ts <= end_ts
			ORDER BY ts DESC
			LIMIT 1
		) AS r(max_ts) ON TRUE
	)
	-- the datum after the last in the slot (or exactly at the end of the slot), requiring
	-- accumulating values; it only contributes to the clock accumulation that crosses the end of
	-- the slot, which is ignored beyond the 1 hour clock tolerance, so it is looked for within at
	-- most an hour, which keeps requiring accumulating values fast when there are none (NET-537)
	, nrange AS (
		SELECT (
			SELECT CASE
				WHEN d.ts IS NULL THEN drange.max_ts
				WHEN drange.max_ts = end_ts THEN drange.max_ts
				ELSE d.ts
			END
			FROM meta, drange, solarhist.dh_find_time_after_ts(sid, drange.max_ts, end_ts + LEAST(tolerance, INTERVAL 'PT1H'), TRUE, meta.has_no_a) AS d(ts)
		) AS max_ts
	)
	-- whether the datum before the slot is needed, which is only when something returned can be
	-- differenced against it; otherwise its search is skipped, as it reads every row back to the
	-- datum it finds, or its whole window when there is none (NET-539). SolarNet also needs it
	-- when a Reset record is in the slot, which cannot happen here.
	, prior AS (
		-- a stream without accumulating properties has no datum before to find
		SELECT NOT COALESCE((SELECT has_no_a FROM meta), TRUE) AND (
				-- the first datum in the slot has accumulating values, which decides it for most
				-- streams without another lookup
				COALESCE(drange.min_has_a, FALSE)

				-- the datum after the slot is returned, so the clock accumulation from the datum
				-- before to it crosses the slot
				OR COALESCE(nrange.max_ts > drange.max_ts, FALSE)

				-- any datum in the slot has accumulating values, counting one exactly at the end of
				-- the slot (NET-469)
				OR EXISTS (
					SELECT 1
					FROM solarhist.dh_datum d
					WHERE d.stream_id = sid
						AND d.ts >= start_ts
						AND d.ts <= end_ts
						AND d.data_a IS NOT NULL
				)
			) AS needed
		FROM drange, nrange
	)
	-- the datum before and after the slot
	, srange AS (
		SELECT COALESCE(t.min_ts, drange.min_ts, start_ts) AS min_ts, COALESCE(nrange.max_ts, drange.max_ts, end_ts) AS max_ts
		FROM drange, nrange, (
			-- the datum before is searched for only when it is needed; not searching gives the same
			-- result as a search that finds nothing (NET-539)
			SELECT CASE WHEN prior.needed THEN COALESCE(
				(
					-- the datum before the first in the slot (or exactly at the start of the
					-- slot), requiring accumulating values, within at most 14 days because
					-- requiring them is slow when there are none
					SELECT CASE
						WHEN d.ts IS NULL THEN drange.min_ts
						WHEN drange.min_ts = start_ts AND drange.min_has_a THEN drange.min_ts
						ELSE d.ts
					END
					FROM meta, drange, solarhist.dh_find_time_before_ts(sid, COALESCE(drange.min_ts, start_ts), start_ts - LEAST(tolerance, INTERVAL 'P14D'), TRUE, meta.has_no_a) AS d(ts)
				),
				(
					-- then over the full tolerance, not requiring accumulating values, which an
					-- index-only scan can find quickly
					WITH way_back AS (
						SELECT CASE
							WHEN d.ts IS NULL THEN drange.min_ts
							WHEN drange.min_ts = start_ts AND drange.min_has_a THEN drange.min_ts
							ELSE d.ts
						END
						FROM meta, drange, solarhist.dh_find_time_before_ts(
							  sid
							, CASE WHEN drange.min_has_a THEN drange.min_ts ELSE start_ts END - LEAST(tolerance, INTERVAL 'P14D')
							, start_ts - tolerance
							, FALSE
							, meta.has_no_a
							) AS d(ts)
					)
					-- whether the way-back datum has accumulating values
					, way_back_d AS (
						SELECT d.ts, d.has_a
						FROM way_back, solarhist.dh_datum_has_accumulating_at(sid, way_back.ts) AS d
					)
					SELECT COALESCE(
						(
							-- if the way-back datum has accumulating values, use it
							SELECT ts
							FROM way_back_d
							WHERE has_a
						),
						(
							-- otherwise a short search before it, requiring accumulating values,
							-- within the tolerance, unless the stream has none (NET-538)
							SELECT d.ts
							FROM meta, way_back_d, solarhist.dh_find_time_before_ts(
								  sid
								, way_back_d.ts
								, GREATEST(way_back_d.ts - LEAST(tolerance, INTERVAL 'P14D'), start_ts - tolerance)
								, TRUE
								, meta.has_no_a
							) AS d(ts)
							WHERE NOT meta.has_no_a
						)
					)
				)
			) END AS min_ts
			FROM prior
		) t
	)
	-- the range to return; what SolarNet's combined datum and Reset range reduces to without
	-- Reset records
	, combined_srange AS (
		SELECT CASE
				WHEN drange.min_ts IS NULL THEN start_ts
				ELSE srange.min_ts
			END AS min_ts
			, CASE
				WHEN drange.max_ts IS NULL THEN end_ts
				WHEN drange.max_ts = end_ts THEN drange.max_ts
				ELSE srange.max_ts
			END AS max_ts
		FROM drange, srange
	)
	SELECT d.*
	FROM combined_srange, solarhist.dh_find_datum_between(sid, combined_srange.min_ts, combined_srange.max_ts) d
	-- a NULL argument gives no rows, as for a STRICT function
	WHERE num_nulls(sid, start_ts, end_ts, tolerance) = 0
$$;


/**
 * Compute the aggregate of a time span from raw datum.
 *
 * - `data_i`: the mean of each instantaneous property within the span
 * - `stat_i`: [count, minimum, maximum] of each instantaneous property within the span
 * - `data_a`: the clock accumulation of each accumulating property
 * - `read_a`: [difference, start, end] of the readings of each accumulating property
 * - `data_s`: the most frequent value of each status property, as a JSON array
 * - `data_t`: the distinct tags
 *
 * Ported from the SolarNet solardatm.rollup_datm_for_time_span() function, without Reset
 * records.
 *
 * NOTE `solarhist.dh_rollup_datum_15m_for_hour()` has a copy of this query, and the stored hours
 * are computed by that copy: a change here must be made there too.
 *
 * @param sid 				the stream ID
 * @param start_ts			the start of the span (inclusive)
 * @param end_ts 			the end of the span (exclusive)
 * @param tolerance_clock 	the longest time between two readings for their difference to count
 * 							in the clock accumulation
 * @param tolerance_read 	the maximum time to look back for the starting reading
 * @returns one row, or none if the span has no datum
 * @see solarhist.dh_find_datum_for_time_slot()
 * @see solarhist.dh_rollup_datum_15m_for_hour()
 */
CREATE OR REPLACE FUNCTION solarhist.dh_rollup_datum_for_time_span(
		sid 			INTEGER,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		tolerance_clock INTERVAL DEFAULT INTERVAL 'PT1H',
		tolerance_read 	INTERVAL DEFAULT INTERVAL 'P1Y'
	) RETURNS SETOF solarhist.dh_agg_datum LANGUAGE SQL STABLE ROWS 500 AS
$$
	WITH m AS (
		SELECT COALESCE(array_length(names_i, 1), 0) AS len_i
			 , COALESCE(array_length(names_a, 1), 0) AS len_a
		FROM solarhist.dh_datum_meta
		WHERE stream_id = sid
	)
	-- the datum, with value arrays padded to the stream's property count
	, d AS (
		SELECT
			  stream_id
			, ts
			, pad_vec(data_i, len_i) AS data_i
			, pad_vec(data_a, len_a) AS data_a
			, data_s
			, data_t
			, (ts >= start_ts AND ts < end_ts) AS inc
		FROM m, solarhist.dh_find_datum_for_time_slot(
			sid,
			start_ts,
			end_ts,
			tolerance_read
		)
	)
	-- instantaneous statistics per property
	, di_ary AS (
		SELECT
			  vec_trim_scale(vec_agg_mean(d.data_i)) AS data_i
			, solarhist.array_transpose2d(ARRAY[
				  vec_agg_count(d.data_i)::NUMERIC[]
				, vec_agg_min(d.data_i)
				, vec_agg_max(d.data_i)
			  ]) AS stat_i
		FROM d
		WHERE d.inc
	)
	-- clock accumulation per property
	-- NOTE "unnest() WITH ORDINALITY" not used because of possible sparse array slice
	, wa AS (
		SELECT
			  p.idx
			, d.data_a[p.idx] AS val
			, d.ts
			, COALESCE(d.data_a[p.idx] - lag(d.data_a[p.idx]) OVER slot, 0)::NUMERIC AS diff
			, CASE
				-- too much time between rows for clock diff, or no time passed
				WHEN d.ts - lag(d.ts) OVER slot > tolerance_clock OR d.ts = lag(d.ts) OVER slot
					THEN 0

				-- before bounds
				WHEN d.ts < start_ts
					THEN 0

				-- crossing start of slot; allocate only end portion of accumulation within this slot
				WHEN lag(d.ts) OVER slot < start_ts
					THEN EXTRACT('epoch' FROM d.ts - start_ts) / EXTRACT('epoch' FROM d.ts - lag(d.ts) OVER slot)

				-- crossing end of slot; allocate only start portion of accumulation within this slot
				WHEN d.ts > end_ts AND lag(d.ts) OVER slot < end_ts
					THEN EXTRACT('epoch' FROM end_ts - lag(d.ts) OVER slot) / EXTRACT('epoch' FROM d.ts - lag(d.ts) OVER slot)

				ELSE 1
				END AS portion
			-- readings at or before the end of the slot
			, ts <= end_ts AS rinc
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_a, 1)) AS p(idx) ON TRUE
		WINDOW slot AS (PARTITION BY p.idx ORDER BY CASE WHEN d.data_a[p.idx] IS NULL THEN 1 ELSE 0 END, d.ts
			RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- accumulating statistics per property
	, da AS (
		SELECT
			  idx
			, sum(diff * portion::NUMERIC) AS cdiff
			, sum(diff) FILTER (WHERE rinc) AS rdiff
			, solarhist.first(val ORDER BY ts) FILTER (WHERE rinc) AS rstart
			, solarhist.first(val ORDER BY ts DESC) FILTER (WHERE rinc) AS rend
		FROM wa
		GROUP BY idx
	)
	, da_ary AS (
		SELECT
			  vec_trim_scale(array_agg(cdiff ORDER BY idx)) AS data_a
			, array_agg(
				ARRAY[rdiff, rstart, rend] ORDER BY idx
			) AS read_a
		FROM da
	)
	-- status values per property; JSON null counts as no value
	, ws AS (
		SELECT
			  p.idx AS idx
			, NULLIF(d.data_s->(p.idx - 1), 'null'::JSONB) AS val
		FROM d
		INNER JOIN generate_series(1, jsonb_array_length(d.data_s)) AS p(idx) ON TRUE
		WHERE d.data_s IS NOT NULL AND d.inc
	)
	-- the most frequent value of each status property
	, ds AS (
		SELECT
			  ws.idx
			, mode() WITHIN GROUP (ORDER BY ws.val) AS val
		FROM ws
		GROUP BY ws.idx
	)
	, ds_ary AS (
		SELECT
			  jsonb_agg(ds.val ORDER BY ds.idx) AS data_s
		FROM ds
	)
	-- the distinct tags
	, dt_ary AS (
		SELECT
			  array_agg(DISTINCT p.val) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL AND d.inc
	)
	SELECT
		sid AS stream_id
		, start_ts AS ts_start
		, di_ary.data_i
		, da_ary.data_a
		, ds_ary.data_s
		, dt_ary.data_t
		, di_ary.stat_i
		, da_ary.read_a
	FROM di_ary, da_ary, ds_ary, dt_ary
	WHERE data_i IS NOT NULL OR data_a IS NOT NULL OR data_s IS NOT NULL OR data_t IS NOT NULL
$$;


/**
 * Compute the aggregate of one hour from raw datum, together with the aggregates of its 15
 * minute slots.
 *
 * The first eight output columns are the hour, exactly as
 * `solarhist.dh_rollup_datum_for_time_span()` computes it for the same arguments, and no row is
 * returned when that function returns none. The `slots` column holds the hour's 15 minute rows
 * in time order.
 *
 * The slots are computed from exactly the datum, and the per-datum differences and portions,
 * that the hour uses, and each contribution to the hour is given to one or two of its slots, so
 * the slots add up to the hour:
 *
 * - clock accumulation between two datum goes to the slot of the earlier datum, up to the start
 *   of the slot of the later datum; the later datum's slot gets the remainder of the hour's
 *   portion
 * - a contribution that crosses the start of the hour goes to the slot of the later datum, and
 *   one that crosses the end of the hour to the slot of the earlier datum
 * - a reading difference goes to the slot of its datum; a datum before the hour counts in the
 *   first slot holding a datum, and a datum exactly at the end of the hour in the last one (or
 *   the last slot of the hour, if no slot holds a datum)
 * - means, counts, minimums, maximums, status and tags are grouped by slot
 * - a slot whose datum have no value for an accumulating property gets NULL for it when another
 *   slot of the hour has one, and zero when none has, as the hour reports
 *
 * Slots are 15 minute buckets of UTC time, from `date_bin()` with the origin
 * `solarhist.dh_rollup_datum_for_time_span_slots()` uses, so the hour must start on a 15 minute
 * boundary. `solarhist.dh_process_one_agg_stale()` always passes a UTC hour.
 *
 * Ported from the SolarNet solardatm.rollup_datm_15m_for_hour() function of NET-540, without
 * Reset records, with `date_bin()` for TimescaleDB's `time_bucket()`, and with `data_s` as a
 * JSON array.
 *
 * NOTE the CTEs that compute the hour, up to `dt_ary`, are a copy of the query of
 * `solarhist.dh_rollup_datum_for_time_span()`, with only `p_ts` added to `wa`: a change there
 * must be made here too. Sharing them would make every caller of that function pay for the
 * slots.
 *
 * @param sid 				the stream ID
 * @param start_ts			the start of the hour (inclusive)
 * @param end_ts 			the end of the hour (exclusive)
 * @param tolerance_clock 	as for `solarhist.dh_rollup_datum_for_time_span()`
 * @param tolerance_read 	as for `solarhist.dh_rollup_datum_for_time_span()`
 * @returns one row, or none if the hour has no datum
 * @see solarhist.dh_rollup_datum_for_time_span()
 * @see solarhist.dh_process_one_agg_stale()
 */
CREATE OR REPLACE FUNCTION solarhist.dh_rollup_datum_15m_for_hour(
		sid 			INTEGER,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		tolerance_clock INTERVAL DEFAULT INTERVAL 'PT1H',
		tolerance_read 	INTERVAL DEFAULT INTERVAL 'P1Y'
	) RETURNS TABLE (
		stream_id	INTEGER,
		ts_start	TIMESTAMP WITH TIME ZONE,
		data_i		NUMERIC[],
		data_a		NUMERIC[],
		data_s		JSONB,
		data_t		TEXT[],
		stat_i		NUMERIC[][],
		read_a		NUMERIC[][],
		slots		solarhist.dh_agg_datum[]
	) LANGUAGE SQL STABLE ROWS 1 AS
$$
	WITH m AS (
		SELECT COALESCE(array_length(names_i, 1), 0) AS len_i
			 , COALESCE(array_length(names_a, 1), 0) AS len_a
		FROM solarhist.dh_datum_meta
		WHERE stream_id = sid
	)
	-- the datum, with value arrays padded to the stream's property count
	, d AS (
		SELECT
			  stream_id
			, ts
			, pad_vec(data_i, len_i) AS data_i
			, pad_vec(data_a, len_a) AS data_a
			, data_s
			, data_t
			, (ts >= start_ts AND ts < end_ts) AS inc
		FROM m, solarhist.dh_find_datum_for_time_slot(
			sid,
			start_ts,
			end_ts,
			tolerance_read
		)
	)
	-- instantaneous statistics per property
	, di_ary AS (
		SELECT
			  vec_trim_scale(vec_agg_mean(d.data_i)) AS data_i
			, solarhist.array_transpose2d(ARRAY[
				  vec_agg_count(d.data_i)::NUMERIC[]
				, vec_agg_min(d.data_i)
				, vec_agg_max(d.data_i)
			  ]) AS stat_i
		FROM d
		WHERE d.inc
	)
	-- clock accumulation per property
	-- NOTE "unnest() WITH ORDINALITY" not used because of possible sparse array slice
	, wa AS (
		SELECT
			  p.idx
			, d.data_a[p.idx] AS val
			, d.ts
			, COALESCE(d.data_a[p.idx] - lag(d.data_a[p.idx]) OVER slot, 0)::NUMERIC AS diff
			, CASE
				-- too much time between rows for clock diff, or no time passed
				WHEN d.ts - lag(d.ts) OVER slot > tolerance_clock OR d.ts = lag(d.ts) OVER slot
					THEN 0

				-- before bounds
				WHEN d.ts < start_ts
					THEN 0

				-- crossing start of slot; allocate only end portion of accumulation within this slot
				WHEN lag(d.ts) OVER slot < start_ts
					THEN EXTRACT('epoch' FROM d.ts - start_ts) / EXTRACT('epoch' FROM d.ts - lag(d.ts) OVER slot)

				-- crossing end of slot; allocate only start portion of accumulation within this slot
				WHEN d.ts > end_ts AND lag(d.ts) OVER slot < end_ts
					THEN EXTRACT('epoch' FROM end_ts - lag(d.ts) OVER slot) / EXTRACT('epoch' FROM d.ts - lag(d.ts) OVER slot)

				ELSE 1
				END AS portion
			-- readings at or before the end of the slot
			, ts <= end_ts AS rinc
			-- the time of the datum before, for the 15 minute slots
			, lag(d.ts) OVER slot AS p_ts
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_a, 1)) AS p(idx) ON TRUE
		WINDOW slot AS (PARTITION BY p.idx ORDER BY CASE WHEN d.data_a[p.idx] IS NULL THEN 1 ELSE 0 END, d.ts
			RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- accumulating statistics per property
	, da AS (
		SELECT
			  idx
			, sum(diff * portion::NUMERIC) AS cdiff
			, sum(diff) FILTER (WHERE rinc) AS rdiff
			, solarhist.first(val ORDER BY ts) FILTER (WHERE rinc) AS rstart
			, solarhist.first(val ORDER BY ts DESC) FILTER (WHERE rinc) AS rend
		FROM wa
		GROUP BY idx
	)
	, da_ary AS (
		SELECT
			  vec_trim_scale(array_agg(cdiff ORDER BY idx)) AS data_a
			, array_agg(
				ARRAY[rdiff, rstart, rend] ORDER BY idx
			) AS read_a
		FROM da
	)
	-- status values per property; JSON null counts as no value
	, ws AS (
		SELECT
			  p.idx AS idx
			, NULLIF(d.data_s->(p.idx - 1), 'null'::JSONB) AS val
		FROM d
		INNER JOIN generate_series(1, jsonb_array_length(d.data_s)) AS p(idx) ON TRUE
		WHERE d.data_s IS NOT NULL AND d.inc
	)
	-- the most frequent value of each status property
	, ds AS (
		SELECT
			  ws.idx
			, mode() WITHIN GROUP (ORDER BY ws.val) AS val
		FROM ws
		GROUP BY ws.idx
	)
	, ds_ary AS (
		SELECT
			  jsonb_agg(ds.val ORDER BY ds.idx) AS data_s
		FROM ds
	)
	-- the distinct tags
	, dt_ary AS (
		SELECT
			  array_agg(DISTINCT p.val) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL AND d.inc
	)

	-- the 15 minute slots of the hour, from here on

	-- slots of the hour holding a datum
	, occ AS (
		SELECT DISTINCT date_bin(INTERVAL 'PT15M', d.ts, TIMESTAMPTZ '2000-01-03 00:00:00+00') AS slot
		FROM d
		WHERE d.inc
	)
	-- the first and last of those slots, or the last slot of the hour if there are none
	, edge AS (
		SELECT COALESCE(min(slot), end_ts - INTERVAL 'PT15M') AS first_slot
			 , COALESCE(max(slot), end_ts - INTERVAL 'PT15M') AS last_slot
		FROM occ
	)
	-- instantaneous statistics per slot
	, slot_di_ary AS (
		SELECT
			  date_bin(INTERVAL 'PT15M', d.ts, TIMESTAMPTZ '2000-01-03 00:00:00+00') AS slot
			, vec_trim_scale(vec_agg_mean(d.data_i)) AS data_i
			, solarhist.array_transpose2d(ARRAY[
				  vec_agg_count(d.data_i)::NUMERIC[]
				, vec_agg_min(d.data_i)
				, vec_agg_max(d.data_i)
			  ]) AS stat_i
		FROM d
		WHERE d.inc
		GROUP BY 1
	)
	-- the slots of each datum and of the datum before it, when they are within the hour
	, wb AS (
		SELECT wa.*
			, wa.portion::NUMERIC AS hp
			, CASE WHEN wa.ts >= start_ts AND wa.ts < end_ts
				THEN date_bin(INTERVAL 'PT15M', wa.ts, TIMESTAMPTZ '2000-01-03 00:00:00+00') END AS c_slot
			, CASE WHEN wa.p_ts >= start_ts AND wa.p_ts < end_ts
				THEN date_bin(INTERVAL 'PT15M', wa.p_ts, TIMESTAMPTZ '2000-01-03 00:00:00+00') END AS p_slot
		FROM wa
	)
	-- the earlier datum's share of the hour's portion, when the two datum are in different slots
	-- of the hour; datum without a value for the property have no difference to share out
	, wc AS (
		SELECT wb.*
			, CASE
				WHEN p_slot IS NOT NULL AND c_slot IS NOT NULL AND c_slot > p_slot
					THEN (EXTRACT('epoch' FROM c_slot - p_ts) / EXTRACT('epoch' FROM ts - p_ts))::NUMERIC
				END AS p_share
		FROM wb
		WHERE hp <> 0 AND val IS NOT NULL
	)
	, alloc AS (
		-- to the slot of the earlier datum
		SELECT idx, p_slot AS slot, diff * COALESCE(p_share, hp) AS amt
		FROM wc
		WHERE p_slot IS NOT NULL AND (c_slot IS NULL OR c_slot > p_slot)

		UNION ALL

		-- to the slot of the later datum: everything, or the remainder after the earlier datum's
		-- share
		SELECT idx, c_slot AS slot, diff * (hp - COALESCE(p_share, 0)) AS amt
		FROM wc
		WHERE c_slot IS NOT NULL

		UNION ALL

		-- neither datum is within the hour
		SELECT idx, edge.last_slot AS slot, diff * hp AS amt
		FROM wc, edge
		WHERE p_slot IS NULL AND c_slot IS NULL
	)
	-- clock accumulation per slot
	, ca AS (
		SELECT idx, slot, sum(amt) AS cdiff
		FROM alloc
		GROUP BY idx, slot
	)
	-- the datum the hour's readings count, each in its slot
	, rd AS (
		SELECT wa.idx
			, CASE
				WHEN wa.ts < start_ts THEN edge.first_slot
				WHEN wa.ts < end_ts THEN date_bin(INTERVAL 'PT15M', wa.ts, TIMESTAMPTZ '2000-01-03 00:00:00+00')
				ELSE edge.last_slot
				END AS slot
			, wa.val
			, wa.diff
			, wa.ts
		FROM wa, edge
		WHERE wa.rinc
	)
	-- readings per slot
	, ra0 AS (
		SELECT idx, slot
			, sum(diff) AS rdiff
			, solarhist.first(val ORDER BY ts) AS rstart_first
			, solarhist.first(val - diff ORDER BY ts) AS rstart_later
			, solarhist.first(val ORDER BY ts DESC) AS rend
		FROM rd
		GROUP BY idx, slot
	)
	-- for each property: whether any slot of the hour has a value for it, and the first slot that
	-- does
	, ra AS (
		SELECT ra0.*
			, bool_or(ra0.rend IS NOT NULL) OVER (PARTITION BY ra0.idx) AS hour_has
			, ra0.slot = min(ra0.slot) FILTER (WHERE ra0.rend IS NOT NULL) OVER (PARTITION BY ra0.idx) AS is_first
		FROM ra0
	)
	, slot_ts AS (
		SELECT slot FROM occ
		UNION
		SELECT slot FROM ca
		UNION
		SELECT slot FROM ra
	)
	-- A slot whose datum have no value for a property gets NULL for it when another slot of the
	-- hour has one, so that rolling the slots up takes the hour's first and last readings from
	-- slots that have them. When no slot has a value the hourly rollup reports zero, and so does
	-- each slot.
	, slot_da_ary AS (
		SELECT s.slot
			, CASE WHEN count(ca.idx) + count(ra.idx) > 0 THEN
				vec_trim_scale(array_agg(
					CASE
						WHEN ra.rend IS NOT NULL OR ca.cdiff <> 0 THEN COALESCE(ca.cdiff, 0)
						WHEN ra.hour_has THEN NULL
						WHEN ra.idx IS NOT NULL THEN 0
						END
					ORDER BY p.idx))
				END AS data_a
			, CASE WHEN count(ra.idx) > 0 THEN
				array_agg(ARRAY[
					  ra.rdiff
					, CASE WHEN ra.is_first THEN ra.rstart_first ELSE ra.rstart_later END
					, ra.rend
				] ORDER BY p.idx)
				END AS read_a
		FROM slot_ts s
		CROSS JOIN m
		CROSS JOIN generate_series(1, m.len_a) AS p(idx)
		LEFT OUTER JOIN ca ON ca.slot = s.slot AND ca.idx = p.idx
		LEFT OUTER JOIN ra ON ra.slot = s.slot AND ra.idx = p.idx
		GROUP BY s.slot
	)
	-- status values per slot and property; JSON null counts as no value
	, slot_ws AS (
		SELECT
			  date_bin(INTERVAL 'PT15M', d.ts, TIMESTAMPTZ '2000-01-03 00:00:00+00') AS slot
			, p.idx AS idx
			, NULLIF(d.data_s->(p.idx - 1), 'null'::JSONB) AS val
		FROM d
		INNER JOIN generate_series(1, jsonb_array_length(d.data_s)) AS p(idx) ON TRUE
		WHERE d.data_s IS NOT NULL AND d.inc
	)
	-- the most frequent value of each status property per slot
	, slot_ds AS (
		SELECT slot_ws.slot, slot_ws.idx, mode() WITHIN GROUP (ORDER BY slot_ws.val) AS val
		FROM slot_ws
		GROUP BY slot_ws.slot, slot_ws.idx
	)
	, slot_ds_ary AS (
		SELECT slot, jsonb_agg(val ORDER BY idx) AS data_s
		FROM slot_ds
		GROUP BY slot
	)
	-- the distinct tags per slot
	, slot_dt_ary AS (
		SELECT date_bin(INTERVAL 'PT15M', d.ts, TIMESTAMPTZ '2000-01-03 00:00:00+00') AS slot
			, array_agg(DISTINCT p.val) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL AND d.inc
		GROUP BY 1
	)
	-- the slot rows, in time order
	, slot_ary AS (
		SELECT array_agg(ROW(
				  sid
				, slot_ts.slot
				, slot_di_ary.data_i
				, slot_da_ary.data_a
				, slot_ds_ary.data_s
				, slot_dt_ary.data_t
				, slot_di_ary.stat_i
				, slot_da_ary.read_a
			)::solarhist.dh_agg_datum ORDER BY slot_ts.slot) AS slots
		FROM slot_ts
		LEFT OUTER JOIN slot_di_ary ON slot_di_ary.slot = slot_ts.slot
		LEFT OUTER JOIN slot_da_ary ON slot_da_ary.slot = slot_ts.slot
		LEFT OUTER JOIN slot_ds_ary ON slot_ds_ary.slot = slot_ts.slot
		LEFT OUTER JOIN slot_dt_ary ON slot_dt_ary.slot = slot_ts.slot
		WHERE slot_di_ary.data_i IS NOT NULL OR slot_da_ary.data_a IS NOT NULL
			OR slot_ds_ary.data_s IS NOT NULL OR slot_dt_ary.data_t IS NOT NULL
	)
	SELECT
		sid AS stream_id
		, start_ts AS ts_start
		, di_ary.data_i
		, da_ary.data_a
		, ds_ary.data_s
		, dt_ary.data_t
		, di_ary.stat_i
		, da_ary.read_a
		, slot_ary.slots
	FROM di_ary, da_ary, ds_ary, dt_ary, slot_ary
	WHERE data_i IS NOT NULL OR data_a IS NOT NULL OR data_s IS NOT NULL OR data_t IS NOT NULL
$$;


/* ============================================================================
 * Sub-hour rollup
 * ============================================================================
 */

/**
 * Return a valid sub-hour slot length for an arbitrary number of seconds.
 *
 * A slot length must be between 60 and 1800 seconds and divide evenly into 1800, so the slots
 * of every length start on the hour. Any other value gives 600.
 *
 * Copied from the SolarNet solardatm.slot_seconds() function.
 *
 * @param secs the seconds to validate
 * @returns `secs`, or 600 if it is not a valid slot length
 */
CREATE OR REPLACE FUNCTION solarhist.dh_slot_seconds(secs INTEGER DEFAULT 600)
	RETURNS INTEGER LANGUAGE SQL IMMUTABLE AS
$$
	SELECT CASE
		WHEN secs < 60 OR secs > 1800 OR 1800 % secs <> 0 THEN 600
	ELSE
		secs
	END
$$;


/**
 * Count the sub-hour slots `solarhist.dh_rollup_datum_for_time_span_slots()` returns for a time
 * span.
 *
 * This complements `solarhist.dh_rollup_datum_for_time_span_slots()`, to count its results
 * much faster than computing them.
 *
 * It counts the slots that start within the span and hold a datum, counting all the datum of the
 * last slot even past the end of the span, and the last slot of each hour without datum that ends
 * with a datum exactly on the next hour, when the stream has accumulating properties. It reads
 * only the time of each datum, not its values, so it also counts the rare slots that
 * `solarhist.dh_rollup_datum_for_time_span_slots()` omits: one whose datum have no values at all,
 * and the last slot of an hour before a datum on the hour that has no accumulating values.
 *
 * It is written in plpgsql so that a session reuses the plan of its query: Postgres cannot inline
 * a SQL function that returns a single value from a query like this one, and plans its body at
 * every call, without the argument values, over every partition of `solarhist.dh_datum`.
 *
 * Ported from the SolarNet solardatm.count_datm_time_span_slots() function, as of NET-540,
 * without Reset records.
 *
 * @param sid 		the stream ID
 * @param start_ts	the minimum time (inclusive)
 * @param end_ts 	the maximum time (exclusive)
 * @param secs		the slot length, in seconds, as for `solarhist.dh_slot_seconds()`
 * @returns the slot count
 * @see solarhist.dh_rollup_datum_for_time_span_slots()
 * @see solarhist.dh_slot_seconds()
 */
CREATE OR REPLACE FUNCTION solarhist.dh_count_datum_time_span_slots(
		sid 			INTEGER,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		secs			INTEGER DEFAULT 600
	) RETURNS BIGINT LANGUAGE plpgsql STABLE AS
$$
DECLARE
	n BIGINT;
BEGIN
	-- the slot length, the first slot starting in the span, and the end of the last slot starting
	-- in it
	WITH b AS (
		SELECT q.slot_len
			, date_bin(q.slot_len, start_ts - INTERVAL 'PT0.000001S', TIMESTAMPTZ '2000-01-03 00:00:00+00') + q.slot_len AS lo
			, date_bin(q.slot_len, end_ts - INTERVAL 'PT0.000001S', TIMESTAMPTZ '2000-01-03 00:00:00+00') + q.slot_len AS hi
		FROM (SELECT make_interval(secs => solarhist.dh_slot_seconds(secs)) AS slot_len) q
	)
	-- the slots with datum, each with the time of its first datum; the datum are read from an hour
	-- before the span to an hour after it, which holds the hour before the first slot and the slot
	-- after the last, with bounds on the arguments alone so the partitions of the datum table are
	-- pruned whether the plan is generic or not
	, s AS (
		SELECT date_bin(b.slot_len, d.ts, TIMESTAMPTZ '2000-01-03 00:00:00+00') AS slot, min(d.ts) AS ts
		FROM b
		INNER JOIN solarhist.dh_datum d ON d.stream_id = sid
			AND d.ts >= start_ts - INTERVAL 'PT1H'
			AND d.ts < end_ts + INTERVAL 'PT1H'
		GROUP BY 1
	)
	SELECT count(*) FILTER (WHERE w.slot >= b.lo AND w.slot < b.hi)
		-- the last slot of an hour without datum, when a datum is exactly at the end of the hour
		+ count(*) FILTER (WHERE w.ts = w.slot
			AND w.slot = date_bin(INTERVAL 'PT1H', w.slot, TIMESTAMPTZ '2000-01-03 00:00:00+00')
			AND w.slot - b.slot_len >= b.lo
			AND w.slot - b.slot_len < b.hi
			AND (w.p_slot IS NULL OR w.p_slot < w.slot - INTERVAL 'PT1H')
			AND m.has_a)
	INTO n
	FROM b
	, (SELECT COALESCE(CARDINALITY(md.names_a) > 0, FALSE) AS has_a
		FROM solarhist.dh_datum_meta md
		WHERE md.stream_id = sid) m
	, (SELECT s.slot, s.ts, lag(s.slot) OVER (ORDER BY s.slot) AS p_slot FROM s) w;
	RETURN n;
END
$$;


/**
 * Compute the aggregates of the sub-hour slots of a time span from raw datum.
 *
 * Nothing is stored: the slots are computed when queried. They are `secs` long, and start at
 * multiples of `secs` seconds from 2000-01-03 00:00 UTC, the origin SolarNet's TimescaleDB
 * time_bucket() function uses. `date_bin()` is used instead, so TimescaleDB is not needed; for
 * every slot length `solarhist.dh_slot_seconds()` allows, the slots start on the hour in UTC.
 *
 * A slot is returned when it starts within the span, and is computed from all its datum, so a
 * span that ends part way through a slot gets all of that slot, and one that starts part way
 * through a slot does not get that slot. A slot's result therefore does not depend on the span it
 * is part of. Each slot returned has, as for `solarhist.dh_rollup_datum_for_time_span()`:
 *
 * - `data_i`: the mean of each instantaneous property within the slot
 * - `stat_i`: [count, minimum, maximum] of each instantaneous property within the slot
 * - `data_a`: the clock accumulation of each accumulating property allocated to the slot
 * - `read_a`: NULL, as reading statistics are not computed for slots
 * - `data_s`: the most frequent value of each status property, as a JSON array
 * - `data_t`: the distinct tags
 *
 * The clock accumulation of each UTC hour is what `solarhist.dh_rollup_datum_for_time_span()`
 * computes for the hour, and it is allocated to the slots of the hour as
 * `solarhist.dh_rollup_datum_15m_for_hour()` allocates it to 15 minute slots, for any slot length,
 * so the slots of an hour add up to the hour:
 *
 * - the difference between two readings in the same slot goes to that slot
 * - the difference between two readings of the hour in different slots is split at the start of
 *   the later reading's slot, so that slot gets the part after its start and the earlier
 *   reading's slot the rest; slots between them get nothing
 * - the hour's part of a difference that crosses the start of the hour goes to the slot of the
 *   first reading in the hour, and the hour's part of one that crosses the end of the hour goes
 *   to the slot of the last reading in the hour
 * - no accumulation is counted across a gap longer than `tolerance_clock`, nor between readings
 *   at the same time
 * - the last slot of an hour without datum, but with a datum with accumulating values exactly at
 *   its end, is returned with no accumulation, as the hour has an hourly rollup
 *
 * A datum that lacks some of the stream's accumulating properties is differenced against the
 * nearest readings with any accumulating property, and a property one of the two lacks adds
 * nothing. The hourly rollup instead differences each property against the nearest readings that
 * have it, so for such a property the slots of an hour can add up to less than the hour.
 *
 * Ported from the SolarNet solardatm.rollup_datm_for_time_span_slots() function, as of NET-540,
 * without Reset records, with `date_bin()` for TimescaleDB's `time_bucket()`, and with `data_s` as
 * a JSON array. A NULL argument gives no rows, as for a STRICT function.
 *
 * @param sid 				the stream ID
 * @param start_ts			the start of the span (inclusive)
 * @param end_ts 			the end of the span (exclusive)
 * @param secs				the slot length, in seconds; must be between 60 and 1800 and divide
 *                          evenly into 1800, or else 600 is used
 * @param tolerance_clock 	the longest time between two readings for their difference to count
 * 							in the clock accumulation, and so how far before and after the hours
 * 							of the span datum are read
 * @returns the slot aggregates
 * @see solarhist.dh_count_datum_time_span_slots()
 * @see solarhist.dh_rollup_datum_for_time_span()
 * @see solarhist.dh_rollup_datum_15m_for_hour()
 * @see solarhist.dh_slot_seconds()
 */
CREATE OR REPLACE FUNCTION solarhist.dh_rollup_datum_for_time_span_slots(
		sid 			INTEGER,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		secs			INTEGER DEFAULT 600,
		tolerance_clock INTERVAL DEFAULT INTERVAL 'PT1H'
	) RETURNS SETOF solarhist.dh_agg_datum LANGUAGE SQL STABLE ROWS 500 AS
$$
	-- the stream's array lengths, the slot length, and the whole UTC hours holding the span; a
	-- NULL argument gives no rows, as for a STRICT function
	WITH m AS (
		SELECT COALESCE(array_length(names_i, 1), 0) AS len_i
			, COALESCE(array_length(names_a, 1), 0) AS len_a
			, COALESCE(CARDINALITY(names_a) = 0, TRUE) AS has_no_a
			, make_interval(secs => solarhist.dh_slot_seconds(secs)) AS slot_len
			, date_bin(INTERVAL 'PT1H', start_ts, TIMESTAMPTZ '2000-01-03 00:00:00+00') AS hs
			, date_bin(INTERVAL 'PT1H', end_ts - INTERVAL 'PT0.000001S', TIMESTAMPTZ '2000-01-03 00:00:00+00')
				+ INTERVAL 'PT1H' AS he
		FROM solarhist.dh_datum_meta
		WHERE stream_id = sid
			AND num_nulls(start_ts, end_ts, secs, tolerance_clock) = 0
	)
	-- the datum with accumulating values just before and after those hours, within the tolerance
	, b AS (
		SELECT m.*
			, COALESCE((SELECT t FROM solarhist.dh_find_time_before_ts(sid, m.hs, m.hs - tolerance_clock
				, TRUE, m.has_no_a) AS t), m.hs) AS lo
			, COALESCE((SELECT t FROM solarhist.dh_find_time_after_ts(sid, m.he, m.he + tolerance_clock
				, TRUE, m.has_no_a) AS t), m.he) AS hi
		FROM m
	)
	-- the datum, with value arrays padded to the stream's property count
	, d AS (
		SELECT d.ts
			, pad_vec(d.data_i, b.len_i) AS data_i
			, pad_vec(d.data_a, b.len_a) AS data_a
			, d.data_s
			, d.data_t
		FROM b, solarhist.dh_find_datum_between(sid, b.lo, b.hi) d
	)
	-- the times on the hour of datum without accumulating values: such a datum ends the datum of
	-- the hour before it, as for the hourly rollup, so no accumulation crosses the end of that hour
	, nb AS (
		SELECT array_agg(d.ts) FILTER (
				WHERE d.ts = date_bin(INTERVAL 'PT1H', d.ts, TIMESTAMPTZ '2000-01-03 00:00:00+00')) AS ts
		FROM d
		WHERE d.data_a IS NULL
	)
	-- each datum with the datum before and after it that have accumulating values
	, w AS (
		SELECT d.*
			, date_bin(b.slot_len, d.ts, TIMESTAMPTZ '2000-01-03 00:00:00+00') AS slot
			, date_bin(INTERVAL 'PT1H', d.ts, TIMESTAMPTZ '2000-01-03 00:00:00+00') AS hr
			, lag(d.ts) OVER win AS p_ts
			, lag(d.data_a) OVER win AS p_a
			, lead(d.ts) OVER win AS n_ts
			, lead(d.data_a) OVER win AS n_a
			-- whether the datum and the datum before and after it have every accumulating property
			, array_position(d.data_a, NULL) IS NULL AS full_a
			, lag(array_position(d.data_a, NULL) IS NULL) OVER win AS p_full_a
			, lead(array_position(d.data_a, NULL) IS NULL) OVER win AS n_full_a
		FROM b, d
		WINDOW win AS (ORDER BY d.data_a IS NULL, d.ts)
	)
	-- the portion of the difference from the datum before that falls in this datum's slot, and the
	-- portion of the difference to the datum after that falls in this datum's slot
	, p0 AS (
		SELECT w.*
			, CASE
				WHEN w.data_a IS NULL OR w.p_ts IS NULL
					OR w.ts - w.p_ts > tolerance_clock OR w.ts = w.p_ts
					THEN 0
				-- crossing the start of the hour
				WHEN w.p_ts < w.hr
					THEN CASE
						WHEN w.ts = w.hr THEN 0
						ELSE EXTRACT('epoch' FROM w.ts - w.hr) / EXTRACT('epoch' FROM w.ts - w.p_ts)
						END
				WHEN w.p_ts >= w.slot
					THEN 1
				ELSE 1 - EXTRACT('epoch' FROM w.slot - w.p_ts) / EXTRACT('epoch' FROM w.ts - w.p_ts)
				END AS pb
			, CASE
				WHEN w.data_a IS NULL OR w.n_a IS NULL
					OR w.n_ts - w.ts > tolerance_clock OR w.n_ts = w.ts
					THEN 0
				-- at or crossing the end of the hour
				WHEN w.n_ts >= w.hr + INTERVAL 'PT1H'
					THEN CASE
						WHEN w.n_ts = w.hr + INTERVAL 'PT1H' THEN 1
						-- a datum without accumulating values at the end of the hour ends its datum
						WHEN w.hr + INTERVAL 'PT1H' = ANY(nb.ts) THEN 0
						ELSE EXTRACT('epoch' FROM w.hr + INTERVAL 'PT1H' - w.ts) / EXTRACT('epoch' FROM w.n_ts - w.ts)
						END
				WHEN w.n_ts < w.slot + b.slot_len
					THEN 0
				ELSE EXTRACT('epoch' FROM date_bin(b.slot_len, w.n_ts, TIMESTAMPTZ '2000-01-03 00:00:00+00') - w.ts)
					/ EXTRACT('epoch' FROM w.n_ts - w.ts)
				END AS pa
		FROM b, nb, w
		-- computed once per datum, not once per reference below
		OFFSET 0
	)
	-- per slot: instantaneous statistics, and the clock accumulation of its datum, NULL for a
	-- property none of its datum has
	, sa AS (
		SELECT p.slot
			, date_bin(INTERVAL 'PT1H', p.slot, TIMESTAMPTZ '2000-01-03 00:00:00+00') AS hr
			, bool_or(p.a_at_slot) AS a_at_slot
			, vec_to_sum(p.c) AS acc
			, vec_trim_scale(vec_agg_mean(p.data_i)) AS data_i
			, vec_agg_count(p.data_i)::NUMERIC[] AS cnt_i
			, vec_agg_min(p.data_i) AS min_i
			, vec_agg_max(p.data_i) AS max_i
		FROM (
			-- each datum's accumulation within its slot, NULL for a property the datum lacks
			SELECT p.slot
				, p.data_i
				, (p.data_a IS NOT NULL AND p.ts = p.slot) AS a_at_slot
				, CASE
					WHEN p.data_a IS NULL THEN NULL
					WHEN p.pb = 0 AND p.pa = 0 THEN vec_mul(p.data_a, 0::NUMERIC)
					-- every property present: the differences have no NULL to mask
					WHEN p.full_a AND (p.pb = 0 OR p.p_full_a) AND (p.pa = 0 OR p.n_full_a) THEN CASE
						WHEN p.pa = 0 AND p.pb = 1 THEN vec_sub(p.data_a, p.p_a)
						WHEN p.pa = 0 THEN vec_mul(vec_sub(p.data_a, p.p_a), p.pb::NUMERIC)
						WHEN p.pb = 0 THEN vec_mul(vec_sub(p.n_a, p.data_a), p.pa::NUMERIC)
						WHEN p.pb = 1 THEN vec_add(vec_sub(p.data_a, p.p_a)
							, vec_mul(vec_sub(p.n_a, p.data_a), p.pa::NUMERIC))
						ELSE vec_add(vec_mul(vec_sub(p.data_a, p.p_a), p.pb::NUMERIC)
							, vec_mul(vec_sub(p.n_a, p.data_a), p.pa::NUMERIC))
						END
					-- otherwise a property the datum has but a datum it is differenced against lacks
					-- adds zero, and one the datum lacks stays NULL
					ELSE vec_add(vec_mul(p.data_a, 0::NUMERIC), vec_add(
						  CASE WHEN p.pb <> 0
							THEN vec_coalesce(vec_mul(vec_sub(p.data_a, p.p_a), p.pb::NUMERIC), 0::NUMERIC)
							ELSE vec_coalesce(vec_mul(p.data_a, 0::NUMERIC), 0::NUMERIC)
							END
						, CASE WHEN p.pa <> 0
							THEN vec_coalesce(vec_mul(vec_sub(p.n_a, p.data_a), p.pa::NUMERIC), 0::NUMERIC)
							ELSE vec_coalesce(vec_mul(p.data_a, 0::NUMERIC), 0::NUMERIC)
							END
					))
					END AS c
			FROM p0 p
			-- so the aggregate reads (and spills) only these columns
			OFFSET 0
		) p
		GROUP BY p.slot
	)
	-- per hour: its first and last slots, and which properties any of its datum has
	, sh AS (
		SELECT sa.hr
			, min(sa.slot) AS fs
			, max(sa.slot) AS ls
			, bool_or(sa.acc IS NOT NULL) AS has_a
			, bool_or(sa.a_at_slot) FILTER (WHERE sa.slot = sa.hr) AS a_at_hr
			, vec_to_max(vec_mul(sa.acc, 0::NUMERIC)) AS a_mask
		FROM sa
		GROUP BY sa.hr
	)
	-- with the hours before and after
	, sh2 AS (
		SELECT sh.*
			, COALESCE(lag(sh.hr) OVER o = sh.hr - INTERVAL 'PT1H', FALSE) AS prev_rows
			, COALESCE(lag(sh.hr) OVER o = sh.hr - INTERVAL 'PT1H' AND lag(sh.has_a) OVER o, FALSE) AS prev_a
			, COALESCE(lead(sh.hr) OVER o = sh.hr + INTERVAL 'PT1H' AND lead(sh.has_a) OVER o, FALSE) AS next_a
			, COALESCE(lead(sh.hr) OVER o = sh.hr + INTERVAL 'PT1H' AND lead(sh.a_at_hr) OVER o, FALSE) AS end_a
		FROM sh
		WINDOW o AS (ORDER BY sh.hr)
	)
	-- status values per slot and property; JSON null counts as no value
	, ws AS (
		SELECT date_bin(b.slot_len, d.ts, TIMESTAMPTZ '2000-01-03 00:00:00+00') AS slot
			, p.idx AS idx
			, NULLIF(d.data_s->(p.idx - 1), 'null'::JSONB) AS val
		FROM b, d
		INNER JOIN generate_series(1, jsonb_array_length(d.data_s)) AS p(idx) ON TRUE
		WHERE d.data_s IS NOT NULL AND d.ts >= start_ts AND d.ts < b.he
	)
	-- the most frequent value of each status property per slot
	, ds AS (
		SELECT ws.slot, ws.idx, mode() WITHIN GROUP (ORDER BY ws.val) AS val
		FROM ws
		GROUP BY ws.slot, ws.idx
	)
	, ds_ary AS (
		SELECT ds.slot, jsonb_agg(ds.val ORDER BY ds.idx) AS data_s
		FROM ds
		GROUP BY ds.slot
	)
	-- the distinct tags per slot
	, dt_ary AS (
		SELECT date_bin(b.slot_len, d.ts, TIMESTAMPTZ '2000-01-03 00:00:00+00') AS slot
			, array_agg(DISTINCT p.val) AS data_t
		FROM b, d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL AND d.ts >= start_ts AND d.ts < b.he
		GROUP BY 1
	)
	SELECT
		  sid AS stream_id
		, r.slot AS ts_start
		, r.data_i
		, r.data_a
		, ds_ary.data_s
		, dt_ary.data_t
		, r.stat_i
		, NULL::NUMERIC[][] AS read_a
	FROM (
		SELECT sa.slot
			, sa.data_i
			, CASE
				-- a property none of the slot's datum has is NULL when another datum of the hour has
				-- it, and zero when none has
				WHEN sa.acc IS NOT NULL THEN vec_trim_scale(CASE
					WHEN array_position(sa.acc, NULL) IS NULL THEN sa.acc
					ELSE vec_add(vec_coalesce(sa.acc, 0::NUMERIC), vec_without_outliers(
						vec_sub(vec_coalesce(vec_mul(sa.acc, 0::NUMERIC), 1::NUMERIC)
							, vec_coalesce(sh2.a_mask, 1::NUMERIC))
						, array_fill(0::NUMERIC, ARRAY[b.len_a]), array_fill(0::NUMERIC, ARRAY[b.len_a])))
					END)
				-- the first slot of an hour gets the reading of the datum before the hour, and the
				-- last slot the reading of a datum at the end of the hour
				WHEN (sa.slot = sh2.fs AND sh2.prev_a AND (sh2.has_a OR sh2.next_a))
						OR (sa.slot = sh2.ls AND sh2.end_a)
					THEN array_fill(0::NUMERIC, ARRAY[b.len_a])
				END AS data_a
			-- [count, min, max] per property, as solarhist.array_transpose2d() would give but
			-- without the cost of a function call per slot
			, (SELECT array_agg(ARRAY[x.c, x.mn, x.mx] ORDER BY x.idx)
				FROM unnest(sa.cnt_i, sa.min_i, sa.max_i) WITH ORDINALITY AS x(c, mn, mx, idx)) AS stat_i
		FROM b, sa
		INNER JOIN sh2 ON sh2.hr = sa.hr
		WHERE sa.slot >= start_ts AND sa.slot < end_ts

		UNION ALL

		-- an hour without datum, but with a datum with accumulating values exactly at its end, gets
		-- a row in its last slot
		SELECT sh2.hr - b.slot_len AS slot
			, NULL::NUMERIC[] AS data_i
			, array_fill(0::NUMERIC, ARRAY[b.len_a]) AS data_a
			, NULL::NUMERIC[][] AS stat_i
		FROM b, sh2
		WHERE sh2.a_at_hr AND NOT sh2.prev_rows
			AND sh2.hr - b.slot_len >= start_ts AND sh2.hr - b.slot_len < end_ts
	) r
	LEFT OUTER JOIN ds_ary ON ds_ary.slot = r.slot
	LEFT OUTER JOIN dt_ary ON dt_ary.slot = r.slot
	WHERE r.data_i IS NOT NULL OR r.data_a IS NOT NULL OR ds_ary.data_s IS NOT NULL OR dt_ary.data_t IS NOT NULL
$$;


/* ============================================================================
 * Daily and monthly rollup
 * ============================================================================
 */

/**
 * Find the aggregate rows of a stream within a time range.
 *
 * Ported from the SolarNet solardatm.find_agg_datm_for_time_span() function.
 *
 * @param sid 		the stream ID
 * @param start_ts	the minimum time (inclusive)
 * @param end_ts 	the maximum time (exclusive)
 * @param kind 		the aggregate kind: `h`, `d` or `M` for hourly, daily or monthly
 * @returns the aggregate rows
 */
CREATE OR REPLACE FUNCTION solarhist.dh_find_agg_datum_for_time_span(
		sid 		INTEGER,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE,
		kind 		CHARACTER
	) RETURNS SETOF solarhist.dh_agg_datum LANGUAGE plpgsql STABLE ROWS 2000 AS
$$
BEGIN
	RETURN QUERY EXECUTE format(
		'SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a '
		'FROM solarhist.%I '
		'WHERE stream_id = $1 AND ts_start >= $2 AND ts_start < $3'
		, 'dh_agg_' || CASE kind WHEN 'd' THEN 'daily' WHEN 'M' THEN 'monthly' ELSE 'hourly' END)
	USING sid, start_ts, end_ts;
END
$$;


/**
 * Compute the aggregate of a time span from lower level aggregate rows: a day from hours, or
 * a month from days.
 *
 * Means are weighted by count, and clock accumulations and reading differences are summed. The
 * starting reading of each accumulating property is that of the first row that has one, and the
 * ending reading that of the last, preferring rows whose accumulation is not NULL (NET-544). An
 * hour in which the stream's datum did not carry a property has an accumulation of 0 for it but
 * no reading, so taking the readings from the first and last row alone gave a day that starts or
 * ends with such an hour, or a month that starts or ends with such a day, a NULL reading.
 *
 * Ported from the SolarNet solardatm.rollup_agg_data_for_time_span() function, as of NET-544.
 *
 * @param sid 		the stream ID
 * @param start_ts	the start of the span (inclusive)
 * @param end_ts 	the end of the span (exclusive)
 * @param kind 		the aggregate kind to roll up: `h` or `d` for hourly or daily
 * @returns one row, or none if the span has no aggregate rows
 * @see solarhist.dh_find_agg_datum_for_time_span()
 */
CREATE OR REPLACE FUNCTION solarhist.dh_rollup_agg_data_for_time_span(
		sid 			INTEGER,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		kind 			CHARACTER
	) RETURNS SETOF solarhist.dh_agg_datum LANGUAGE SQL STABLE ROWS 500 AS
$$
	WITH d AS (
		SELECT * FROM solarhist.dh_find_agg_datum_for_time_span(
			sid,
			start_ts,
			end_ts,
			kind
		)
	)
	-- instantaneous values per property
	, wi AS (
		SELECT
			  p.idx
			, d.data_i[p.idx] AS val
			, d.stat_i[p.idx][1] AS cnt
			, d.stat_i[p.idx][2] AS min
			, d.stat_i[p.idx][3] AS max
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_i, 1)) AS p(idx) ON TRUE
	)
	, di AS (
		SELECT
			idx
			, sum(val * cnt) AS val
			, sum(cnt) AS cnt
			, min(min) AS val_min
			, max(max) AS val_max
		FROM wi
		GROUP BY idx
	)
	, di_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val / cnt ORDER BY idx)) AS data_i
			, array_agg(
				ARRAY[cnt, val_min, val_max] ORDER BY idx
			) AS stat_i
		FROM di
	)
	-- accumulating values per property
	, wa AS (
		SELECT
			  p.idx
			, d.ts_start
			, d.data_a[p.idx] AS val
			, d.read_a[p.idx][1] AS rdiff
			, d.read_a[p.idx][2] AS rstart
			, d.read_a[p.idx][3] AS rend
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_a, 1)) AS p(idx) ON TRUE
	)
	-- accumulating statistics per property; the start and end readings are those of the first
	-- and last row that has them, taking rows whose accumulation is not NULL first (NET-544)
	, da AS (
		SELECT
			idx
			, sum(val) AS val
			, sum(rdiff) AS rdiff
			, (array_agg(rstart ORDER BY val IS NULL, ts_start) FILTER (WHERE rstart IS NOT NULL))[1] AS rstart
			, (array_agg(rend ORDER BY val IS NULL, ts_start DESC) FILTER (WHERE rend IS NOT NULL))[1] AS rend
		FROM wa
		GROUP BY idx
	)
	, da_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val ORDER BY idx)) AS data_a
			, array_agg(
				ARRAY[rdiff, rstart, rend] ORDER BY idx
			) AS read_a
		FROM da
	)
	-- status values per property; JSON null counts as no value
	, ws AS (
		SELECT
			  p.idx AS idx
			, NULLIF(d.data_s->(p.idx - 1), 'null'::JSONB) AS val
		FROM d
		INNER JOIN generate_series(1, jsonb_array_length(d.data_s)) AS p(idx) ON TRUE
		WHERE d.data_s IS NOT NULL
	)
	-- the most frequent value of each status property
	, ds AS (
		SELECT
			  ws.idx
			, mode() WITHIN GROUP (ORDER BY ws.val) AS val
		FROM ws
		GROUP BY ws.idx
	)
	, ds_ary AS (
		SELECT
			  jsonb_agg(val ORDER BY idx) AS data_s
		FROM ds
	)
	-- the distinct tags
	, dt_ary AS (
		SELECT
			  array_agg(DISTINCT p.val) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL
	)
	SELECT
		sid AS stream_id,
		start_ts AS ts_start
		, di_ary.data_i
		, da_ary.data_a
		, ds_ary.data_s
		, dt_ary.data_t
		, di_ary.stat_i
		, da_ary.read_a
	FROM di_ary, da_ary, ds_ary, dt_ary
	WHERE data_i IS NOT NULL OR data_a IS NOT NULL OR data_s IS NOT NULL OR data_t IS NOT NULL
$$;


/* ============================================================================
 * Stale processing
 * ============================================================================
 */

/**
 * Take one row of a given kind off the `solarhist.dh_agg_stale` queue, compute that aggregate
 * and store it, and add the period that contains it at the next level to the queue: the day
 * of an hour, the month of a day.
 *
 * If the period has no data its aggregate row is deleted. Day and month rows in the period
 * that start at a different time, left from a previous time zone, are deleted too.
 *
 * An hour and its 15 minute rows are computed together by
 * `solarhist.dh_rollup_datum_15m_for_hour()`, and both are stored in one statement: the hour as
 * before, then each 15 minute row that is new or has changed, while any 15 minute row of the
 * hour that was not computed is deleted, as are all of them when the hour has no row. A 15
 * minute row is changed when any of its values is distinct from the stored one, so an hour
 * processed again unchanged rewrites its hourly row but none of its 15 minute rows. As in
 * SolarNet's NET-540, without its "complete from" boundary: every hour is processed this way.
 *
 * An hour that starts before the oldest `solarhist.dh_datum` partition is not computed, and
 * the queue row is just removed: its raw datum have been dropped, and computing it again would
 * erase the retained aggregate and its 15 minute rows, which are both kept.
 * `solarhist.dh_datum` is locked before its partitions are looked at, so a partition dropped
 * with `DROP TABLE`, or detached without `CONCURRENTLY`, waits until the hour is computed. A
 * concurrent detach does not wait, so is not safe to use.
 *
 * The yearly `solarhist.dh_agg_hourly` and `solarhist.dh_agg_15m` partitions of an hour that
 * is computed are created first, if needed, by `solarhist.dh_agg_ensure_partition()`, the
 * hourly one first. Their table locks are the last locks the hour takes before it stores its
 * rows, after the queue row, the stream's advisory lock and `solarhist.dh_datum`, always in
 * that order, and no transaction that waits for one of those holds a lock that conflicts with
 * them, so it cannot deadlock. They are only waited for when a partition does not exist yet,
 * by another transaction creating the same partition.
 *
 * Ported from the SolarNet solardatm.process_one_agg_stale_datm() function, without the audit
 * and SolarFlux parts.
 *
 * @param kind the aggregate kind: `h`, `d` or `M` for hourly, daily or monthly
 * @returns the queue row taken, or no row if the queue has none of that kind
 * @see solarhist.dh_rollup_datum_15m_for_hour()
 * @see solarhist.dh_rollup_agg_data_for_time_span()
 */
CREATE OR REPLACE FUNCTION solarhist.dh_process_one_agg_stale(kind CHARACTER)
	RETURNS TABLE (
		stream_id	INTEGER,
		ts_start	TIMESTAMP WITH TIME ZONE,
		agg_kind	CHARACTER
	) LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	agg_span 				INTERVAL;
	dest_name				TEXT;

	stale_sid				INTEGER;
	stale_ts				TIMESTAMP WITH TIME ZONE;
	tz						TEXT;

	local_ts_start			TIMESTAMP;
	local_ts_end			TIMESTAMP;
	ts_end					TIMESTAMP WITH TIME ZONE;
	ts_prevstart			TIMESTAMP WITH TIME ZONE;
	raw_start				TIMESTAMP WITH TIME ZONE;

	num_rows				BIGINT;
BEGIN
	CASE kind
		WHEN 'd' THEN
			agg_span := INTERVAL 'P1D';
			dest_name := 'dh_agg_daily';
		WHEN 'M' THEN
			agg_span := INTERVAL 'P1M';
			dest_name := 'dh_agg_monthly';
		ELSE
			agg_span := INTERVAL 'PT1H';
			dest_name := 'dh_agg_hourly';
	END CASE;

	-- lock one queue row, skipping any another transaction has locked
	SELECT s.stream_id, s.ts_start
	FROM solarhist.dh_agg_stale s
	WHERE s.agg_kind = kind
	FOR UPDATE SKIP LOCKED
	LIMIT 1
	INTO stale_sid, stale_ts;

	IF NOT FOUND THEN
		RETURN;
	END IF;

	IF kind = 'h' THEN
		-- wait for any transaction that has stored a datum in the stream to end, so every
		-- datum that marked this hour stale is committed and seen below; see
		-- solarhist.dh_datum_mark_stale()
		PERFORM pg_advisory_xact_lock(hashtext('solarhist.dh_agg_stale'), stale_sid);
	END IF;

	DELETE FROM solarhist.dh_agg_stale s
	WHERE s.agg_kind = kind
		AND s.ts_start = stale_ts
		AND s.stream_id = stale_sid;

	stream_id := stale_sid;
	ts_start := stale_ts;
	agg_kind := kind;

	-- leave an hour whose raw datum have been dropped as it is
	IF kind = 'h' THEN
		-- dropping a partition locks the partitioned table, so from here it waits for this
		-- transaction to end, and the partitions found next are those the hour is computed from
		LOCK TABLE ONLY solarhist.dh_datum IN ACCESS SHARE MODE;
		raw_start := solarhist.dh_datum_oldest_partition_start();
		IF raw_start IS NULL OR stale_ts < raw_start THEN
			RETURN NEXT;
			RETURN;
		END IF;
	END IF;

	tz := solarhist.dh_stream_time_zone(stale_sid);

	-- local start and end, to work with calendar intervals; ts_prevstart is used to find rows
	-- left from a previous time zone
	local_ts_start := stale_ts AT TIME ZONE tz;
	local_ts_end   := local_ts_start + agg_span;
	ts_end         := CASE kind WHEN 'h' THEN stale_ts + agg_span ELSE local_ts_end AT TIME ZONE tz END;
	ts_prevstart   := CASE kind WHEN 'h' THEN stale_ts - agg_span ELSE (local_ts_start - agg_span) AT TIME ZONE tz END;

	IF kind = 'h' THEN
		-- the hour's yearly partitions, created after the check above so that a skipped hour
		-- creates none, the hourly one first; this is the only place hourly and 15 minute rows
		-- are stored
		PERFORM solarhist.dh_agg_ensure_partition('dh_agg_hourly', stale_ts);
		PERFORM solarhist.dh_agg_ensure_partition('dh_agg_15m', stale_ts);

		-- store the hour and its 15 minute rows, all computed together; a 15 minute row is
		-- rewritten only when it has changed, and those of the hour not computed are deleted (all
		-- of them, when the hour has no row)
		EXECUTE
				'WITH r AS ('
				'    SELECT * FROM solarhist.dh_rollup_datum_15m_for_hour($1, $2, $3)'
				')'
				', h AS ('
				'    INSERT INTO solarhist.dh_agg_hourly (stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a) '
				'    SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a '
				'    FROM r '
				'    ON CONFLICT (stream_id, ts_start) DO UPDATE SET '
				'        data_i = EXCLUDED.data_i, '
				'        data_a = EXCLUDED.data_a, '
				'        data_s = EXCLUDED.data_s, '
				'        data_t = EXCLUDED.data_t, '
				'        stat_i = EXCLUDED.stat_i, '
				'        read_a = EXCLUDED.read_a '
				'    RETURNING 1'
				')'
				', s AS ('
				'    SELECT x.* FROM r, unnest(r.slots) x'
				')'
				', su AS ('
				'    INSERT INTO solarhist.dh_agg_15m (stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a) '
				'    SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a '
				'    FROM s '
				'    ON CONFLICT (stream_id, ts_start) DO UPDATE SET '
				'        data_i = EXCLUDED.data_i, '
				'        data_a = EXCLUDED.data_a, '
				'        data_s = EXCLUDED.data_s, '
				'        data_t = EXCLUDED.data_t, '
				'        stat_i = EXCLUDED.stat_i, '
				'        read_a = EXCLUDED.read_a '
				'    WHERE (dh_agg_15m.data_i, dh_agg_15m.data_a, dh_agg_15m.data_s, '
				'            dh_agg_15m.data_t, dh_agg_15m.stat_i, dh_agg_15m.read_a) '
				'        IS DISTINCT FROM (EXCLUDED.data_i, EXCLUDED.data_a, EXCLUDED.data_s, '
				'            EXCLUDED.data_t, EXCLUDED.stat_i, EXCLUDED.read_a)'
				')'
				', sd AS ('
				'    DELETE FROM solarhist.dh_agg_15m d '
				'    WHERE d.stream_id = $1 AND d.ts_start >= $2 AND d.ts_start < $3 '
				'        AND NOT EXISTS (SELECT 1 FROM s WHERE s.ts_start = d.ts_start)'
				')'
				'SELECT count(*) FROM h'
		INTO num_rows
		USING stale_sid, stale_ts, ts_end;
	ELSE
		EXECUTE format(
				'INSERT INTO solarhist.%I (stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a) '
				'SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a '
				'FROM solarhist.dh_rollup_agg_data_for_time_span($1, $2, $3, $4) '
				'ON CONFLICT (stream_id, ts_start) DO UPDATE SET '
				'    data_i = EXCLUDED.data_i, '
				'    data_a = EXCLUDED.data_a, '
				'    data_s = EXCLUDED.data_s, '
				'    data_t = EXCLUDED.data_t, '
				'    stat_i = EXCLUDED.stat_i, '
				'    read_a = EXCLUDED.read_a'
				, dest_name)
		USING stale_sid, stale_ts, ts_end, CASE kind WHEN 'M' THEN 'd' ELSE 'h' END;
		GET DIAGNOSTICS num_rows = ROW_COUNT;
	END IF;

	IF num_rows < 1 THEN
		-- delete everything within the span, using > ts_prevstart to handle time zone changes
		EXECUTE format(
				'DELETE FROM solarhist.%I '
				'WHERE stream_id = $1 AND ts_start > $2 AND ts_start < $3'
				, dest_name)
		USING stale_sid, ts_prevstart, ts_end;
	ELSIF kind <> 'h' THEN
		-- delete everything but the stored row, using > ts_prevstart to handle time zone changes
		EXECUTE format(
				'DELETE FROM solarhist.%I '
				'WHERE stream_id = $1 AND ts_start > $2 AND ts_start < $3 '
				'    AND ts_start <> $4'
				, dest_name)
		USING stale_sid, ts_prevstart, ts_end, stale_ts;
	END IF;

	-- compute the next level again
	CASE kind
		WHEN 'h' THEN
			INSERT INTO solarhist.dh_agg_stale (stream_id, ts_start, agg_kind)
			VALUES (stale_sid, date_trunc('day', local_ts_start) AT TIME ZONE tz, 'd')
			ON CONFLICT DO NOTHING;

		WHEN 'd' THEN
			INSERT INTO solarhist.dh_agg_stale (stream_id, ts_start, agg_kind)
			VALUES (stale_sid, date_trunc('month', local_ts_start) AT TIME ZONE tz, 'M')
			ON CONFLICT DO NOTHING;

		ELSE
			-- nothing above a month
	END CASE;

	RETURN NEXT;
END
$$;


/**
 * Drain the `solarhist.dh_agg_stale` queue: hours, then days, then months, as each level adds
 * to the queue of the next. Each row is computed in a transaction of its own, so no lock is
 * held for long.
 *
 * Only one call runs at a time: each transaction takes an advisory lock that other calls
 * check for, and a call that finds it taken returns. Call it with `CALL`, outside an explicit
 * transaction; `pg_cron` runs it this way (see postgres-init-agg-cron.sql).
 *
 * @param max_rows the most queue rows of each kind to process in this call
 */
CREATE OR REPLACE PROCEDURE solarhist.dh_process_agg_stale(max_rows INTEGER DEFAULT 100000)
	LANGUAGE plpgsql AS
$$
DECLARE
	k		CHARACTER;
	n		INTEGER;
	got		BOOLEAN;
BEGIN
	FOREACH k IN ARRAY ARRAY['h', 'd', 'M']::CHARACTER[] LOOP
		n := 0;
		LOOP
			EXIT WHEN n >= max_rows;

			IF NOT pg_try_advisory_xact_lock(hashtext('solarhist.dh_process_agg_stale')) THEN
				-- another call is processing the queue
				RETURN;
			END IF;

			SELECT count(*) > 0 FROM solarhist.dh_process_one_agg_stale(k) INTO got;
			COMMIT;

			EXIT WHEN NOT got;
			n := n + 1;
		END LOOP;
	END LOOP;
END
$$;


/* ============================================================================
 * Reading difference, with hourly readings where raw datum have been dropped
 * ============================================================================
 */

/**
 * Find the hourly reading for a time that has no raw datum with accumulating values at or
 * before it, as steps 2 and 3 of `solarhist.dh_find_datum_diff_near_rows()` choose it.
 *
 * It returns the ending readings (`read_a[n][3]`) of the latest complete hour that ends at or
 * before the time, as a row at the end of that hour. Failing that, it returns the starting
 * readings (`read_a[n][2]`) of the earliest complete hour that starts at or after the time and
 * ends before `next_ts`, as a row at the start of that hour. Only hours with readings within
 * the tolerance of the time are considered.
 *
 * This is a function, as `solarhist.dh_find_datum_between()` is for `solarhist.dh_datum`, so
 * that the planner never plans the hourly partitions as part of the calling query. The hours
 * are bounded by times computed with `TIMESTAMP WITH TIME ZONE` arithmetic, which is STABLE,
 * so the planner cannot prune partitions with those bounds while planning. Looked up in the
 * calling query, every yearly partition would be planned for each lookup whenever a reading
 * difference is planned, even one that needs no hourly reading, and pruned only when the
 * query starts: with 25 partitions, that adds up to 0.5 ms to a reading difference. Here the
 * bounds are computed first, so each query compares `ts_start` with plain values. It is
 * planned for just the partitions they fall in, or pruned to them when it starts if its plan
 * is cached, and it runs only for a time that needs it.
 *
 * @param sid 		the stream ID
 * @param ts_at 	the time
 * @param tolerance the maximum time to look before and after `ts_at`
 * @param next_ts	the time of the first raw datum after `ts_at`, or `infinity` if there is none
 * @returns at most one row, with only the accumulating values
 * @see solarhist.dh_find_datum_diff_near_rows()
 */
CREATE OR REPLACE FUNCTION solarhist.dh_find_hourly_reading_near_ts(
		sid 		INTEGER,
		ts_at 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL,
		next_ts		TIMESTAMP WITH TIME ZONE
	) RETURNS SETOF solarhist.dh_datum_rec LANGUAGE plpgsql STRICT STABLE ROWS 1 AS
$$
DECLARE
	-- the start of a complete hour that ends at or before ts_at, within the tolerance
	before_min	CONSTANT TIMESTAMP WITH TIME ZONE := ts_at - INTERVAL 'PT1H' - tolerance;
	before_max	CONSTANT TIMESTAMP WITH TIME ZONE := ts_at - INTERVAL 'PT1H';

	-- the latest start of a complete hour at or after ts_at, within the tolerance
	after_max	CONSTANT TIMESTAMP WITH TIME ZONE := ts_at + tolerance;
BEGIN
	RETURN QUERY
	SELECT h.stream_id
		, h.ts_start + INTERVAL 'PT1H'
		, NULL::NUMERIC[]
		, ARRAY(SELECT h.read_a[i][3] FROM generate_subscripts(h.read_a, 1) AS i ORDER BY i)
		, NULL::JSONB
		, NULL::TEXT[]
	FROM solarhist.dh_agg_hourly h
	WHERE h.stream_id = sid
		AND h.ts_start >= before_min
		AND h.ts_start <= before_max
		AND h.read_a IS NOT NULL
	ORDER BY h.ts_start DESC
	LIMIT 1;

	IF FOUND THEN
		RETURN;
	END IF;

	RETURN QUERY
	SELECT h.stream_id
		, h.ts_start
		, NULL::NUMERIC[]
		, ARRAY(SELECT h.read_a[i][2] FROM generate_subscripts(h.read_a, 1) AS i ORDER BY i)
		, NULL::JSONB
		, NULL::TEXT[]
	FROM solarhist.dh_agg_hourly h
	WHERE h.stream_id = sid
		AND h.ts_start >= ts_at
		AND h.ts_start <= after_max
		AND h.ts_start + INTERVAL 'PT1H' < next_ts
		AND h.read_a IS NOT NULL
	ORDER BY h.ts_start
	LIMIT 1;
END
$$;


/**
 * Find the datum for calculating the reading difference between two times, within a
 * tolerance, falling back to hourly readings where the raw datum have been dropped.
 *
 * This replaces the postgres-init.sql version of this function. For each of the two times it
 * chooses a reading as follows, considering only datum with accumulating values:
 *
 * 1. The raw datum at that time, or the last one before it, as the postgres-init.sql version
 *    and SolarNet choose it.
 * 2. Otherwise, if a complete hour that ends at or before that time has an hourly row, the
 *    ending readings (`read_a[n][3]`) of the latest such hour, as a row at the end of that
 *    hour. This is the last reading at or before the end of the hour, so for a time on an hour
 *    boundary it is the same reading the raw datum would have given.
 * 3. Otherwise the first raw datum after that time, as SolarNet chooses it, unless a complete
 *    hour that starts at or after that time ends before that datum (or there is no such
 *    datum): then the starting readings (`read_a[n][2]`) of the earliest such hour, as a row
 *    at the start of that hour.
 *
 * The hourly choices in steps 2 and 3 can only apply where raw datum have been removed but
 * their hourly rows kept, as happens when old `solarhist.dh_datum` partitions are dropped:
 * while the raw datum exist, every hourly row has a raw datum within it, at or before its end,
 * so the raw choice always applies first. The result is therefore the same as SolarNet's
 * wherever no raw datum have been dropped.
 *
 * The two times are chosen independently, so one end of a result can come from the hourly
 * rows and the other from raw datum; the difference is then the raw reading minus the hourly
 * one. When a time is not on an hour boundary and its raw datum have been dropped, the reading
 * used is the one at the hour boundary before it (step 2), or after it when no hour before it
 * is kept (step 3), and the row's time is that boundary.
 *
 * The hourly readings of steps 2 and 3 are looked up by
 * `solarhist.dh_find_hourly_reading_near_ts()`, and only for a time without a raw datum at or
 * before it. They are not looked up by this query itself, which the planner inlines into the
 * calling query, because then every hourly partition would be planned with it; that function
 * explains why.
 *
 * @param sid 		the stream ID
 * @param start_ts	the starting time
 * @param end_ts 	the ending time
 * @param tolerance the maximum time to look before and after each time
 * @returns up to two datum rows, with only the accumulating values
 * @see solarhist.dh_find_datum_diff_rows()
 * @see solarhist.dh_find_hourly_reading_near_ts()
 */
CREATE OR REPLACE FUNCTION solarhist.dh_find_datum_diff_near_rows(
		sid 			INTEGER,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		tolerance 		INTERVAL DEFAULT INTERVAL 'P3M'
	) RETURNS SETOF solarhist.dh_datum_rec LANGUAGE SQL STABLE ROWS 10 AS
$$
	-- find if the stream has accumulating properties at all, to avoid a costly scan
	WITH meta AS (
		SELECT COALESCE(CARDINALITY(names_a) = 0, TRUE) AS has_no_a
		FROM solarhist.dh_datum_meta
		WHERE stream_id = sid
	)
	-- the earliest of the raw datum around each time, which may be an exact match
	, raw_start AS (
		SELECT d.stream_id, d.ts, d.data_a
		FROM meta, solarhist.dh_find_datum_around_ts(sid, start_ts, tolerance, TRUE, meta.has_no_a) AS d
		ORDER BY d.ts
		LIMIT 1
	)
	, raw_end AS (
		SELECT d.stream_id, d.ts, d.data_a
		FROM meta, solarhist.dh_find_datum_around_ts(sid, end_ts, tolerance, TRUE, meta.has_no_a) AS d
		ORDER BY d.ts
		LIMIT 1
	)
	-- the hourly reading for each time, only when there is no raw datum at or before that time:
	-- that of the latest complete hour before it, else that of the earliest one after it that
	-- ends before the raw datum after it; looked up by a function, so that the planner of this
	-- query never plans the hourly partitions
	, hour_start AS (
		SELECT h.stream_id, h.ts, h.data_a
		FROM meta, solarhist.dh_find_hourly_reading_near_ts(sid, start_ts, tolerance
				, COALESCE((SELECT raw_start.ts FROM raw_start), 'infinity')) AS h
		WHERE NOT meta.has_no_a
			AND NOT EXISTS (SELECT 1 FROM raw_start WHERE raw_start.ts <= start_ts)
	)
	, hour_end AS (
		SELECT h.stream_id, h.ts, h.data_a
		FROM meta, solarhist.dh_find_hourly_reading_near_ts(sid, end_ts, tolerance
				, COALESCE((SELECT raw_end.ts FROM raw_end), 'infinity')) AS h
		WHERE NOT meta.has_no_a
			AND NOT EXISTS (SELECT 1 FROM raw_end WHERE raw_end.ts <= end_ts)
	)
	-- one reading for each time: raw at or before it, else an hourly row before it, else an
	-- hourly row after it that is earlier than the raw datum after it, else that raw datum
	, d AS (
		(
			SELECT * FROM raw_start WHERE ts <= start_ts
			UNION ALL
			SELECT * FROM hour_start
			UNION ALL
			SELECT * FROM raw_start WHERE ts > start_ts
				AND NOT EXISTS (SELECT 1 FROM hour_start)
		)
		UNION
		(
			SELECT * FROM raw_end WHERE ts <= end_ts
			UNION ALL
			SELECT * FROM hour_end
			UNION ALL
			SELECT * FROM raw_end WHERE ts > end_ts
				AND NOT EXISTS (SELECT 1 FROM hour_end)
		)
	)
	-- drop any leading row before the last one at or before the start, and any trailing row
	-- after the last one at or before the end
	, ts_range AS (
		SELECT COALESCE(max(ts) FILTER (WHERE ts <= start_ts), start_ts) AS min_ts
			, COALESCE(max(ts) FILTER (WHERE ts <= end_ts), end_ts) AS max_ts
		FROM d
	)
	SELECT d.stream_id
		, d.ts
		, NULL::NUMERIC[] AS data_i
		, d.data_a
		, NULL::JSONB AS data_s
		, NULL::TEXT[] AS data_t
	FROM d, ts_range
	WHERE d.ts >= ts_range.min_ts AND d.ts <= ts_range.max_ts
$$;

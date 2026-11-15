/* ============================================================================
 * SQL script to add stored Hour, Day and Month aggregates to the JDBC
 * DatumHistorian. Run it after postgres-init.sql, in the same schema.
 *
 * Aggregate support is chosen when a historian schema is set up: run this
 * script then or not at all. Like postgres-init.sql it uses the solarhist
 * schema, and it is applied to another schema such as solaruhist in the same
 * way.
 *
 * This is a port of SolarNet's stale-queue design: the solardatm aggregate
 * tables and rollup functions, with integer stream IDs, without SolarNet's
 * Reset auxiliary records, audit and SolarFlux parts. SolarNet's query SQL can
 * therefore be ported to read these tables.
 *
 * - The dh_agg_hourly, dh_agg_daily and dh_agg_monthly tables have the same
 *   columns as SolarNet's agg_datm_hourly, agg_datm_daily and agg_datm_monthly
 *   tables, except that data_s is a JSON array, like solarhist.dh_datum.data_s.
 * - A trigger on solarhist.dh_datum adds the hours a stored, changed or
 *   deleted datum affects to the dh_agg_stale queue.
 * - The solarhist.dh_process_agg_stale() procedure drains the queue: hours,
 *   then the days they are in, then the months. The postgres-init-agg-cron.sql
 *   script schedules it with pg_cron.
 * - Hours are UTC-aligned. Days and months start at midnight in the time zone
 *   of the stream's node, from the dh_node_time_zone table, falling back to
 *   UTC.
 * - Raw datum partitions may be dropped while the aggregates are kept: an hour
 *   that starts before the oldest solarhist.dh_datum partition is never
 *   recomputed. Drop a partition with DROP TABLE, or detach it without
 *   CONCURRENTLY: either waits for an hour being computed to commit.
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
);

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

-- time zone of each node, for the days and months of its streams, including location streams
-- stored by the node; a node without a row uses UTC, as SolarNet does, so the result never
-- depends on the session that processes the stale queue
--
-- Adding, changing or removing a row marks the days of the node's streams stale, so they and
-- their months are computed again from the hourly rows. An invalid zone is rejected.
CREATE TABLE solarhist.dh_node_time_zone (
	node_id		BIGINT NOT NULL,
	time_zone	TEXT NOT NULL,
	updated		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT dh_node_time_zone_pk PRIMARY KEY (node_id)
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
 * and months in the old zone are deleted as the new ones are computed.
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
 * A datum exactly at the end of the slot is included, and then no datum after it is looked
 * for. A datum exactly at the start of the slot with accumulating values is the starting
 * reading, and then no datum before it is looked for. If the slot has no datum, nothing is
 * returned.
 *
 * Ported from the SolarNet solardatm.find_datm_for_time_slot() function, without Reset
 * records. Without them SolarNet's target_agg parameter has no effect, so it is left out.
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
	-- the datum before and after the slot
	, srange AS (
		SELECT COALESCE(t.min_ts, drange.min_ts, start_ts) AS min_ts, COALESCE(t.max_ts, drange.max_ts, end_ts) AS max_ts
		FROM drange, (
			SELECT COALESCE(
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
			) AS min_ts
			, (
				-- the datum after the last in the slot (or exactly at the end of the slot),
				-- requiring accumulating values, within at most an hour (NET-537)
				SELECT CASE
					WHEN d.ts IS NULL THEN drange.max_ts
					WHEN drange.max_ts = end_ts THEN drange.max_ts
					ELSE d.ts
				END
				FROM meta, drange, solarhist.dh_find_time_after_ts(sid, drange.max_ts, end_ts + LEAST(tolerance, INTERVAL 'PT1H'), TRUE, meta.has_no_a) AS d(ts)
			) AS max_ts
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
 * @param sid 				the stream ID
 * @param start_ts			the start of the span (inclusive)
 * @param end_ts 			the end of the span (exclusive)
 * @param tolerance_clock 	the longest time between two readings for their difference to count
 * 							in the clock accumulation
 * @param tolerance_read 	the maximum time to look back for the starting reading
 * @returns one row, or none if the span has no datum
 * @see solarhist.dh_find_datum_for_time_slot()
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
 * Means are weighted by count, clock accumulations and reading differences are summed, the
 * starting reading is that of the first row and the ending reading that of the last. Ported
 * from the SolarNet solardatm.rollup_agg_data_for_time_span() function.
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
			, d.data_a[p.idx] AS val
			, d.read_a[p.idx][1] AS rdiff
			, first_value(d.read_a[p.idx][2]) OVER slot_start AS rstart
			, first_value(d.read_a[p.idx][3]) OVER slot_end AS rend
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_a, 1)) AS p(idx) ON TRUE
		WINDOW slot_start AS (PARTITION BY p.idx ORDER BY CASE WHEN d.data_a[p.idx] IS NULL THEN 1 ELSE 0 END, d.ts_start
				RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING),
			slot_end AS (PARTITION BY p.idx ORDER BY CASE WHEN d.data_a[p.idx] IS NULL THEN 1 ELSE 0 END, d.ts_start DESC
				RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	, da AS (
		SELECT
			idx
			, sum(val) AS val
			, sum(rdiff) AS rdiff
			, min(rstart) AS rstart
			, min(rend) AS rend
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
 * An hour that starts before the oldest `solarhist.dh_datum` partition is not computed, and
 * the queue row is just removed: its raw datum have been dropped, and computing it again would
 * erase the retained aggregate. `solarhist.dh_datum` is locked before its partitions are
 * looked at, so a partition dropped with `DROP TABLE`, or detached without `CONCURRENTLY`,
 * waits until the hour is computed. A concurrent detach does not wait, so is not safe to use.
 *
 * Ported from the SolarNet solardatm.process_one_agg_stale_datm() function, without the audit
 * and SolarFlux parts.
 *
 * @param kind the aggregate kind: `h`, `d` or `M` for hourly, daily or monthly
 * @returns the queue row taken, or no row if the queue has none of that kind
 * @see solarhist.dh_rollup_datum_for_time_span()
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
		EXECUTE format(
				'INSERT INTO solarhist.%I (stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a) '
				'SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a '
				'FROM solarhist.dh_rollup_datum_for_time_span($1, $2, $3) '
				'ON CONFLICT (stream_id, ts_start) DO UPDATE SET '
				'    data_i = EXCLUDED.data_i, '
				'    data_a = EXCLUDED.data_a, '
				'    data_s = EXCLUDED.data_s, '
				'    data_t = EXCLUDED.data_t, '
				'    stat_i = EXCLUDED.stat_i, '
				'    read_a = EXCLUDED.read_a'
				, dest_name)
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
	END IF;
	GET DIAGNOSTICS num_rows = ROW_COUNT;

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
 * @param sid 		the stream ID
 * @param start_ts	the starting time
 * @param end_ts 	the ending time
 * @param tolerance the maximum time to look before and after each time
 * @returns up to two datum rows, with only the accumulating values
 * @see solarhist.dh_find_datum_diff_rows()
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
	-- the ending readings of the latest complete hour at or before each time, only when there is
	-- no raw datum at or before that time
	, hour_start AS (
		SELECT h.stream_id
			, h.ts_start + INTERVAL 'PT1H' AS ts
			, ARRAY(SELECT h.read_a[i][3] FROM generate_subscripts(h.read_a, 1) AS i ORDER BY i) AS data_a
		FROM meta, solarhist.dh_agg_hourly h
		WHERE NOT meta.has_no_a
			AND NOT EXISTS (SELECT 1 FROM raw_start WHERE raw_start.ts <= start_ts)
			AND h.stream_id = sid
			AND h.ts_start <= start_ts - INTERVAL 'PT1H'
			AND h.ts_start >= start_ts - INTERVAL 'PT1H' - tolerance
			AND h.read_a IS NOT NULL
		ORDER BY h.ts_start DESC
		LIMIT 1
	)
	, hour_end AS (
		SELECT h.stream_id
			, h.ts_start + INTERVAL 'PT1H' AS ts
			, ARRAY(SELECT h.read_a[i][3] FROM generate_subscripts(h.read_a, 1) AS i ORDER BY i) AS data_a
		FROM meta, solarhist.dh_agg_hourly h
		WHERE NOT meta.has_no_a
			AND NOT EXISTS (SELECT 1 FROM raw_end WHERE raw_end.ts <= end_ts)
			AND h.stream_id = sid
			AND h.ts_start <= end_ts - INTERVAL 'PT1H'
			AND h.ts_start >= end_ts - INTERVAL 'PT1H' - tolerance
			AND h.read_a IS NOT NULL
		ORDER BY h.ts_start DESC
		LIMIT 1
	)
	-- the starting readings of the earliest complete hour at or after each time that ends before
	-- the raw datum after that time, only when there is no reading at or before that time
	, hour_start_after AS (
		SELECT h.stream_id
			, h.ts_start AS ts
			, ARRAY(SELECT h.read_a[i][2] FROM generate_subscripts(h.read_a, 1) AS i ORDER BY i) AS data_a
		FROM meta, solarhist.dh_agg_hourly h
		WHERE NOT meta.has_no_a
			AND NOT EXISTS (SELECT 1 FROM raw_start WHERE raw_start.ts <= start_ts)
			AND NOT EXISTS (SELECT 1 FROM hour_start)
			AND h.stream_id = sid
			AND h.ts_start >= start_ts
			AND h.ts_start <= start_ts + tolerance
			AND h.ts_start + INTERVAL 'PT1H' < COALESCE((SELECT ts FROM raw_start), 'infinity')
			AND h.read_a IS NOT NULL
		ORDER BY h.ts_start
		LIMIT 1
	)
	, hour_end_after AS (
		SELECT h.stream_id
			, h.ts_start AS ts
			, ARRAY(SELECT h.read_a[i][2] FROM generate_subscripts(h.read_a, 1) AS i ORDER BY i) AS data_a
		FROM meta, solarhist.dh_agg_hourly h
		WHERE NOT meta.has_no_a
			AND NOT EXISTS (SELECT 1 FROM raw_end WHERE raw_end.ts <= end_ts)
			AND NOT EXISTS (SELECT 1 FROM hour_end)
			AND h.stream_id = sid
			AND h.ts_start >= end_ts
			AND h.ts_start <= end_ts + tolerance
			AND h.ts_start + INTERVAL 'PT1H' < COALESCE((SELECT ts FROM raw_end), 'infinity')
			AND h.read_a IS NOT NULL
		ORDER BY h.ts_start
		LIMIT 1
	)
	-- one reading for each time: raw at or before it, else an hourly row before it, else an
	-- hourly row after it that is earlier than the raw datum after it, else that raw datum
	, d AS (
		(
			SELECT * FROM raw_start WHERE ts <= start_ts
			UNION ALL
			SELECT * FROM hour_start
			UNION ALL
			SELECT * FROM hour_start_after
			UNION ALL
			SELECT * FROM raw_start WHERE ts > start_ts
				AND NOT EXISTS (SELECT 1 FROM hour_start)
				AND NOT EXISTS (SELECT 1 FROM hour_start_after)
		)
		UNION
		(
			SELECT * FROM raw_end WHERE ts <= end_ts
			UNION ALL
			SELECT * FROM hour_end
			UNION ALL
			SELECT * FROM hour_end_after
			UNION ALL
			SELECT * FROM raw_end WHERE ts > end_ts
				AND NOT EXISTS (SELECT 1 FROM hour_end)
				AND NOT EXISTS (SELECT 1 FROM hour_end_after)
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

-- NET-540: 24 generated datum streams, for testing the 15 minute rollup against the hourly one.
--
-- A port of SolarNet's net540-generated-streams.sql test fixture, without its Reset records,
-- which the node does not have: streams 11, 12, 18 and 19 keep their datum, and lose only the
-- Reset records that gave them their labels there. The datum are the same kinds as SolarNet's,
-- though not the same values, as the random numbers are drawn in a different order.
--
-- The streams have regular and jittered cadences from 1 minute to 2 hours, gaps and outages,
-- datum that lack some or all accumulating properties, and a second accumulating property that
-- is carried only in part of each hour or never. All run from 2030-03-04 00:00 UTC, for up to 3
-- days, except stream 10, which is offline for 23 days.
--
-- Creates the temporary table t_stream (k, sid, label), dropped at commit, so run this inside a
-- transaction. The tests create t_hours (k, sid, hour) from it, with every hour of each stream's
-- datum plus two hours either side.

SELECT setseed(0.540);

SELECT solarhist.dh_datum_ensure_partition('2030-03-01 00:00:00+00');

CREATE TEMP TABLE t_stream (k INT PRIMARY KEY, sid INT NOT NULL, label TEXT NOT NULL) ON COMMIT DROP;

WITH t(k, label) AS (VALUES
	  ( 1, '1 min, jittered')
	, ( 2, '5 min, on boundaries')
	, ( 3, '15 min, on boundaries')
	, ( 4, '20 min, jittered')
	, ( 5, '30 min, on boundaries')
	, ( 6, '60 min, jittered (55-65)')
	, ( 7, '60 min, on the hour')
	, ( 8, 'random gaps, 1 s to 90 min')
	, ( 9, '1 min with outages to 6 h')
	, (10, 'offline for 23 days')
	, (11, '1 min, jittered (SolarNet: with reset records)')
	, (12, '5 min offset (SolarNet: resets on boundaries)')
	, (13, '2 hours')
	, (14, '45 min, exact')
	, (15, '1 min, some datum lack accumulating')
	, (16, '10 min offset, jittered')
	, (17, '1 min, each property missing at times')
	, (18, '20 min, jittered (SolarNet: pairs of resets between datum)')
	, (19, '5 min, jittered (SolarNet: pairs of resets between datum)')
	, (20, '2nd property only in minutes 0-19')
	, (21, '2nd property only in minutes 40-59')
	, (22, '2nd property only in minutes 20-39')
	, (23, '2nd property never')
	, (24, '2nd property zero, minutes 0-19 only')
)
, m AS (
	INSERT INTO solarhist.dh_datum_meta (node_id, source_id, names_i, names_a, names_s, type_ids)
	SELECT 540002, 'net540/' || k, ARRAY['w','v'], ARRAY['wh','x'], ARRAY['st'], ARRAY[1]::SMALLINT[]
	FROM t
	RETURNING stream_id, source_id
)
INSERT INTO t_stream (k, sid, label)
SELECT t.k, m.stream_id, t.label
FROM t
INNER JOIN m ON m.source_id = 'net540/' || t.k;

CREATE TEMP TABLE t_times (k INT NOT NULL, n INT NOT NULL, ts TIMESTAMPTZ NOT NULL) ON COMMIT DROP;

-- cumulative random gaps, in seconds
INSERT INTO t_times (k, n, ts)
SELECT k, n, '2030-03-04 00:00:00+00'::timestamptz + (sum(gap) OVER (PARTITION BY k ORDER BY n)) * interval '1 second'
FROM (
	SELECT 1 AS k, n, round((50 + random() * 20)::numeric, 3) AS gap FROM generate_series(1, 4400) n
	UNION ALL
	SELECT 4, n, round((900 + random() * 600)::numeric, 3) FROM generate_series(1, 220) n
	UNION ALL
	SELECT 6, n, round((3300 + random() * 600)::numeric, 3) FROM generate_series(1, 75) n
	UNION ALL
	SELECT 8, n, round((1 + random() * 5399)::numeric, 3) FROM generate_series(1, 100) n
	UNION ALL
	SELECT 9, n, CASE WHEN random() < 0.01 THEN round((1200 + random() * 20400)::numeric, 3)
			ELSE round((50 + random() * 20)::numeric, 3) END FROM generate_series(1, 3000) n
	UNION ALL
	SELECT 10, n, CASE WHEN n = 361 THEN 23 * 86400 + 7 * 3600 + 13 * 60 + 0.5
			ELSE round((50 + random() * 20)::numeric, 3) END FROM generate_series(1, 720) n
	UNION ALL
	SELECT 11, n, round((50 + random() * 20)::numeric, 3) FROM generate_series(1, 4400) n
	UNION ALL
	SELECT 15, n, round((50 + random() * 20)::numeric, 3) FROM generate_series(1, 2200) n
	UNION ALL
	SELECT 16, n, round((540 + random() * 120)::numeric, 3) FROM generate_series(1, 430) n
) g;

-- exact cadences
INSERT INTO t_times (k, n, ts)
SELECT k, n, '2030-03-04 00:00:00+00'::timestamptz + off + n * step
FROM (VALUES
	  ( 2, interval '5 minutes',  interval '0',       864)
	, ( 3, interval '15 minutes', interval '0',       288)
	, ( 5, interval '30 minutes', interval '0',       144)
	, ( 7, interval '60 minutes', interval '0',        72)
	, (12, interval '5 minutes',  interval '150 sec', 864)
	, (13, interval '2 hours',    interval '7 min',    36)
	, (14, interval '45 minutes', interval '0',        96)
) AS c(k, step, off, cnt)
, generate_series(0, c.cnt - 1) n;

INSERT INTO solarhist.dh_datum (ts, stream_id, type_id, data_i, data_a, data_s, data_t)
SELECT t.ts, s.sid, 1
	, ARRAY[round((random() * 1000)::numeric, 3), round((random() * 250)::numeric, 1)]
	, ARRAY[sum(round((random() * 10)::numeric, 3)) OVER w, (t.n * 7)::numeric]
	, jsonb_build_array((ARRAY['on','off','idle'])[1 + floor(random() * 3)::int])
	, CASE WHEN random() < 0.1 THEN ARRAY['t' || floor(random() * 3)::int] END
FROM t_times t
INNER JOIN t_stream s USING (k)
WINDOW w AS (PARTITION BY t.k ORDER BY t.n);

-- datum without any accumulating properties
UPDATE solarhist.dh_datum SET data_a = NULL
WHERE stream_id = (SELECT sid FROM t_stream WHERE k = 15) AND random() < 0.15;

-- each accumulating property missing at times
INSERT INTO solarhist.dh_datum (ts, stream_id, type_id, data_i, data_a, data_s)
SELECT g.ts, s.sid, 1
	, ARRAY[round((random() * 1000)::numeric, 3), CASE WHEN random() < 0.2 THEN NULL ELSE round((random() * 250)::numeric, 1) END]
	, ARRAY[CASE WHEN g.r1 < 0.25 THEN NULL ELSE g.c1 END, CASE WHEN g.r2 < 0.25 THEN NULL ELSE g.n * 7 END]
	, jsonb_build_array((ARRAY['on','off','idle'])[1 + floor(random() * 3)::int])
FROM t_stream s
, LATERAL (
	SELECT n, '2030-03-04 00:00:00+00'::timestamptz + (sum(round((50 + random() * 20)::numeric, 3)) OVER (ORDER BY n)) * interval '1 second' AS ts
		, sum(round((random() * 10)::numeric, 3)) OVER (ORDER BY n) AS c1
		, random() AS r1, random() AS r2
	FROM generate_series(1, 2200) n
) g
WHERE s.k = 17;

INSERT INTO solarhist.dh_datum (ts, stream_id, type_id, data_i, data_a, data_s)
SELECT g.ts, s.sid, 1
	, ARRAY[round((random() * 1000)::numeric, 3), round((random() * 250)::numeric, 1)]
	, ARRAY[g.c1, g.n * 7]
	, '["on"]'::jsonb
FROM t_stream s
, LATERAL (
	SELECT n, '2030-03-04 00:00:00+00'::timestamptz + (sum(round((CASE s.k WHEN 18 THEN 900 + random() * 600 ELSE 280 + random() * 40 END)::numeric, 3)) OVER (ORDER BY n)) * interval '1 second' AS ts
		, sum(round((random() * 10)::numeric, 3)) OVER (ORDER BY n) AS c1
	FROM generate_series(1, CASE s.k WHEN 18 THEN 220 ELSE 860 END) n
) g
WHERE s.k IN (18, 19);

-- a second accumulating property that the datum carry only in part of each hour, or never
INSERT INTO solarhist.dh_datum (ts, stream_id, type_id, data_i, data_a, data_s)
SELECT g.ts, s.sid, 1
	, ARRAY[round((random() * 1000)::numeric, 3), round((random() * 250)::numeric, 1)]
	, CASE s.k
		WHEN 20 THEN CASE WHEN g.mi < 20 THEN ARRAY[g.c1, g.n * 7] ELSE ARRAY[g.c1] END
		WHEN 21 THEN CASE WHEN g.mi >= 40 THEN ARRAY[g.c1, g.n * 7] ELSE ARRAY[g.c1] END
		WHEN 22 THEN CASE WHEN g.mi >= 20 AND g.mi < 40 THEN ARRAY[g.c1, g.n * 7] ELSE ARRAY[g.c1] END
		WHEN 23 THEN ARRAY[g.c1]
		ELSE CASE WHEN g.mi < 20 THEN ARRAY[g.c1, 0] ELSE ARRAY[g.c1] END
		END
	, '["on"]'::jsonb
FROM t_stream s
, LATERAL (
	SELECT x.n, x.ts, x.c1, extract(minute FROM x.ts AT TIME ZONE 'UTC')::int AS mi
	FROM (
		SELECT n
			, '2030-03-04 00:00:00+00'::timestamptz + (sum(round((50 + random() * 20)::numeric, 3)) OVER (ORDER BY n)) * interval '1 second' AS ts
			, sum(round((random() * 10)::numeric, 3)) OVER (ORDER BY n) AS c1
		FROM generate_series(1, 1500) n
	) x
) g
WHERE s.k BETWEEN 20 AND 24;

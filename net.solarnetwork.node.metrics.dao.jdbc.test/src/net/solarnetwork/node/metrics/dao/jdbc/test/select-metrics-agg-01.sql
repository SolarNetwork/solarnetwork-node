WITH m AS (
	SELECT mname
		, min(val) AS m0
		, max(val) AS m1
		, avg(val) AS m2
		, count(val) AS m3
		, sum(val) AS m4
		, percentile_cont(?) WITHIN GROUP (ORDER BY val) AS m5
		, percentile_cont(?) WITHIN GROUP (ORDER BY val) AS m6
	FROM solarnode.mtr_metric
	WHERE ts >= ?
		AND ts < ?
		AND mtype = ANY(?)
	GROUP BY mname
)
, d AS (
	SELECT 'min' AS mtype
		, mname
		, m0 AS val
	FROM m
	UNION ALL
	SELECT 'max' AS mtype
		, mname
		, m1 AS val
	FROM m
	UNION ALL
	SELECT 'avg' AS mtype
		, mname
		, m2 AS val
	FROM m
	UNION ALL
	SELECT 'cnt' AS mtype
		, mname
		, m3 AS val
	FROM m
	UNION ALL
	SELECT 'sum' AS mtype
		, mname
		, m4 AS val
	FROM m
	UNION ALL
	SELECT 'q:25' AS mtype
		, mname
		, m5 AS val
	FROM m
	UNION ALL
	SELECT 'q:75' AS mtype
		, mname
		, m6 AS val
	FROM m
)
SELECT CURRENT_TIMESTAMP AS ts
	, mtype
	, mname
	, val
FROM d
ORDER BY mname, mtype
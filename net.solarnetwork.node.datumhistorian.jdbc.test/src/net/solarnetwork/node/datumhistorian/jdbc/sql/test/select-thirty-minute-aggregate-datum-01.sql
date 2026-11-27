SELECT m.stream_id, m.node_id, m.ts_start
	, ci.data_i, ca.data_a
	, CASE WHEN a.data_s IS NOT NULL OR b.data_s IS NOT NULL THEN (
		SELECT jsonb_agg(LEAST(NULLIF(x.sa, 'null'::JSONB), NULLIF(x.sb, 'null'::JSONB)) ORDER BY x.idx)
		FROM ROWS FROM (jsonb_array_elements(a.data_s), jsonb_array_elements(b.data_s))
			WITH ORDINALITY AS x(sa, sb, idx)
		) END AS data_s
	, CASE WHEN a.data_t IS NOT NULL OR b.data_t IS NOT NULL THEN (
		SELECT array_agg(DISTINCT t.val) FROM unnest(a.data_t || b.data_t) AS t(val)
		) END AS data_t
	, ci.stat_i, ca.read_a
FROM (
	SELECT m.stream_id, m.node_id, m.loc_id, m.source_id, m.ts_start
	FROM (
		SELECT d.stream_id, m.node_id, m.loc_id, m.source_id, date_bin(INTERVAL 'PT30M', d.ts_start, TIMESTAMPTZ '2000-01-03 00:00:00+00') AS ts_start
			, lag(d.ts_start) OVER (PARTITION BY d.stream_id ORDER BY d.ts_start) AS ts_prev
		FROM solarhist.dh_datum_meta m
		INNER JOIN solarhist.dh_agg_15m d ON d.stream_id = m.stream_id
		WHERE m.node_id = ?
	) m
	WHERE m.ts_prev IS NULL OR m.ts_prev < m.ts_start
	ORDER BY m.node_id, m.loc_id, m.source_id, m.ts_start
	OFFSET 0
) m
LEFT OUTER JOIN LATERAL (
	SELECT d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a
	FROM solarhist.dh_agg_15m d
	WHERE d.stream_id = m.stream_id AND d.ts_start = m.ts_start
	LIMIT 1
) a ON TRUE
LEFT OUTER JOIN LATERAL (
	SELECT d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a
	FROM solarhist.dh_agg_15m d
	WHERE d.stream_id = m.stream_id AND d.ts_start = m.ts_start + INTERVAL 'PT15M'
	LIMIT 1
) b ON TRUE
CROSS JOIN LATERAL (
	SELECT vec_trim_scale(array_agg(COALESCE(x.ai * a.stat_i[x.idx][1] + x.bi * b.stat_i[x.idx][1], x.ai * a.stat_i[x.idx][1], x.bi * b.stat_i[x.idx][1])
			/ COALESCE(a.stat_i[x.idx][1] + b.stat_i[x.idx][1], a.stat_i[x.idx][1], b.stat_i[x.idx][1]) ORDER BY x.idx)) AS data_i
		, array_agg(ARRAY[COALESCE(a.stat_i[x.idx][1] + b.stat_i[x.idx][1], a.stat_i[x.idx][1], b.stat_i[x.idx][1])
			, LEAST(b.stat_i[x.idx][2], a.stat_i[x.idx][2]), GREATEST(b.stat_i[x.idx][3], a.stat_i[x.idx][3])] ORDER BY x.idx) AS stat_i
	FROM unnest(a.data_i, b.data_i) WITH ORDINALITY AS x(ai, bi, idx)
	WHERE m.ts_start IS NOT NULL
) ci
CROSS JOIN LATERAL (
	SELECT vec_trim_scale(array_agg(COALESCE(x.aa + x.ba, x.aa, x.ba) ORDER BY x.idx)) AS data_a
		, array_agg(ARRAY[COALESCE(a.read_a[x.idx][1] + b.read_a[x.idx][1], a.read_a[x.idx][1], b.read_a[x.idx][1])
			, CASE WHEN x.aa IS NULL AND x.ba IS NOT NULL THEN COALESCE(b.read_a[x.idx][2], a.read_a[x.idx][2]) ELSE COALESCE(a.read_a[x.idx][2], b.read_a[x.idx][2]) END
			, CASE WHEN x.ba IS NULL AND x.aa IS NOT NULL THEN COALESCE(a.read_a[x.idx][3], b.read_a[x.idx][3]) ELSE COALESCE(b.read_a[x.idx][3], a.read_a[x.idx][3]) END
			] ORDER BY x.idx) AS read_a
	FROM unnest(a.data_a, b.data_a) WITH ORDINALITY AS x(aa, ba, idx)
	WHERE m.ts_start IS NOT NULL
) ca
ORDER BY m.node_id, m.loc_id, m.source_id, m.ts_start

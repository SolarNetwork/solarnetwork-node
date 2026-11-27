SELECT m.stream_id, m.node_id, d.ts_start
	, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a
FROM solarhist.dh_datum_meta m
INNER JOIN LATERAL (
	SELECT d.ts_start, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a
	FROM solarhist.dh_agg_15m d
	WHERE d.stream_id = m.stream_id
		AND d.ts_start >= ?
	ORDER BY d.ts_start DESC
	LIMIT 1
) d ON TRUE
WHERE m.node_id = ?
ORDER BY m.node_id, m.loc_id, m.source_id, d.ts_start

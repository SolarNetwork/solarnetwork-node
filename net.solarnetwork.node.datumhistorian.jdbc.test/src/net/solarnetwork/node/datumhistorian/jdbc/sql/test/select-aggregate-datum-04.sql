SELECT m.stream_id, m.node_id, d.ts_start
	, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a
FROM solarhist.dh_datum_meta m
INNER JOIN solarhist.dh_agg_hourly d ON d.stream_id = m.stream_id
WHERE m.loc_id = ?
	AND d.ts_start >= ?
ORDER BY m.node_id, m.loc_id, m.source_id, d.ts_start

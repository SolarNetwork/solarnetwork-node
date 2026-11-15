SELECT m.stream_id, m.node_id, d.ts_start
	, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a
FROM solarhist.dh_datum_meta m
LEFT OUTER JOIN solarhist.dh_node_time_zone z ON z.node_id = m.node_id
INNER JOIN solarhist.dh_agg_daily d ON d.stream_id = m.stream_id
WHERE m.node_id = ?
	AND d.ts_start >= ? AT TIME ZONE COALESCE(z.time_zone, 'UTC')
	AND d.ts_start < ? AT TIME ZONE COALESCE(z.time_zone, 'UTC')
ORDER BY m.node_id, m.loc_id, m.source_id, d.ts_start

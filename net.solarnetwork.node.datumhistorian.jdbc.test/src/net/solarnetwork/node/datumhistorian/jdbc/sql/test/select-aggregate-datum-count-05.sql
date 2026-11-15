SELECT COUNT(*)
FROM solarhist.dh_datum_meta m
LEFT OUTER JOIN solarhist.dh_node_time_zone z ON z.node_id = m.node_id
INNER JOIN solarhist.dh_agg_daily d ON d.stream_id = m.stream_id
WHERE m.node_id = ?
	AND d.ts_start >= ? AT TIME ZONE COALESCE(z.time_zone, 'UTC')
	AND d.ts_start < ? AT TIME ZONE COALESCE(z.time_zone, 'UTC')

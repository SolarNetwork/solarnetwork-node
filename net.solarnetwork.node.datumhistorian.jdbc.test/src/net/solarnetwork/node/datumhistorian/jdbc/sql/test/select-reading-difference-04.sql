SELECT m.stream_id, m.node_id, d.ts_start
	, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a, d.ts_end
FROM solarhist.dh_datum_meta m
LEFT OUTER JOIN solarhist.dh_node_time_zone z ON z.node_id = m.node_id
INNER JOIN LATERAL (
	SELECT (solarhist.dh_diff_datum(r ORDER BY r.ts)).*
	FROM solarhist.dh_find_datum_diff_rows(m.stream_id
		, ? AT TIME ZONE COALESCE(z.time_zone, 'UTC')
		, ? AT TIME ZONE COALESCE(z.time_zone, 'UTC')
	) r
	HAVING COUNT(*) > 0
) d ON TRUE
WHERE m.node_id = ?
ORDER BY m.node_id, m.loc_id, m.source_id, d.ts_start

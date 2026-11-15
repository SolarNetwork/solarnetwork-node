SELECT COUNT(*)
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

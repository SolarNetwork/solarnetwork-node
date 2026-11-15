SELECT COUNT(*)
FROM solarhist.dh_datum_meta m
LEFT OUTER JOIN solarhist.dh_node_time_zone z ON z.node_id = m.node_id
INNER JOIN solarhist.dh_datum d ON d.stream_id = m.stream_id
INNER JOIN solarhist.dh_datum_type t ON t.type_id = d.type_id
WHERE m.node_id = ?
	AND d.ts >= ? AT TIME ZONE COALESCE(z.time_zone, 'UTC')
	AND d.ts < ? AT TIME ZONE COALESCE(z.time_zone, 'UTC')

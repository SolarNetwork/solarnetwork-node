SELECT m.stream_id, m.source_id, m.loc_id, d.ts, t.class_name, t.type_names
	, m.names_i, m.names_a, m.names_s
	, d.data_i, d.data_a, d.data_s, d.data_t
	, m.node_id
FROM solarhist.dh_datum_meta m
LEFT OUTER JOIN solarhist.dh_node_time_zone z ON z.node_id = m.node_id
INNER JOIN LATERAL (
	SELECT d.ts, d.type_id, d.data_i, d.data_a, d.data_s, d.data_t
	FROM solarhist.dh_datum d
	WHERE d.stream_id = m.stream_id
		AND d.ts < ? AT TIME ZONE COALESCE(z.time_zone, 'UTC')
	ORDER BY d.ts DESC
	LIMIT 1
) d ON TRUE
INNER JOIN solarhist.dh_datum_type t ON t.type_id = d.type_id
WHERE m.node_id = ?
ORDER BY m.node_id, m.loc_id, m.source_id, d.ts

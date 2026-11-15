SELECT m.stream_id, m.source_id, m.loc_id, d.ts, t.class_name, t.type_names
	, m.names_i, m.names_a, m.names_s
	, d.data_i, d.data_a, d.data_s, d.data_t
	, m.node_id
FROM (
	SELECT DISTINCT ON (m.loc_id, m.source_id, d.ts) m.stream_id, d.ts
	FROM solarhist.dh_datum_meta m
	LEFT OUTER JOIN solarhist.dh_node_time_zone z ON z.node_id = m.node_id
	INNER JOIN solarhist.dh_datum d ON d.stream_id = m.stream_id
	WHERE m.loc_id = ?
		AND d.ts >= ? AT TIME ZONE COALESCE(z.time_zone, 'UTC')
	ORDER BY m.loc_id, m.source_id, d.ts, CASE WHEN m.node_id = ? THEN 0 ELSE 1 END, m.node_id
) k
INNER JOIN solarhist.dh_datum_meta m ON m.stream_id = k.stream_id
INNER JOIN solarhist.dh_datum d ON d.stream_id = k.stream_id AND d.ts = k.ts
INNER JOIN solarhist.dh_datum_type t ON t.type_id = d.type_id
ORDER BY m.loc_id, m.source_id, d.ts

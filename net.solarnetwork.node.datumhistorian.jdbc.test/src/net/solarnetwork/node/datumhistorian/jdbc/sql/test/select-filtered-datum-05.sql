SELECT m.stream_id, m.source_id, m.loc_id, d.ts, t.class_name, t.type_names
	, m.names_i, m.names_a, m.names_s
	, d.data_i, d.data_a, d.data_s, d.data_t
	, m.node_id
FROM solarhist.dh_datum_meta m
INNER JOIN solarhist.dh_datum d ON d.stream_id = m.stream_id
INNER JOIN solarhist.dh_datum_type t ON t.type_id = d.type_id
WHERE m.loc_id IS NULL
ORDER BY m.node_id, m.loc_id, m.source_id, d.ts

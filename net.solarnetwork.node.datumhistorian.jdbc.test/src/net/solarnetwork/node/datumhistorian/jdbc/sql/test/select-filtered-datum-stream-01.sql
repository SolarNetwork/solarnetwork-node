SELECT m.stream_id, m.node_id, d.ts
	, d.data_i, d.data_a, d.data_s, d.data_t
FROM solarhist.dh_datum_meta m
INNER JOIN solarhist.dh_datum d ON d.stream_id = m.stream_id
WHERE m.node_id = ?
ORDER BY m.node_id, m.loc_id, m.source_id, d.ts

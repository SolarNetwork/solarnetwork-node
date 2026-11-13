SELECT m.stream_id, m.node_id, m.source_id, m.loc_id, m.names_i, m.names_a, m.names_s
FROM solaruhist.dh_datum_meta m
WHERE m.loc_id = ?
	AND m.stream_id = ?
ORDER BY m.node_id, m.loc_id, m.source_id

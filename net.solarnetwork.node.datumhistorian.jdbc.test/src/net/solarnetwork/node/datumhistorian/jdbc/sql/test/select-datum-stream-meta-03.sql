SELECT m.stream_id, m.node_id, m.source_id, m.loc_id, m.names_i, m.names_a, m.names_s
	, COALESCE(z.time_zone, 'UTC') AS time_zone
FROM solaruhist.dh_datum_meta m
LEFT OUTER JOIN solaruhist.dh_node_time_zone z ON z.node_id = m.node_id
WHERE m.loc_id = ?
	AND m.stream_id = ?
ORDER BY m.node_id, m.loc_id, m.source_id

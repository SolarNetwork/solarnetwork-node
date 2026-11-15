SELECT m.stream_id, m.node_id, m.source_id, m.loc_id, m.names_i, m.names_a, m.names_s
	, COALESCE(z.time_zone, 'UTC') AS time_zone
FROM solarhist.dh_datum_meta m
LEFT OUTER JOIN solarhist.dh_node_time_zone z ON z.node_id = m.node_id
WHERE m.node_id = ANY(?)
	AND m.loc_id IS NULL
	AND m.stream_id = ANY(?)
	AND m.source_id ~* ANY(solarhist.ant_patterns_to_regexps(?))
	AND m.type_ids && ARRAY(SELECT tt.type_id FROM solarhist.dh_datum_type tt WHERE tt.type_names && ?)
ORDER BY m.node_id, m.loc_id, m.source_id

SELECT m.stream_id, m.node_id, d.ts_start
	, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a
FROM solarhist.dh_datum_meta m
INNER JOIN solarhist.dh_agg_15m d ON d.stream_id = m.stream_id
WHERE m.node_id = ANY(?)
	AND m.loc_id IS NULL
	AND m.stream_id = ANY(?)
	AND m.source_id ~* ANY(solarhist.ant_patterns_to_regexps(?))
	AND m.type_ids && ARRAY(SELECT tt.type_id FROM solarhist.dh_datum_type tt WHERE tt.type_names && ?)
	AND d.ts_start >= ?
	AND d.ts_start < ?
ORDER BY d.ts_start DESC, m.source_id, m.node_id, m.loc_id
LIMIT ?
OFFSET ?

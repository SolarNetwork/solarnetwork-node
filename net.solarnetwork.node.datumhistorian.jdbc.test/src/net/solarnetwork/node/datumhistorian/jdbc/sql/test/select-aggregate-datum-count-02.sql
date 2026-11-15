SELECT COUNT(*)
FROM solarhist.dh_datum_meta m
INNER JOIN solarhist.dh_agg_daily d ON d.stream_id = m.stream_id
WHERE m.node_id = ANY(?)
	AND m.loc_id IS NULL
	AND m.stream_id = ANY(?)
	AND m.source_id ~* ANY(solarhist.ant_patterns_to_regexps(?))
	AND m.type_ids && ARRAY(SELECT tt.type_id FROM solarhist.dh_datum_type tt WHERE tt.type_names && ?)
	AND d.ts_start >= ?
	AND d.ts_start < ?

SELECT COUNT(*)
FROM solarhist.dh_datum_meta m
INNER JOIN LATERAL (
	SELECT (solarhist.dh_diff_datum(r ORDER BY r.ts)).*
	FROM solarhist.dh_find_datum_diff_rows(m.stream_id, ?, ?) r
	HAVING COUNT(*) > 0
) d ON TRUE
WHERE m.node_id = ANY(?)
	AND m.loc_id IS NULL
	AND m.stream_id = ANY(?)
	AND m.source_id ~* ANY(solarhist.ant_patterns_to_regexps(?))
	AND m.type_ids && ARRAY(SELECT tt.type_id FROM solarhist.dh_datum_type tt WHERE tt.type_names && ?)

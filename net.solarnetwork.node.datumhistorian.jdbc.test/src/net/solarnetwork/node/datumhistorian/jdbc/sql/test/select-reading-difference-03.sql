SELECT m.stream_id, m.node_id, d.ts_start
	, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a, d.ts_end
FROM solaruhist.dh_datum_meta m
INNER JOIN LATERAL (
	SELECT (solaruhist.dh_diff_datum(r ORDER BY r.ts)).*
	FROM solaruhist.dh_find_datum_diff_rows(m.stream_id, ?, ?) r
	HAVING COUNT(*) > 0
) d ON TRUE
WHERE m.loc_id = ANY(?)
	AND m.source_id ~* ANY(solaruhist.ant_patterns_to_regexps(?))
	AND m.type_ids && ARRAY(SELECT tt.type_id FROM solaruhist.dh_datum_type tt WHERE tt.type_names && ?)
ORDER BY m.node_id DESC, d.ts_start DESC, m.loc_id, m.source_id
LIMIT ?
OFFSET ?

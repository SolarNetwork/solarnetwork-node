SELECT COALESCE(SUM(d.dcount), 0)
FROM solaruhist.dh_datum_meta m
INNER JOIN solaruhist.dh_count_datum_time_span_slots(m.stream_id, ?, ?, ?) d(dcount) ON TRUE
WHERE m.node_id = ANY(?)
	AND m.loc_id IS NULL
	AND m.stream_id = ANY(?)
	AND m.source_id ~* ANY(solaruhist.ant_patterns_to_regexps(?))
	AND m.type_ids && ARRAY(SELECT tt.type_id FROM solaruhist.dh_datum_type tt WHERE tt.type_names && ?)

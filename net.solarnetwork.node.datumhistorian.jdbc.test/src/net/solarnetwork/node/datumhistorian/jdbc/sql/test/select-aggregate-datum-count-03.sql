SELECT COUNT(*)
FROM solaruhist.dh_datum_meta m
INNER JOIN LATERAL (
	SELECT d.ts_start, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a
	FROM solaruhist.dh_agg_monthly d
	WHERE d.stream_id = m.stream_id
		AND d.ts_start >= ?
		AND d.ts_start < ?
	ORDER BY d.ts_start DESC
	LIMIT 1
) d ON TRUE
WHERE m.loc_id = ANY(?)
	AND m.source_id ~* ANY(solaruhist.ant_patterns_to_regexps(?))
	AND m.type_ids && ARRAY(SELECT tt.type_id FROM solaruhist.dh_datum_type tt WHERE tt.type_names && ?)

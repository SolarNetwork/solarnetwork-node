SELECT COUNT(*)
FROM (
	SELECT DISTINCT d.stream_id, date_bin(INTERVAL 'PT30M', d.ts_start, TIMESTAMPTZ '2000-01-03 00:00:00+00')
	FROM solarhist.dh_datum_meta m
	INNER JOIN solarhist.dh_agg_15m d ON d.stream_id = m.stream_id
	WHERE m.node_id = ANY(?)
		AND m.loc_id IS NULL
		AND m.stream_id = ANY(?)
		AND m.source_id ~* ANY(solarhist.ant_patterns_to_regexps(?))
		AND m.type_ids && ARRAY(SELECT tt.type_id FROM solarhist.dh_datum_type tt WHERE tt.type_names && ?)
		AND d.ts_start >= date_bin(INTERVAL 'PT30M', (?::TIMESTAMPTZ) - INTERVAL 'PT0.000001S', TIMESTAMPTZ '2000-01-03 00:00:00+00') + INTERVAL 'PT30M'
		AND d.ts_start < date_bin(INTERVAL 'PT30M', (?::TIMESTAMPTZ) - INTERVAL 'PT0.000001S', TIMESTAMPTZ '2000-01-03 00:00:00+00') + INTERVAL 'PT30M'
) k

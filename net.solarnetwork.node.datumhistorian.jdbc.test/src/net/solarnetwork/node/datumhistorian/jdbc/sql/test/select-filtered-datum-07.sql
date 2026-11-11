SELECT m.stream_id, m.source_id, m.loc_id, d.ts, t.class_name, t.type_names
	, m.names_i, m.names_a, m.names_s
	, d.data_i::TEXT[] AS data_i, d.data_a::TEXT[] AS data_a, d.data_s, d.data_t
	, m.node_id
FROM (
	SELECT DISTINCT ON (m.loc_id, m.source_id) m.stream_id, d.ts
	FROM solarhist.dh_datum_meta m
	INNER JOIN LATERAL (
		SELECT d.ts
		FROM solarhist.dh_datum d
		WHERE d.stream_id = m.stream_id
			AND d.ts >= ?
			AND d.ts < ?
			AND d.type_id = ANY(ARRAY(SELECT tt.type_id FROM solarhist.dh_datum_type tt WHERE tt.type_names && ?))
		ORDER BY d.ts DESC
		LIMIT 1
	) d ON TRUE
	WHERE m.loc_id = ANY(?)
		AND m.source_id ~* ANY(solarhist.ant_patterns_to_regexps(?))
		AND m.type_ids && ARRAY(SELECT tt.type_id FROM solarhist.dh_datum_type tt WHERE tt.type_names && ?)
	ORDER BY m.loc_id, m.source_id, d.ts DESC, CASE WHEN m.node_id = ? THEN 0 ELSE 1 END, m.node_id
) k
INNER JOIN solarhist.dh_datum_meta m ON m.stream_id = k.stream_id
INNER JOIN solarhist.dh_datum d ON d.stream_id = k.stream_id AND d.ts = k.ts
INNER JOIN solarhist.dh_datum_type t ON t.type_id = d.type_id
ORDER BY d.ts DESC, m.loc_id, m.source_id
LIMIT ?
OFFSET ?

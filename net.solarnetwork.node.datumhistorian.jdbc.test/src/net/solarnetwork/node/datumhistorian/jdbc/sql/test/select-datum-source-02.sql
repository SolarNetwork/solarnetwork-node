SELECT d.stream_id, d.source_id, d.loc_id, d.ts, t.class_name, t.type_names
	, d.names_i, d.names_a, d.names_s
	, d.data_i::TEXT[] AS data_i, d.data_a::TEXT[] AS data_a, d.data_s, d.data_t
FROM (
	SELECT m.stream_id, m.source_id, m.loc_id, m.names_i, m.names_a, m.names_s
		, d.ts, d.type_id, d.data_i, d.data_a, d.data_s, d.data_t
		, row_number() OVER (PARTITION BY m.source_id ORDER BY d.ts DESC, m.stream_id DESC) AS rn
	FROM solarhist.dh_datum_meta m
	CROSS JOIN LATERAL (
		SELECT d.ts, d.type_id, d.data_i, d.data_a, d.data_s, d.data_t
		FROM solarhist.dh_datum d
		WHERE d.stream_id = m.stream_id
			AND d.ts <= ?
		ORDER BY d.ts DESC
		LIMIT ?
	) d
	WHERE m.node_id = ?
		AND m.source_id = ?
		AND m.source_id IN (
			SELECT tm.source_id
			FROM solarhist.dh_datum_meta tm
			WHERE tm.node_id = ?
				AND tm.type_ids && ARRAY(
					SELECT tt.type_id
					FROM solarhist.dh_datum_type tt
					WHERE tt.type_names @> ARRAY[?]::TEXT[]
				)
		)
) d
INNER JOIN solarhist.dh_datum_type t ON t.type_id = d.type_id
WHERE d.rn BETWEEN ? AND ?
ORDER BY d.source_id, d.rn

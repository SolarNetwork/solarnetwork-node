SELECT m.stream_id, m.node_id, d.ts_start
	, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a, d.ts_end
FROM solarhist.dh_datum_meta m
INNER JOIN LATERAL (
	SELECT (solarhist.dh_diff_datum(r ORDER BY r.ts)).*
	FROM solarhist.dh_find_datum_diff_rows(m.stream_id, ?, ?) r
	HAVING COUNT(*) > 0
) d ON TRUE
WHERE m.node_id = ?
ORDER BY m.node_id, m.loc_id, m.source_id, d.ts_start

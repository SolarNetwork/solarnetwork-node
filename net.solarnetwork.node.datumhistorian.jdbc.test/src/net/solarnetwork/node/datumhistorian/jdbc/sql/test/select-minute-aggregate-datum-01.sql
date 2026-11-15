SELECT m.stream_id, m.node_id, d.ts_start
	, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a
FROM solarhist.dh_datum_meta m
INNER JOIN solarhist.dh_rollup_datum_for_time_span_slots(m.stream_id, ?, ?, ?) d ON TRUE
WHERE m.node_id = ?
ORDER BY m.node_id, m.loc_id, m.source_id, d.ts_start

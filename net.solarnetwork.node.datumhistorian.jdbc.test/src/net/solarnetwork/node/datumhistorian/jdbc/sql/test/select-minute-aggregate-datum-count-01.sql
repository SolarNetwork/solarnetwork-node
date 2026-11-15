SELECT COALESCE(SUM(d.dcount), 0)
FROM solarhist.dh_datum_meta m
INNER JOIN solarhist.dh_count_datum_time_span_slots(m.stream_id, ?, ?, ?) d(dcount) ON TRUE
WHERE m.node_id = ?

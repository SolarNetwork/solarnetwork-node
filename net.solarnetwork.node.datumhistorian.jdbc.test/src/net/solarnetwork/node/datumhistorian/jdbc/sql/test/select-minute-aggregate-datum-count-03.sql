SELECT COALESCE(SUM(d.dcount), 0)
FROM solarhist.dh_datum_meta m
LEFT OUTER JOIN solarhist.dh_node_time_zone z ON z.node_id = m.node_id
INNER JOIN solarhist.dh_count_datum_time_span_slots(m.stream_id, ? AT TIME ZONE COALESCE(z.time_zone, 'UTC'), ? AT TIME ZONE COALESCE(z.time_zone, 'UTC'), ?) d(dcount) ON TRUE
WHERE m.loc_id = ANY(?)

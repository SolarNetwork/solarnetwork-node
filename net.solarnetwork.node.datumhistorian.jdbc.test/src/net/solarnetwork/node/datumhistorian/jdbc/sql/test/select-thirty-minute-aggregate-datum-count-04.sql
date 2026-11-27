SELECT COUNT(*)
FROM (
	SELECT DISTINCT d.stream_id, date_bin(INTERVAL 'PT30M', d.ts_start, TIMESTAMPTZ '2000-01-03 00:00:00+00')
	FROM solarhist.dh_datum_meta m
	LEFT OUTER JOIN solarhist.dh_node_time_zone z ON z.node_id = m.node_id
	INNER JOIN solarhist.dh_agg_15m d ON d.stream_id = m.stream_id
	WHERE m.node_id = ?
		AND d.ts_start >= date_bin(INTERVAL 'PT30M', (? AT TIME ZONE COALESCE(z.time_zone, 'UTC')) - INTERVAL 'PT0.000001S', TIMESTAMPTZ '2000-01-03 00:00:00+00') + INTERVAL 'PT30M'
		AND d.ts_start < date_bin(INTERVAL 'PT30M', (? AT TIME ZONE COALESCE(z.time_zone, 'UTC')) - INTERVAL 'PT0.000001S', TIMESTAMPTZ '2000-01-03 00:00:00+00') + INTERVAL 'PT30M'
) k

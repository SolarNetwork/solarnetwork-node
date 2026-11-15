INSERT INTO solarhist.dh_node_time_zone AS z (node_id, time_zone)
VALUES (?, ?)
ON CONFLICT (node_id) DO UPDATE
	SET time_zone = EXCLUDED.time_zone, updated = CURRENT_TIMESTAMP
	WHERE z.time_zone IS DISTINCT FROM EXCLUDED.time_zone

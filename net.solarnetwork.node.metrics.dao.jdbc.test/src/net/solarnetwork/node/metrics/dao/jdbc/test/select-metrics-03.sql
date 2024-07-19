SELECT ts, mtype, mname, val
FROM solarnode.mtr_metric
WHERE ts >= ?
	AND ts < ?
ORDER BY ts DESC, mname, val DESC
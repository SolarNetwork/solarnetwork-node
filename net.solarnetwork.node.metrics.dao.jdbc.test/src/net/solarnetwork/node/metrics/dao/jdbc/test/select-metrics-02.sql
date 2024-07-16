SELECT ts, mtype, mname, val
FROM solarnode.mtr_metric
WHERE ts >= ?
	AND ts < ?
ORDER BY ts, mtype, mname
OFFSET ? ROWS
FETCH FIRST ? ROWS ONLY
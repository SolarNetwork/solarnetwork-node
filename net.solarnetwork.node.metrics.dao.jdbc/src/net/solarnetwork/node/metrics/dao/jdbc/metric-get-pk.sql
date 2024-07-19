SELECT ts, mtype, mname, val
FROM solarnode.mtr_metric
WHERE ts = ?
	AND mtype = ?
	AND mname = ?
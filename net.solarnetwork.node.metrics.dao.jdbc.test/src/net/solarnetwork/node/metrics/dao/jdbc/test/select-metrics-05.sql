SELECT ts, mtype, mname, val FROM (
	SELECT DISTINCT ON (mtype, mname) ts, mtype, mname, val
	FROM solarnode.mtr_metric
	WHERE ts >= ?
		AND ts < ?
	ORDER BY ts DESC, mtype, mname
) m
ORDER BY mtype, mname
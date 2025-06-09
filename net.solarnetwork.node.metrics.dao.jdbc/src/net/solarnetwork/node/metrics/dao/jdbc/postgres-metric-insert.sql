INSERT INTO solarnode.mtr_metric (ts, mtype, mname, val) 
VALUES (?, ?, ?, ?)
ON CONFLICT (ts, mtype, mname) DO NOTHING
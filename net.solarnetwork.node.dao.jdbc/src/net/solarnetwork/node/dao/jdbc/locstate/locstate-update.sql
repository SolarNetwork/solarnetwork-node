MERGE INTO solarnode.sn_locstate r
USING (VALUES (?, ?, ?, ?, ?)) s(skey, created, modified, stype, sdata)
	ON r.skey = s.skey
WHEN MATCHED THEN UPDATE
	SET modified = s.modified, stype = s.stype, sdata = s.sdata
WHEN NOT MATCHED THEN INSERT
	VALUES (s.skey, s.created, s.modified, s.stype, s.sdata)

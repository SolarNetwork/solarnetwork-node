SELECT skey, created, modified, stype, sdata FROM FINAL TABLE (
	MERGE INTO solarnode.sn_locstate r
	USING (VALUES (?, ?, ?, ?, ?, CAST(? AS BYTEA))) s(skey, created, modified, stype, sdata, sdata_expected)
		ON r.skey = s.skey
	WHEN MATCHED AND r.sdata <> s.sdata_expected THEN UPDATE
		SET r.stype = r.stype
	WHEN MATCHED THEN UPDATE
		SET modified = s.modified, stype = s.stype, sdata = s.sdata
	WHEN NOT MATCHED THEN INSERT
		VALUES (s.skey, s.created, s.modified, s.stype, s.sdata)
)
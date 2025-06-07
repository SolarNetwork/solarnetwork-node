MERGE INTO solarnode.sn_locstate r
USING (VALUES (
			  CAST(? AS VARCHAR)
			, CAST(? AS TIMESTAMP WITH TIME ZONE)
			, CAST(? AS TIMESTAMP WITH TIME ZONE)
			, CAST(? AS CHARACTER)
			, CAST(? AS BYTEA)))
	AS s(skey, created, modified, stype, sdata)
	ON r.skey = s.skey
WHEN MATCHED AND r.sdata = s.sdata THEN UPDATE
	SET stype = r.stype
WHEN MATCHED THEN UPDATE
	SET modified = s.modified, stype = s.stype, sdata = s.sdata
WHEN NOT MATCHED THEN INSERT
	VALUES (s.skey, s.created, s.modified, s.stype, s.sdata)
RETURNING r.skey, r.created, r.modified, r.stype, r.sdata
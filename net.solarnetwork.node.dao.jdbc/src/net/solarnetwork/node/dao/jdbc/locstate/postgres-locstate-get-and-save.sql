WITH ins AS (
	SELECT * FROM (VALUES (
			  CAST(? AS VARCHAR)
			, CAST(? AS TIMESTAMP WITH TIME ZONE)
			, CAST(? AS TIMESTAMP WITH TIME ZONE)
			, CAST(? AS CHARACTER)
			, CAST(? AS BYTEA))) 
	AS s(skey, created, modified, stype, sdata)
), prev AS (
	SELECT s.skey, s.created, s.modified, s.stype, s.sdata
	FROM solarnode.sn_locstate s
	INNER JOIN ins ON ins.skey = s.skey
	FOR UPDATE OF s
), u AS (
	MERGE INTO solarnode.sn_locstate r
	USING ins s ON r.skey = s.skey
	WHEN MATCHED AND r.sdata = s.sdata THEN DO NOTHING
	WHEN MATCHED THEN UPDATE
		SET modified = s.modified, stype = s.stype, sdata = s.sdata
	WHEN NOT MATCHED THEN INSERT
		VALUES (s.skey, s.created, s.modified, s.stype, s.sdata)
)
SELECT * FROM prev

MERGE INTO solarnode.sn_general_node_datum d
USING (VALUES (CAST(? AS TIMESTAMP), CAST(? AS VARCHAR(64)), CAST(? AS BIGINT), CAST(? AS VARCHAR(8192)))) 
	s(created, source_id, obj_id, jdata)
	ON d.created = s.created
	AND d.source_id = s.source_id
WHEN MATCHED AND d.jdata <> s.jdata AND d.uploaded IS NOT NULL THEN UPDATE
	SET jdata = s.jdata
		, uploaded = NULL
WHEN MATCHED AND d.jdata <> s.jdata THEN UPDATE
	SET jdata = s.jdata
WHEN NOT MATCHED THEN INSERT
	VALUES (s.created, s.source_id, s.obj_id, NULL, s.jdata)

MERGE INTO solarnode.modbus_server_register r
USING (VALUES (
		  CAST(? AS VARCHAR(256))
		, CAST(? AS SMALLINT)
		, CAST(? AS SMALLINT)
		, CAST(? AS INTEGER)
		, CAST(? AS TIMESTAMP)
		, CAST(? AS TIMESTAMP)
		, CAST(? AS SMALLINT)))
	s(server_id, unit_id, block_type, addr, created, modified, val)
	ON r.server_id = s.server_id
	AND r.unit_id = s.unit_id
	AND r.block_type = s.block_type
	AND r.addr = s.addr
WHEN MATCHED THEN UPDATE
	SET modified = s.modified
		, val = s.val
WHEN NOT MATCHED THEN INSERT
	VALUES (s.server_id, s.unit_id, s.block_type, s.addr, s.created, s.modified, s.val)

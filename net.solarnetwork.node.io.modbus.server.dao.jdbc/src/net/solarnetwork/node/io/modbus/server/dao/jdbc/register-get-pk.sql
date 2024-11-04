SELECT server_id, unit_id, block_type, addr, created, modified, val
FROM solarnode.modbus_server_register
WHERE server_id = ?
	AND unit_id = ?
	AND block_type = ?
	AND addr = ?
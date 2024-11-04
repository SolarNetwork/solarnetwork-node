SELECT server_id, unit_id, block_type, addr, created, modified, val
FROM solarnode.modbus_server_register
ORDER BY server_id, unit_id, block_type, addr
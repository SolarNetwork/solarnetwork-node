SELECT 
	created,
	source_id,
	jdata
FROM solarnode.sn_general_node_datum
WHERE created = ? AND source_id = ?

SELECT 
	created,
	source_id,
	jdata
FROM solarnode.sn_general_node_datum
WHERE uploaded IS NULL
ORDER BY created, source_id

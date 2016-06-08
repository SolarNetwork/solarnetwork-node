SELECT 
	created,
	loc_id,
	source_id,
	jdata
FROM solarnode.sn_general_loc_datum
WHERE created = ? AND source_id = ?

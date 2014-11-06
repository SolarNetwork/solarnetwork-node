SELECT 
	created,
	loc_id,
	source_id,
	jdata
FROM solarnode.sn_general_loc_datum
WHERE uploaded IS NULL
ORDER BY created, loc_id, source_id

SELECT 
	created, 
	source_id,
	price_loc_id,
	watts, 
	watt_hour
FROM solarnode.sn_consum_datum 
WHERE uploaded IS NULL
ORDER BY created, source_id

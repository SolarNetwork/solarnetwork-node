SELECT 
	created,
	source_id,
	location_id,
	price
FROM solarnode.sn_price_datum
WHERE uploaded IS NULL
ORDER BY created, source_id

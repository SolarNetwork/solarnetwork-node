SELECT 
	created,
	location_id,
	tz,
	latitude,
	longitude,
	sunrise,
	sunset
FROM solarnode.sn_day_datum d 
WHERE uploaded IS NULL
ORDER BY created, location_id

SELECT d.id, 
	d.created,
	d.source_id,
	d.tz,
	d.latitude,
	d.longitude,
	d.sunrise,
	d.sunset,
	d.error_msg
FROM solarnode.sn_day_datum d 
LEFT OUTER JOIN solarnode.sn_day_datum_upload u
	ON u.datum_id = d.id AND u.destination = ?
WHERE u.datum_id IS NULL
ORDER BY d.id

SELECT p.id, 
	p.source_id,
	p.location_id,
	p.created,
	p.price
FROM solarnode.sn_price_datum p 
LEFT OUTER JOIN solarnode.sn_price_datum_upload u
	ON u.datum_id = p.id AND u.destination = ?
WHERE u.datum_id IS NULL
ORDER BY p.id

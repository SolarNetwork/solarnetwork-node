SELECT c.id, 
	c.source_id,
	c.price_loc_id,
	c.created,
	c.voltage, 
	c.amps, 
	c.watt_hour
FROM solarnode.sn_consum_datum c 
LEFT OUTER JOIN solarnode.sn_consum_datum_upload u
	ON u.consum_datum_id = c.id AND u.destination = ?
WHERE u.consum_datum_id IS NULL
ORDER BY c.id

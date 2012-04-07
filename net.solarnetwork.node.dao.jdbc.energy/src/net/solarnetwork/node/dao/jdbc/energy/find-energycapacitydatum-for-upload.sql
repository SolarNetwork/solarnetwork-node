SELECT 
	d.id,
	d.created,
	d.source_id,
	d.voltage, 
	d.amp_hour, 
	d.watt_hour
FROM solarnode.sn_energy_capacity_datum d 
LEFT OUTER JOIN solarnode.sn_energy_capacity_datum_upload u
	ON u.datum_id = d.id AND u.destination = ?
WHERE u.datum_id IS NULL
ORDER BY d.id

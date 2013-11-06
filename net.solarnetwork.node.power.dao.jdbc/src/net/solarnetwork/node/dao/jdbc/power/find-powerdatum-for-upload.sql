SELECT 
	p.id,
	p.source_id,
	p.price_loc_id,
	p.created,
	p.watts, 
	p.bat_volts, 
	p.bat_amp_hrs, 
	p.dc_out_volts, 
	p.dc_out_amps, 
	p.ac_out_volts, 
	p.ac_out_amps,
	p.watt_hours,
	p.amp_hours
FROM solarnode.sn_power_datum p 
LEFT OUTER JOIN solarnode.sn_power_datum_upload u
	ON u.power_datum_id = p.id AND u.destination = ?
WHERE u.power_datum_id IS NULL
ORDER BY p.id

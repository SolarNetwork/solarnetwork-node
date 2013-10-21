ALTER TABLE solarnode.sn_power_datum
ADD COLUMN watts INTEGER;

UPDATE solarnode.sn_power_datum SET watts = CASE 
	WHEN pv_volts IS NOT NULL AND pv_amps IS NOT NULL THEN CEIL(pv_volts * pv_amps) 
	ELSE NULL END;

ALTER TABLE solarnode.sn_power_datum DROP COLUMN pv_amps;
ALTER TABLE solarnode.sn_power_datum DROP COLUMN pv_volts;

UPDATE solarnode.sn_settings SET svalue = '8' WHERE skey = 'solarnode.sn_power_datum.version'; 

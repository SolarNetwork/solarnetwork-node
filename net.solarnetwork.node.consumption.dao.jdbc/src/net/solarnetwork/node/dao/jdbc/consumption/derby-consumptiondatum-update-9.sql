ALTER TABLE solarnode.sn_consum_datum
ADD COLUMN watts INTEGER;

UPDATE solarnode.sn_consum_datum SET watts = CASE 
	WHEN voltage IS NOT NULL AND amps IS NOT NULL THEN CEIL(voltage * amps) 
	ELSE NULL END;

ALTER TABLE solarnode.sn_consum_datum DROP COLUMN amps;
ALTER TABLE solarnode.sn_consum_datum DROP COLUMN voltage;

UPDATE solarnode.sn_settings SET svalue = '9' WHERE skey = 'solarnode.sn_consum_datum.version'; 

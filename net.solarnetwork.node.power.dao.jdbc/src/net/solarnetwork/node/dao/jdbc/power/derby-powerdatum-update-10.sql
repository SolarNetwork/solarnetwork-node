ALTER TABLE solarnode.sn_power_datum ADD COLUMN watt_hours BIGINT;

UPDATE solarnode.sn_power_datum SET 
	watt_hours = CASE WHEN kwatt_hours IS NOT NULL THEN CEIL(kwatt_hours * 1000) ELSE NULL END;

ALTER TABLE solarnode.sn_power_datum DROP COLUMN kwatt_hours;

UPDATE solarnode.sn_settings SET svalue = '10' WHERE skey = 'solarnode.sn_power_datum.version'; 

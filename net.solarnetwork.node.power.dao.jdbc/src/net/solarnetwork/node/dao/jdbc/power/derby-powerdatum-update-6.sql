ALTER TABLE solarnode.sn_power_datum
ADD COLUMN price_loc_id BIGINT;

UPDATE solarnode.sn_settings SET svalue = '6' WHERE skey = 'solarnode.sn_power_datum.version'; 

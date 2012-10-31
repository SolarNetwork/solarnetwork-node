ALTER TABLE solarnode.sn_power_datum
ADD COLUMN source_id VARCHAR(255);

UPDATE solarnode.sn_settings SET svalue = '7' WHERE skey = 'solarnode.sn_power_datum.version'; 

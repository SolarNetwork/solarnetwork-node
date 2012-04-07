ALTER TABLE solarnode.sn_consum_datum
ADD COLUMN watt_hour BIGINT;

UPDATE solarnode.sn_settings SET svalue = '6' WHERE skey = 'solarnode.sn_consum_datum.version'; 

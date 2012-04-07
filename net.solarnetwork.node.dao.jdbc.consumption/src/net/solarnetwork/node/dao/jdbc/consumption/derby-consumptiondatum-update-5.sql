ALTER TABLE solarnode.sn_consum_datum
ADD COLUMN price_loc_id BIGINT;

UPDATE solarnode.sn_settings SET svalue = '5' WHERE skey = 'solarnode.sn_consum_datum.version'; 

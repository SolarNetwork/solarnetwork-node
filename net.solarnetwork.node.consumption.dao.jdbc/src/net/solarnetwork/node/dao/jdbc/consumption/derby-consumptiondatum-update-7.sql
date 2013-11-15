ALTER TABLE solarnode.sn_consum_datum
DROP COLUMN error_msg;

UPDATE solarnode.sn_settings SET svalue = '7' WHERE skey = 'solarnode.sn_consum_datum.version'; 

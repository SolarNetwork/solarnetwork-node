ALTER TABLE solarnode.sn_day_datum DROP COLUMN error_msg;

UPDATE solarnode.sn_settings SET svalue = '2' WHERE skey = 'solarnode.sn_day_datum.version'; 

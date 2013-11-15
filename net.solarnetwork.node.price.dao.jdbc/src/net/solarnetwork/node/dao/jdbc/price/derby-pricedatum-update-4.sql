ALTER TABLE solarnode.sn_price_datum DROP COLUMN error_msg;

UPDATE solarnode.sn_settings SET svalue = '4' WHERE skey = 'solarnode.sn_price_datum.version'; 

ALTER TABLE solarnode.sn_weather_datum DROP COLUMN error_msg;

UPDATE solarnode.sn_settings SET svalue = '3' WHERE skey = 'solarnode.sn_weather_datum.version'; 

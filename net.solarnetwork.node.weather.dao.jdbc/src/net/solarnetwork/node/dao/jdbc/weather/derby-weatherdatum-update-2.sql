DELETE FROM solarnode.sn_weather_datum
WHERE temperature IS NULL;

ALTER TABLE solarnode.sn_weather_datum
ALTER COLUMN temperature NOT NULL;

UPDATE solarnode.sn_settings SET svalue = '2' WHERE skey = 'solarnode.sn_weather_datum.version'; 

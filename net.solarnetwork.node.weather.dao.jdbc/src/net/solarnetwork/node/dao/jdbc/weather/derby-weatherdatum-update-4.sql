DROP TABLE solarnode.sn_weather_datum_upload;
DELETE FROM solarnode.sn_settings WHERE skey = 'solarnode.sn_weather_datum_upload.version';

DELETE FROM solarnode.sn_weather_datum;

ALTER TABLE solarnode.sn_weather_datum ADD COLUMN location_id BIGINT;
ALTER TABLE solarnode.sn_weather_datum ALTER COLUMN location_id NOT NULL;
ALTER TABLE solarnode.sn_weather_datum ADD COLUMN uploaded TIMESTAMP;
ALTER TABLE solarnode.sn_weather_datum DROP COLUMN id;
ALTER TABLE solarnode.sn_weather_datum DROP COLUMN source_id;
ALTER TABLE solarnode.sn_weather_datum DROP COLUMN info_date;

ALTER TABLE solarnode.sn_weather_datum ADD PRIMARY KEY (created, location_id);
DROP INDEX solarnode.weather_datum_created_idx;

UPDATE solarnode.sn_settings SET svalue = '4' WHERE skey = 'solarnode.sn_weather_datum.version'; 

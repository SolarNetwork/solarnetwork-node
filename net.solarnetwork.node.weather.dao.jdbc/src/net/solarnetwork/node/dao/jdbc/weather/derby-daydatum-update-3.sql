DROP TABLE solarnode.sn_day_datum_upload;
DELETE FROM solarnode.sn_settings WHERE skey = 'solarnode.sn_day_datum_upload.version';

DELETE FROM solarnode.sn_day_datum;

ALTER TABLE solarnode.sn_day_datum ADD COLUMN location_id BIGINT;
ALTER TABLE solarnode.sn_day_datum ALTER COLUMN location_id NOT NULL;
ALTER TABLE solarnode.sn_day_datum ADD COLUMN uploaded TIMESTAMP;
ALTER TABLE solarnode.sn_day_datum DROP COLUMN id;
ALTER TABLE solarnode.sn_day_datum DROP COLUMN source_id;

ALTER TABLE solarnode.sn_day_datum ADD PRIMARY KEY (created, location_id);
DROP INDEX solarnode.day_datum_created_idx;

UPDATE solarnode.sn_settings SET svalue = '3' WHERE skey = 'solarnode.sn_day_datum.version'; 

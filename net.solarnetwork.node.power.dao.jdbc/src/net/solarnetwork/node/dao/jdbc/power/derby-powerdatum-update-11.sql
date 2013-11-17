DROP TABLE solarnode.sn_power_datum_upload;
DELETE FROM solarnode.sn_settings WHERE skey = 'solarnode.sn_power_datum_upload.version';

ALTER TABLE solarnode.sn_power_datum ADD COLUMN uploaded TIMESTAMP;
ALTER TABLE solarnode.sn_power_datum DROP COLUMN id;

UPDATE solarnode.sn_power_datum SET source_id = '' WHERE source_id IS NULL;
ALTER TABLE solarnode.sn_power_datum ALTER COLUMN source_id NOT NULL;

ALTER TABLE solarnode.sn_power_datum ADD PRIMARY KEY (created, source_id);
DROP INDEX solarnode.power_datum_created_idx;
UPDATE solarnode.sn_settings SET svalue = '11' WHERE skey = 'solarnode.sn_power_datum.version'; 

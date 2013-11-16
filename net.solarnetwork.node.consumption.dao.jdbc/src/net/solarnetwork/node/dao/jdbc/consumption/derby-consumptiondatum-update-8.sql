DROP TABLE solarnode.sn_consum_datum_upload;
DELETE FROM solarnode.sn_settings WHERE skey = 'solarnode.sn_consum_datum_upload.version';

ALTER TABLE solarnode.sn_consum_datum ADD COLUMN uploaded TIMESTAMP;
ALTER TABLE solarnode.sn_consum_datum DROP COLUMN id;
ALTER TABLE solarnode.sn_consum_datum ADD PRIMARY KEY (created, source_id);
DROP INDEX solarnode.consum_datum_created_idx;
UPDATE solarnode.sn_settings SET svalue = '8' WHERE skey = 'solarnode.sn_consum_datum.version'; 

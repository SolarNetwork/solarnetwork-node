DROP TABLE solarnode.sn_power_datum_upload;
DELETE FROM solarnode.sn_settings WHERE skey = 'solarnode.sn_power_datum_upload.version';

DELETE FROM solarnode.sn_power_datum WHERE id IN (
	SELECT p.id FROM solarnode.sn_power_datum p 
	INNER JOIN (
		SELECT created, source_id, MIN(id) as id FROM solarnode.sn_power_datum GROUP BY created, source_id HAVING count(created) > 1
	) s ON p.created = s.created and p.source_id = s.source_id and p.id <> s.id
	WHERE p.created = s.created and p.source_id = s.source_id and p.id <> s.id);

ALTER TABLE solarnode.sn_power_datum ADD COLUMN uploaded TIMESTAMP;
ALTER TABLE solarnode.sn_power_datum DROP COLUMN id;

UPDATE solarnode.sn_power_datum SET source_id = '' WHERE source_id IS NULL;
ALTER TABLE solarnode.sn_power_datum ALTER COLUMN source_id NOT NULL;

ALTER TABLE solarnode.sn_power_datum ADD PRIMARY KEY (created, source_id);
DROP INDEX solarnode.power_datum_created_idx;
UPDATE solarnode.sn_settings SET svalue = '11' WHERE skey = 'solarnode.sn_power_datum.version'; 

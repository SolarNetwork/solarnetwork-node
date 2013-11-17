DROP TABLE solarnode.sn_consum_datum_upload;
DELETE FROM solarnode.sn_settings WHERE skey = 'solarnode.sn_consum_datum_upload.version';

DELETE FROM solarnode.sn_consum_datum WHERE id IN (
	SELECT p.id FROM solarnode.sn_consum_datum p 
	INNER JOIN (
		SELECT created, source_id, MIN(id) as id FROM solarnode.sn_consum_datum GROUP BY created, source_id HAVING count(created) > 1
	) s ON p.created = s.created and p.source_id = s.source_id and p.id <> s.id
	WHERE p.created = s.created and p.source_id = s.source_id and p.id <> s.id);

ALTER TABLE solarnode.sn_consum_datum ADD COLUMN uploaded TIMESTAMP;
ALTER TABLE solarnode.sn_consum_datum DROP COLUMN id;
ALTER TABLE solarnode.sn_consum_datum ADD PRIMARY KEY (created, source_id);
DROP INDEX solarnode.consum_datum_created_idx;
UPDATE solarnode.sn_settings SET svalue = '8' WHERE skey = 'solarnode.sn_consum_datum.version'; 

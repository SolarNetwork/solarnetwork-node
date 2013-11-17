DROP TABLE solarnode.sn_price_datum_upload;
DELETE FROM solarnode.sn_settings WHERE skey = 'solarnode.sn_price_datum_upload.version';

DELETE FROM solarnode.sn_price_datum WHERE id IN (
	SELECT p.id FROM solarnode.sn_price_datum p 
	INNER JOIN (
		SELECT created, location_id, MIN(id) as id FROM solarnode.sn_price_datum GROUP BY created, location_id HAVING count(created) > 1
	) s ON p.created = s.created and p.location_id = s.location_id and p.id <> s.id
	WHERE p.created = s.created and p.location_id = s.location_id and p.id <> s.id);

ALTER TABLE solarnode.sn_price_datum ADD COLUMN uploaded TIMESTAMP;
ALTER TABLE solarnode.sn_price_datum DROP COLUMN id;

ALTER TABLE solarnode.sn_price_datum ADD PRIMARY KEY (created, location_id);
DROP INDEX solarnode.price_datum_created_idx;

UPDATE solarnode.sn_settings SET svalue = '5' WHERE skey = 'solarnode.sn_price_datum.version'; 


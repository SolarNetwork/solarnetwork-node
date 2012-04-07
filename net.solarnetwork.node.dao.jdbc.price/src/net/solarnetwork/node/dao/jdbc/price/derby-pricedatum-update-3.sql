ALTER TABLE solarnode.sn_price_datum
ADD COLUMN location_id BIGINT;

DELETE FROM solarnode.sn_price_datum;

ALTER TABLE solarnode.sn_price_datum
ALTER COLUMN location_id NOT NULL;

ALTER TABLE solarnode.sn_price_datum
DROP COLUMN currency;

ALTER TABLE solarnode.sn_price_datum
DROP COLUMN unit;

UPDATE solarnode.sn_settings SET svalue = '3' WHERE skey = 'solarnode.sn_price_datum.version'; 

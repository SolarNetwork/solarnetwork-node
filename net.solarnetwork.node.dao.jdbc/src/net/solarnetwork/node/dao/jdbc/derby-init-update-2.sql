ALTER TABLE solarnode.sn_settings
ADD COLUMN tkey VARCHAR(255) WITH DEFAULT '';

UPDATE solarnode.sn_settings SET tkey = '';

ALTER TABLE solarnode.sn_settings
ALTER COLUMN tkey NOT NULL;

ALTER TABLE solarnode.sn_settings
DROP PRIMARY KEY;

ALTER TABLE solarnode.sn_settings
ADD PRIMARY KEY (skey, tkey);

UPDATE solarnode.sn_settings SET svalue = '2'
WHERE skey = 'solarnode.sn_settings.version';

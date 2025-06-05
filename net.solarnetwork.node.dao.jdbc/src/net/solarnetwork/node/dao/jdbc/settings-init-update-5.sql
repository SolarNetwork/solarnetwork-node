ALTER TABLE solarnode.sn_settings
ADD COLUMN flags INTEGER WITH DEFAULT 0;

UPDATE solarnode.sn_settings SET flags = 0;

UPDATE solarnode.sn_settings SET flags = 1, tkey = ''
WHERE tkey = '_modification_date_ignore';

ALTER TABLE solarnode.sn_settings
ALTER COLUMN flags NOT NULL;

UPDATE solarnode.sn_settings SET svalue = '5'
WHERE skey = 'solarnode.sn_settings.version';

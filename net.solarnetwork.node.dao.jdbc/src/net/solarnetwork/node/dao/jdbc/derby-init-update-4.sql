ALTER TABLE solarnode.sn_settings
ADD COLUMN modified TIMESTAMP WITH DEFAULT CURRENT_TIMESTAMP;

UPDATE solarnode.sn_settings SET modified = CURRENT_TIMESTAMP;

ALTER TABLE solarnode.sn_settings
ALTER COLUMN modified NOT NULL;

UPDATE solarnode.sn_settings SET svalue = '4'
WHERE skey = 'solarnode.sn_settings.version';

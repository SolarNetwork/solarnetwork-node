ALTER TABLE solarnode.sn_settings
ALTER COLUMN skey SET DATA TYPE VARCHAR(255);

UPDATE solarnode.sn_settings SET svalue = '3'
WHERE skey = 'solarnode.sn_settings.version';

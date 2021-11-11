ALTER TABLE solarnode.sn_settings
ALTER COLUMN svalue SET DATA TYPE VARCHAR(4096);

UPDATE solarnode.sn_settings SET svalue = '6'
WHERE skey = 'solarnode.sn_settings.version';

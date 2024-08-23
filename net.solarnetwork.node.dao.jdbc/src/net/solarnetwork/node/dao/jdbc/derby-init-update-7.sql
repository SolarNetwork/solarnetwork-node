ALTER TABLE solarnode.sn_settings
ADD COLUMN note VARCHAR(4096);

UPDATE solarnode.sn_settings SET svalue = '7'
WHERE skey = 'solarnode.sn_settings.version';

ALTER TABLE solarnode.sn_general_loc_datum
ALTER COLUMN jdata SET DATA TYPE VARCHAR(1024);

UPDATE solarnode.sn_settings SET svalue = '2'
WHERE skey = 'solarnode.sn_general_loc_datum.version';

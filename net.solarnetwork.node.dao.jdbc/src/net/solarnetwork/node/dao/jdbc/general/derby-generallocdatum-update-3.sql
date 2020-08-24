ALTER TABLE solarnode.sn_general_loc_datum
ALTER COLUMN source_id SET DATA TYPE VARCHAR(64);

UPDATE solarnode.sn_settings SET svalue = '3'
WHERE skey = 'solarnode.sn_general_loc_datum.version';

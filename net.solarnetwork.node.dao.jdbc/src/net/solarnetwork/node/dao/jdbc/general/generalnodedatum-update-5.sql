ALTER TABLE solarnode.sn_general_node_datum
ALTER COLUMN jdata SET DATA TYPE VARCHAR(8192);

UPDATE solarnode.sn_settings SET svalue = '5'
WHERE skey = 'solarnode.sn_general_node_datum.version';

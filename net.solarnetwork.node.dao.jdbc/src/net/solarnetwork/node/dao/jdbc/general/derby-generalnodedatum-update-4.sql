ALTER TABLE solarnode.sn_general_node_datum
ADD COLUMN obj_id SET DATA TYPE BIGINT;

UPDATE solarnode.sn_settings SET svalue = '4'
WHERE skey = 'solarnode.sn_general_node_datum.version';

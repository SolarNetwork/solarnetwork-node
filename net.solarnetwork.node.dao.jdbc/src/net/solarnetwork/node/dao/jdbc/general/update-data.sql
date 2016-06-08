UPDATE solarnode.sn_general_node_datum
SET uploaded = NULL, jdata = ? 
WHERE created = ? AND source_id = ?

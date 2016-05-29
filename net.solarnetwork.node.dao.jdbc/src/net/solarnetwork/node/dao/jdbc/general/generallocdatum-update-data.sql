UPDATE solarnode.sn_general_loc_datum 
SET uploaded = NULL, jdata = ?
WHERE created = ? AND source_id = ?

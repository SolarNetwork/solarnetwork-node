DELETE FROM solarnode.sn_general_loc_datum
WHERE uploaded IS NOT NULL AND uploaded < ?

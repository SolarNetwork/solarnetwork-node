DELETE FROM solarnode.sn_general_node_datum
WHERE uploaded IS NOT NULL AND uploaded < ?

DELETE FROM solarnode.sn_consum_datum
WHERE uploaded IS NOT NULL AND created < ?

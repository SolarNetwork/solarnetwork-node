DELETE FROM solarnode.sn_day_datum
WHERE uploaded IS NOT NULL AND created < ?

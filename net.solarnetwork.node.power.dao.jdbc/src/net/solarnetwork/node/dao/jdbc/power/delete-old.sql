DELETE FROM solarnode.sn_power_datum
WHERE uploaded IS NOT NULL AND created < ?

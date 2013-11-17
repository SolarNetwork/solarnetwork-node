DELETE FROM solarnode.sn_price_datum
WHERE uploaded IS NOT NULL AND created < ?

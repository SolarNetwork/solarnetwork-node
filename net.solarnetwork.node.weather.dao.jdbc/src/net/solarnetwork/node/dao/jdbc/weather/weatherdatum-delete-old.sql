DELETE FROM solarnode.sn_weather_datum
WHERE uploaded IS NOT NULL AND created < ?

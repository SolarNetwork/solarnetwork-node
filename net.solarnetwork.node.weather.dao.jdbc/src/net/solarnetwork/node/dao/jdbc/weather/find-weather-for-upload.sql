SELECT w.id, 
	w.created,
	w.source_id,
	w.info_date,
	w.sky_cond,
	w.temperature,
	w.humidity,
	w.bar_pressure,
	w.bar_delta,
	w.visibility,
	w.uv_index,
	w.dew_point
FROM solarnode.sn_weather_datum w 
LEFT OUTER JOIN solarnode.sn_weather_datum_upload u
	ON u.datum_id = w.id AND u.destination = ?
WHERE u.datum_id IS NULL
ORDER BY w.id

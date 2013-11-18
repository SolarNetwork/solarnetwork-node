SELECT  
	created,
	location_id,
	sky_cond,
	temperature,
	humidity,
	bar_pressure,
	bar_delta,
	visibility,
	uv_index,
	dew_point
FROM solarnode.sn_weather_datum w 
WHERE uploaded IS NULL
ORDER BY created, location_id

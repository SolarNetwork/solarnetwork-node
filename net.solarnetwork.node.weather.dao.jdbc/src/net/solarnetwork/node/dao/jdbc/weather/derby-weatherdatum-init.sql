CREATE TABLE solarnode.sn_weather_datum (
	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	location_id 	VARCHAR(255),
	info_date		TIMESTAMP NOT NULL,
	sky_cond		VARCHAR(255),
	temperature		DOUBLE NOT NULL,
	humidity		DOUBLE,
	bar_pressure	DOUBLE,
	bar_delta		VARCHAR(255),
	visibility		DOUBLE,
	uv_index		INTEGER,
	dew_point		DOUBLE,
	uploaded		TIMESTAMP,
	PRIMARY KEY (created, location_id)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_weather_datum.version', '4');

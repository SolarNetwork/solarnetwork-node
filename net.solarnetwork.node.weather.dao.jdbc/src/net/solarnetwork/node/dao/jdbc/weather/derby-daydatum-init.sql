CREATE TABLE solarnode.sn_day_datum (
	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	location_id 	BIGINT NOT NULL,
	tz				VARCHAR(255) NOT NULL,
	latitude		DOUBLE,
	longitude		DOUBLE,
	sunrise			TIME,
	sunset			TIME
	uploaded		TIMESTAMP,
	PRIMARY KEY (created, location_id)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_day_datum.version', '3');

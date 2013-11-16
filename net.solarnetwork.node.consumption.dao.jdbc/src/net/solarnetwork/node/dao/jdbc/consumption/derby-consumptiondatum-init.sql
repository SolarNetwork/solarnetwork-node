CREATE TABLE solarnode.sn_consum_datum (
	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	source_id 		VARCHAR(255) NOT NULL,
	price_loc_id	BIGINT,
	amps			DOUBLE,
	voltage			DOUBLE,
	watt_hour		BIGINT,
	uploaded		TIMESTAMP,
	PRIMARY KEY (created, source_id)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_consum_datum.version', '8');

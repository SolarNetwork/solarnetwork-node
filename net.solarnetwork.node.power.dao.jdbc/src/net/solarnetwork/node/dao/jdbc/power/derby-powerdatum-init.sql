CREATE TABLE solarnode.sn_power_datum (
	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	source_id 		VARCHAR(255) NOT NULL,
	price_loc_id	BIGINT,
	watts 			INTEGER,
	watt_hours		BIGINT,
	bat_volts		DOUBLE,
	bat_amp_hrs		DOUBLE,
	dc_out_volts	DOUBLE,
	dc_out_amps		DOUBLE,
	ac_out_volts	DOUBLE,
	ac_out_amps		DOUBLE,
	amp_hours		DOUBLE,
	PRIMARY KEY (created, source_id)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_power_datum.version', '11');

CREATE TABLE solarnode.sn_power_datum (
	id				BIGINT NOT NULL DEFAULT nextval('solarnode.solarnode_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	pv_volts 		REAL,
	pv_amps			REAL,
	bat_volts		REAL,
	bat_amp_hrs		REAL,
	dc_out_volts	REAL,
	dc_out_amps		REAL,
	ac_out_volts	REAL,
	ac_out_amps		REAL,
	kwatt_hours		REAL,
	amp_hours		REAL,
	error_msg		TEXT,
	PRIMARY KEY (id)
);

CREATE INDEX power_datum_created_idx ON solarnode.sn_power_datum (created);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_power_datum.version', '5');

CREATE TABLE solarnode.sn_power_datum_upload (
	power_datum_id	BIGINT NOT NULL,
	destination		VARCHAR(255) NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	track_id		BIGINT NOT NULL,
	PRIMARY KEY (power_datum_id, destination)
);

ALTER TABLE solarnode.sn_power_datum_upload ADD CONSTRAINT
sn_power_datum_upload_power_datum_fk FOREIGN KEY (power_datum_id)
REFERENCES solarnode.sn_power_datum ON DELETE CASCADE;

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_power_datum_upload.version', '1');


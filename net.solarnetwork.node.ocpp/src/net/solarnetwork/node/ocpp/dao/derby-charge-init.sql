CREATE TABLE solarnode.ocpp_charge (
	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	sessid_hi		BIGINT NOT NULL,
	sessid_lo		BIGINT NOT NULL,
	idtag 			VARCHAR(20) NOT NULL,
	socketid		VARCHAR(64) NOT NULL,
	auth_status		VARCHAR(13),
	xid				BIGINT,
	ended			TIMESTAMP,
	posted			TIMESTAMP,
	CONSTRAINT ocpp_charge_pk PRIMARY KEY (sessid_hi, sessid_lo)
);

CREATE TABLE solarnode.ocpp_meter_reading (
	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	sessid_hi		BIGINT NOT NULL,
	sessid_lo		BIGINT NOT NULL,
	measurand		VARCHAR(40) NOT NULL,
	reading 		VARCHAR(64) NOT NULL,
	context 		VARCHAR(20),
	location		VARCHAR(8),
	unit 			VARCHAR(8),
	CONSTRAINT ocpp_meter_reading_charge_fk FOREIGN KEY (sessid_hi, sessid_lo)
		REFERENCES solarnode.ocpp_charge (sessid_hi, sessid_lo)
		ON DELETE CASCADE
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.ocpp_charge.version', '1');

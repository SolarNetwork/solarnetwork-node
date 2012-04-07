CREATE TABLE solarnode.sn_price_datum (
	id				BIGINT NOT NULL DEFAULT nextval('solarnode.solarnode_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	source_id 		VARCHAR(255) NOT NULL,
	price			REAL,
	currency		VARCHAR(10),
	unit			VARCHAR(20),
	error_msg		TEXT,
	PRIMARY KEY (id)
);

CREATE INDEX price_datum_created_idx ON solarnode.sn_price_datum (created);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_price_datum.version', '2');

CREATE TABLE solarnode.sn_price_datum_upload (
	datum_id		BIGINT NOT NULL,
	destination		VARCHAR(255) NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	track_id		BIGINT NOT NULL,
	PRIMARY KEY (datum_id, destination)
);

ALTER TABLE solarnode.sn_price_datum_upload ADD CONSTRAINT
sn_price_datum_upload_price_datum_fk FOREIGN KEY (datum_id)
REFERENCES solarnode.sn_price_datum;

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_price_datum_upload.version', '1');

CREATE TABLE solarnode.sn_price_datum (
	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	source_id 		VARCHAR(255) NOT NULL,
	location_id		BIGINT NOT NULL,
	price			DOUBLE
	PRIMARY KEY (created, source_id)
);

CREATE INDEX price_datum_created_idx ON solarnode.sn_price_datum (created);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_price_datum.version', '5');

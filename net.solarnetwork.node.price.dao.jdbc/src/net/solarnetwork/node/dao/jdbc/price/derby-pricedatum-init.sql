CREATE TABLE solarnode.sn_price_datum (
	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	location_id		BIGINT NOT NULL,
	price			DOUBLE,
	uploaded		TIMESTAMP,
	PRIMARY KEY (created, location_id)
);

CREATE INDEX price_datum_created_idx ON solarnode.sn_price_datum (created);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_price_datum.version', '5');

CREATE TABLE solarnode.sn_sectok (
	id					VARCHAR(20) NOT NULL,
	created				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	tok_sec 			VARCHAR(32) NOT NULL,
	disp_name			VARCHAR(128) NOT NULL,
	description			VARCHAR(256) NOT NULL,
	CONSTRAINT sn_sectok_pk PRIMARY KEY (id)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_sectok.version', '1');

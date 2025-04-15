CREATE SCHEMA IF NOT EXISTS solarnode;

CREATE TABLE IF NOT EXISTS solarnode.sn_locstate (
	skey				VARCHAR(256) NOT NULL,
	created				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified			TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	stype				CHARACTER NOT NULL,
	sdata				BYTEA(8192),
	CONSTRAINT sn_locstate_pk PRIMARY KEY (skey)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_locstate.version', '1');

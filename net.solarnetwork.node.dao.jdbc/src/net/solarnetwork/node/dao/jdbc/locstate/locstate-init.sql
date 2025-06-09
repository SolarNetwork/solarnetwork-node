CREATE SCHEMA IF NOT EXISTS solarnode;

CREATE TABLE IF NOT EXISTS solarnode.sn_locstate (
	skey				VARCHAR(256) NOT NULL,
	created				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified			TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	stype				CHARACTER NOT NULL,
	sdata				BYTEA,
	CONSTRAINT sn_locstate_pk PRIMARY KEY (skey),
	CONSTRAINT sn_locstate_sdata_len CHECK (length(sdata) <= 8192)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_locstate.version', '1');

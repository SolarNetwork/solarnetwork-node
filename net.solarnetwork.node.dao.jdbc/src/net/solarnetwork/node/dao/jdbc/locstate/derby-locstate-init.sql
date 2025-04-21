CREATE TABLE solarnode.sn_locstate (
	skey				VARCHAR(256) NOT NULL,
	created				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified			TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	stype				CHARACTER NOT NULL,
	sdata				VARCHAR (8192) FOR BIT DATA,
	CONSTRAINT sn_locstate_pk PRIMARY KEY (skey)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_locstate.version', '1');

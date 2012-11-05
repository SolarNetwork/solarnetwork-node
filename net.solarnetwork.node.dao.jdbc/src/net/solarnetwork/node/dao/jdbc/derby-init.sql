CREATE SCHEMA solarnode;

CREATE TABLE solarnode.sn_settings (
	skey	  VARCHAR(255) NOT NULL,
	tkey	  VARCHAR(255) NOT NULL WITH DEFAULT '',
	svalue	  VARCHAR(255) NOT NULL,
	modified  TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (skey, tkey)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_settings.version', '4');

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.db.create.time', CAST(CURRENT_TIMESTAMP AS VARCHAR(255)));

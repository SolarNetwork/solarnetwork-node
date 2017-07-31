CREATE TABLE solarnode.sn_settings (
	skey	  VARCHAR(255) NOT NULL,
	tkey	  VARCHAR(255) NOT NULL WITH DEFAULT '',
	svalue	  VARCHAR(255) NOT NULL,
	flags     INTEGER NOT NULL WITH DEFAULT 0,
	modified  TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (skey, tkey)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_settings.version', '5');

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.db.create.time', CAST(CURRENT_TIMESTAMP AS VARCHAR(255)));

CREATE SCHEMA solarnode;

CREATE TABLE solarnode.sn_settings (
	skey	VARCHAR(64) NOT NULL,
	svalue	VARCHAR(255) NOT NULL,
	PRIMARY KEY (skey)
);

CREATE SEQUENCE solarnode.solarnode_seq;

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_settings.version', '1');

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.db.create.time', CAST(CURRENT_TIMESTAMP AS VARCHAR(255)));

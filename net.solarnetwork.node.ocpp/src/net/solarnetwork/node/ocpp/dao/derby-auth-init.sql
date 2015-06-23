CREATE TABLE solarnode.ocpp_auth (
	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	idtag 			VARCHAR(20) NOT NULL,
	parent_idtag	VARCHAR(20),
	auth_status		VARCHAR(13) NOT NULL,
	expires			TIMESTAMP,
	PRIMARY KEY (idtag)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.ocpp_auth.version', '1');

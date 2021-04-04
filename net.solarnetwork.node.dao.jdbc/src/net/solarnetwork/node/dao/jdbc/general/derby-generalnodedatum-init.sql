CREATE TABLE solarnode.sn_general_node_datum (
	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	source_id 		VARCHAR(64) NOT NULL,
	uploaded		TIMESTAMP,
	jdata			VARCHAR(1024) NOT NULL,
	PRIMARY KEY (created, source_id)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_general_node_datum.version', '3');

CREATE TABLE solarnode.sn_general_node_datum (
	created			TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	source_id 		VARCHAR(64) NOT NULL,
	obj_id			BIGINT,
	uploaded		TIMESTAMP,
	jdata			VARCHAR(8192) NOT NULL,
	PRIMARY KEY (created, source_id)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_general_node_datum.version', '5');

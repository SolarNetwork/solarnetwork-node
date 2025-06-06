CREATE SCHEMA IF NOT EXISTS solarnode;

CREATE TABLE IF NOT EXISTS solarnode.modbus_server_meta (
	skey				VARCHAR(256) NOT NULL,
	svalue				VARCHAR(256) NOT NULL,
	created				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT modbus_server_meta_pk PRIMARY KEY (skey)
);

CREATE TABLE IF NOT EXISTS solarnode.modbus_server_register (
	server_id			VARCHAR(256) NOT NULL,
	unit_id				SMALLINT NOT NULL,
	block_type			SMALLINT NOT NULL,
	addr				INTEGER NOT NULL,
	created				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified			TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	val					SMALLINT NOT NULL,
	CONSTRAINT modbus_server_register_pk PRIMARY KEY (server_id, unit_id, block_type, addr)
);

INSERT INTO solarnode.modbus_server_meta (skey, svalue) 
VALUES ('solarnode.modbus_server_register.version', '1');

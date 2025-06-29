CREATE SCHEMA IF NOT EXISTS solarnode;

CREATE TABLE IF NOT EXISTS solarnode.mqtt_message_meta (
	skey				VARCHAR(256) NOT NULL,
	svalue				VARCHAR(256) NOT NULL,
	created				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT mqtt_message_meta_pk PRIMARY KEY (skey)
);

CREATE TABLE IF NOT EXISTS solarnode.mqtt_message (
	id					BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY,
	created				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	destination			VARCHAR(128) NOT NULL,
	topic				VARCHAR(256) NOT NULL,
	retained			BOOLEAN NOT NULL DEFAULT FALSE,
	qos					SMALLINT NOT NULL DEFAULT 0,
	payload				BYTEA,
	CONSTRAINT mqtt_message_pk PRIMARY KEY (id),
	CONSTRAINT mqtt_message_payload_len CHECK (length(payload) <= 8192)
);

INSERT INTO solarnode.mqtt_message_meta (skey, svalue) 
VALUES ('solarnode.mqtt_message.version', '1');

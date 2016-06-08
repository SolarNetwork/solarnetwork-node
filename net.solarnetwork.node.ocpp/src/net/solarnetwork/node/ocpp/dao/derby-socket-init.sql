CREATE TABLE solarnode.ocpp_socket (
	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	socketid 		VARCHAR(64) NOT NULL,
	enabled			BOOLEAN NOT NULL DEFAULT true,
	CONSTRAINT ocpp_socket_pk PRIMARY KEY (socketid)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.ocpp_socket.version', '1');

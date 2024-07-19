CREATE SCHEMA IF NOT EXISTS solarnode;

CREATE TABLE IF NOT EXISTS solarnode.mtr_metric_meta (
	skey				VARCHAR(256) NOT NULL,
	svalue				VARCHAR(256) NOT NULL,
	created				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT mtr_metric_meta_pk PRIMARY KEY (skey)
);

CREATE TABLE IF NOT EXISTS solarnode.mtr_metric (
	ts					TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	mtype				VARCHAR(64) NOT NULL,
	mname				VARCHAR(128) NOT NULL,
	val					DOUBLE PRECISION,
	CONSTRAINT mtr_metric_pk PRIMARY KEY (ts, mtype, mname)
);

INSERT INTO solarnode.mtr_metric_meta (skey, svalue) 
VALUES ('solarnode.metric.version', '1');

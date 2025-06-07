CREATE TABLE IF NOT EXISTS solarnode.test_csv_io (
	pk	  BIGINT NOT NULL,
	str	  VARCHAR(255),
	inum  INTEGER,
	dnum  DOUBLE PRECISION,
	ts    TIMESTAMP,
	PRIMARY KEY (pk)
);

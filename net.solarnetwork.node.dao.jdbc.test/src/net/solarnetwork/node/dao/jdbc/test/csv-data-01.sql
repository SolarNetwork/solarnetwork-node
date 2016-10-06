CREATE TABLE solarnode.test_csv_io (
	pk	  BIGINT NOT NULL,
	str	  VARCHAR(255),
	inum  INTEGER,
	dnum  DOUBLE,
	ts    TIMESTAMP,
	PRIMARY KEY (pk)
);

INSERT INTO solarnode.test_csv_io (pk, str, inum, dnum, ts)
	VALUES (1, 's01', 1, 1.0, TIMESTAMP('2016-10-01 12:01:02.345'));
INSERT INTO solarnode.test_csv_io (pk, str, inum, dnum, ts)
	VALUES (2, NULL, 2, 2.0, TIMESTAMP('2016-10-02 12:01:02.345'));
INSERT INTO solarnode.test_csv_io (pk, str, inum, dnum, ts)
	VALUES (3, 's03', NULL, 3.0, TIMESTAMP('2016-10-03 12:01:03.345'));
INSERT INTO solarnode.test_csv_io (pk, str, inum, dnum, ts)
	VALUES (4, 's04', 4, NULL, TIMESTAMP('2016-10-04 12:01:04.345'));
INSERT INTO solarnode.test_csv_io (pk, str, inum, dnum, ts)
	VALUES (5, 's05', 5, 5.0, NULL);

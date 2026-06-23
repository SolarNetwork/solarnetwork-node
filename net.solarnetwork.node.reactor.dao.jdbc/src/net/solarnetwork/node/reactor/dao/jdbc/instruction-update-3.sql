DELETE FROM solarnode.sn_instruction;

ALTER TABLE solarnode.sn_instruction_status DROP CONSTRAINT sn_instruction_status_instruction_fk;
ALTER TABLE solarnode.sn_instruction_status DROP PRIMARY KEY;

ALTER TABLE solarnode.sn_instruction_param DROP CONSTRAINT sn_instruction_param_instruction_fk;
ALTER TABLE solarnode.sn_instruction_param DROP PRIMARY KEY;

ALTER TABLE solarnode.sn_instruction DROP CONSTRAINT sn_instruction_unq;
ALTER TABLE solarnode.sn_instruction DROP PRIMARY KEY;
ALTER TABLE solarnode.sn_instruction DROP COLUMN instruction_id;
ALTER TABLE solarnode.sn_instruction ADD CONSTRAINT sn_instruction_pkey PRIMARY KEY (id, instructor_id);

ALTER TABLE solarnode.sn_instruction_param ADD COLUMN instructor_id VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE solarnode.sn_instruction_param ADD CONSTRAINT sn_instruction_param_pkey 
	PRIMARY KEY (instruction_id, instructor_id, pos);
ALTER TABLE solarnode.sn_instruction_param ADD CONSTRAINT sn_instruction_param_instruction_fk 
	FOREIGN KEY (instruction_id, instructor_id) REFERENCES solarnode.sn_instruction ON DELETE CASCADE;

ALTER TABLE solarnode.sn_instruction_status ADD COLUMN instructor_id VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE solarnode.sn_instruction_status ADD CONSTRAINT sn_instruction_status_pkey 
	PRIMARY KEY (instruction_id, instructor_id);
ALTER TABLE solarnode.sn_instruction_status ADD CONSTRAINT sn_instruction_status_instruction_fk 
	FOREIGN KEY (instruction_id, instructor_id) REFERENCES solarnode.sn_instruction ON DELETE CASCADE;

UPDATE solarnode.sn_settings SET svalue = '3'
WHERE skey = 'solarnode.sn_instruction.version';

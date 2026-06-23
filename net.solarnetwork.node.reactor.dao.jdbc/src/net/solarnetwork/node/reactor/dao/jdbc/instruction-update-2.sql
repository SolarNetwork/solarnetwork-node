ALTER TABLE solarnode.sn_instruction_status
ADD COLUMN jparams VARCHAR(4096);

UPDATE solarnode.sn_settings SET svalue = '2'
WHERE skey = 'solarnode.sn_instruction.version';

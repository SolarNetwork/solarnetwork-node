ALTER TABLE solarnode.sn_instruction ADD COLUMN execute_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE solarnode.sn_settings SET svalue = '4'
WHERE skey = 'solarnode.sn_instruction.version';

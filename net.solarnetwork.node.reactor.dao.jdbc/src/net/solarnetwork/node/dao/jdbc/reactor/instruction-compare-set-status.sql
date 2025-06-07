UPDATE solarnode.sn_instruction_status
SET state = ?, jparams = ?, ack_state = ?, modified = CURRENT_TIMESTAMP
WHERE instruction_id = ? AND instructor_id = ? AND state = ?

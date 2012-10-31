UPDATE solarnode.sn_instruction_status
SET state = ?, ack_state = ?, modified = CURRENT_TIMESTAMP
WHERE instruction_id = ?

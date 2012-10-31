DELETE FROM solarnode.sn_instruction
WHERE id IN (
	SELECT i.id FROM solarnode.sn_instruction i
	INNER JOIN solarnode.sn_instruction_status s ON s.instruction_id = i.id
	WHERE i.created < ? 
		AND s.state IN ('Completed', 'Declined') 
		AND s.state = s.ack_state
)

SELECT 
	i.id, i.topic, i.created, i.instruction_id, i.instructor_id, 
	s.state, s.modified, s.jparams, s.ack_state,
	p.pname, p.pvalue
FROM solarnode.sn_instruction i
INNER JOIN solarnode.sn_instruction_status s ON s.instruction_id = i.id
LEFT OUTER JOIN solarnode.sn_instruction_param p ON p.instruction_id = i.id
WHERE s.state <> s.ack_state OR s.ack_state IS NULL
ORDER BY i.id ASC, p.pos ASC

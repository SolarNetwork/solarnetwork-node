SELECT 
	i.id, i.instructor_id, i.topic, i.created,
	s.state, s.modified, s.jparams, s.ack_state,
	p.pname, p.pvalue
FROM solarnode.sn_instruction i
INNER JOIN solarnode.sn_instruction_status s ON s.instruction_id = i.id AND s.instructor_id = i.instructor_id
LEFT OUTER JOIN solarnode.sn_instruction_param p ON p.instruction_id = i.id AND p.instructor_id = i.instructor_id
WHERE i.id = ? AND i.instructor_id = ?
ORDER BY p.pos ASC

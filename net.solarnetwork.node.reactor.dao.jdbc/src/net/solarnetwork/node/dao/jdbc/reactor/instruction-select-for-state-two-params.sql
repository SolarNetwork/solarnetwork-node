SELECT 
	i.id, i.instructor_id, i.topic, i.created,
	s.state, s.modified, s.jparams, s.ack_state,
	p.pname, p.pvalue
FROM solarnode.sn_instruction i
INNER JOIN solarnode.sn_instruction_status s ON s.instruction_id = i.id AND s.instructor_id = i.instructor_id
INNER JOIN solarnode.sn_instruction_param p ON p.instruction_id = i.id AND p.instructor_id = i.instructor_id
LEFT OUTER JOIN solarnode.sn_instruction_param p2 ON p2.instruction_id = i.id AND p2.instructor_id = i.instructor_id
	AND p2.pname = ?  AND p2.pvalue = ?
LEFT OUTER JOIN solarnode.sn_instruction_param p3 ON p3.instruction_id = i.id AND p3.instructor_id = i.instructor_id
	AND p3.pname = ? AND p3.pvalue = ?
WHERE s.state = ?
	AND p2.pname IS NOT NULL
	AND p3.pname IS NOT NULL
ORDER BY i.instructor_id ASC, i.id ASC, p.pos ASC

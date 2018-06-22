/* ==================================================================
 * SimpleReactorService.java - Mar 1, 2011 4:46:31 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.reactor.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionDao;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.ReactorSerializationService;
import net.solarnetwork.node.reactor.ReactorService;

/**
 * Simple implementation of {@link ReactorService}.
 * 
 * @author matt
 * @version 1.1
 */
public class SimpleReactorService implements ReactorService {

	private Collection<ReactorSerializationService> serializationServices;
	private InstructionDao instructionDao;

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public InstructionStatus processInstruction(Instruction instruction) {
		// look for an existing status...
		Instruction instr = instructionDao.getInstruction(instruction.getRemoteInstructionId(),
				instruction.getInstructorId());
		if ( instr == null ) {
			// persist new status
			instr = instructionDao.getInstruction(instructionDao.storeInstruction(instruction));
		}
		return instr.getStatus();
	}

	@Override
	public List<Instruction> parseInstructions(String instructorId, Object data, String dataType,
			Map<String, ?> properties) {
		List<Instruction> instructions = null;
		for ( ReactorSerializationService rss : serializationServices ) {
			try {
				instructions = rss.decodeInstructions(instructorId, data, dataType, properties);
			} catch ( IllegalArgumentException e ) {
				// ignore this, and just move on
			}
			if ( instructions != null ) {
				break;
			}
		}
		return (instructions != null ? instructions : Collections.<Instruction> emptyList());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public List<InstructionStatus> processInstruction(String instructorId, Object data, String dataType,
			Map<String, ?> properties) {
		List<Instruction> instructions = parseInstructions(instructorId, data, dataType, properties);
		List<InstructionStatus> results = new ArrayList<InstructionStatus>(instructions.size());
		for ( Instruction instruction : instructions ) {
			InstructionStatus status = processInstruction(instruction);
			if ( status != null ) {
				results.add(status);
			}
		}
		return results;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Long storeInstruction(Instruction instruction) {
		if ( instruction.getId() != null ) {
			instructionDao.storeInstructionStatus(instruction.getId(), instruction.getStatus());
			return instruction.getId();
		}
		return instructionDao.storeInstruction(instruction);
	}

	public void setSerializationServices(Collection<ReactorSerializationService> serializationServices) {
		this.serializationServices = serializationServices;
	}

	public void setInstructionDao(InstructionDao instructionDao) {
		this.instructionDao = instructionDao;
	}

}

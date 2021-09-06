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

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionDao;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.ReactorService;

/**
 * Simple implementation of {@link ReactorService}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class SimpleReactorService implements ReactorService {

	private final InstructionDao instructionDao;

	/**
	 * Constructor.
	 * 
	 * @param instructionDao
	 *        the instruction DAO to use
	 */
	public SimpleReactorService(InstructionDao instructionDao) {
		super();
		this.instructionDao = instructionDao;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public InstructionStatus processInstruction(Instruction instruction) {
		// look for an existing status...
		Instruction instr = instructionDao.getInstruction(instruction.getId(),
				instruction.getInstructorId());
		if ( instr == null ) {
			// persist new status
			instructionDao.storeInstruction(instruction);
			instr = instructionDao.getInstruction(instruction.getId(), instruction.getInstructorId());
		}
		return instr.getStatus();
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeInstruction(Instruction instruction) {
		if ( instruction.getStatus() != null ) {
			instructionDao.storeInstructionStatus(instruction.getId(), instruction.getInstructorId(),
					instruction.getStatus());
		}
		instructionDao.storeInstruction(instruction);
	}

}

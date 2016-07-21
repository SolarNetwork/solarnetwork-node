/* ==================================================================
 * InstructionDao.java - Feb 28, 2011 11:05:57 AM
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

package net.solarnetwork.node.reactor;

import java.util.List;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;

/**
 * DAO API for Instructor entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface InstructionDao {

	/**
	 * Store an Instruction instance and return its primary key.
	 * 
	 * @param instruction
	 * @return the local primary key
	 */
	Long storeInstruction(Instruction instruction);

	/**
	 * Get an Instruction instance by ID.
	 * 
	 * @param instructionId
	 *        the instruction ID
	 * @return the Instruction, or <em>null</em> if not found
	 */
	Instruction getInstruction(Long instructionId);

	/**
	 * Get an Instruction instance by the remote ID.
	 * 
	 * @param remoteInstructionId
	 *        the remote instruction ID
	 * @param instructorId
	 *        the instructor ID
	 * @return the Instruction, or <em>null</em> if not found
	 */
	Instruction getInstruction(String remoteInstructionId, String instructorId);

	/**
	 * Update an instruction status.
	 * 
	 * @param instruction
	 *        ID the ID of the instruction to update the status for
	 * @param status
	 *        the status
	 */
	void storeInstructionStatus(Long instructionId, InstructionStatus status);

	/**
	 * Find all instructions in a given state.
	 * 
	 * @param state
	 *        the instruction state
	 * @return the found instructions, or empty list if none available
	 */
	List<Instruction> findInstructionsForState(InstructionState state);

	/**
	 * Find all instructions needing acknowledgement.
	 * 
	 * @return the found instructions, or empty list if none available
	 */
	List<Instruction> findInstructionsForAcknowledgement();

	/**
	 * Delete Instruction entities that are not in the Received or Executing
	 * state and are older than a specified number of hours.
	 * 
	 * <p>
	 * This is designed to free up space from local database storage for devices
	 * with limited storage capacity.
	 * </p>
	 * 
	 * @param hours
	 *        the minimum number of hours old the data must be to delete
	 * @return the number of Instruction entities deleted
	 */
	int deleteHandledInstructionsOlderThan(int hours);

}

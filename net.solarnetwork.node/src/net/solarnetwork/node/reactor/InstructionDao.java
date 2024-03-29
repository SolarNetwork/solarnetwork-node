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
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * DAO API for Instructor entities.
 * 
 * @author matt
 * @version 2.1
 */
public interface InstructionDao {

	/**
	 * Store an Instruction instance.
	 * 
	 * @param instruction
	 *        the instruction to store
	 */
	void storeInstruction(Instruction instruction);

	/**
	 * Get an Instruction instance by the remote ID.
	 * 
	 * @param id
	 *        the instruction ID
	 * @param instructorId
	 *        the instructor ID
	 * @return the Instruction, or {@literal null} if not found
	 */
	Instruction getInstruction(Long id, String instructorId);

	/**
	 * Update an instruction status.
	 * 
	 * @param instructionId
	 *        the ID of the instruction to update the status for
	 * @param instructorId
	 *        the instructor ID
	 * @param status
	 *        the status
	 * @since 2.0
	 */
	void storeInstructionStatus(Long instructionId, String instructorId, InstructionStatus status);

	/**
	 * Update an instruction status only if it currently has an expected state.
	 * 
	 * <p>
	 * This is equivalent to an atomic compare-and-set operation. The status of
	 * the given instruction will be set to {@code status} only if the
	 * instruction with {@code instructionId} exists and has the
	 * {@code expectedState} state.
	 * </p>
	 * 
	 * @param instructionId
	 *        the ID of the instruction to update the status for
	 * @param instructorId
	 *        the instructor ID
	 * @param expectedState
	 *        the expected state of the instruction
	 * @param status
	 *        the desired status
	 * @return {@literal true} if the instruction status was updated
	 * @since 2.0
	 */
	boolean compareAndStoreInstructionStatus(Long instructionId, String instructorId,
			InstructionState expectedState, InstructionStatus status);

	/**
	 * Find all instructions in a given state.
	 * 
	 * @param state
	 *        the instruction state
	 * @return the found instructions, or empty list if none available
	 */
	List<Instruction> findInstructionsForState(InstructionState state);

	/**
	 * Find all instructions in a given state that also have a parameters for a
	 * given parent instruction.
	 * 
	 * @param state
	 *        the instruction state
	 * @param parentInstructorId
	 *        the parent instructor ID
	 * @param parentInstructionId
	 *        the parent instruction ID
	 * @return the found instructions, or empty list if none available
	 * @since 2.1
	 */
	List<Instruction> findInstructionsForStateAndParent(InstructionState state,
			String parentInstructorId, Long parentInstructionId);

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

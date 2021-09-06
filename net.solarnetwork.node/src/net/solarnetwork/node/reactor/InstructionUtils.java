/* ==================================================================
 * InstructionUtils.java - Jul 20, 2013 6:16:07 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

import java.time.Instant;
import java.util.Map;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Utilities for dealing with common Instruction patterns.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public final class InstructionUtils {

	private InstructionUtils() {
		// can't create me
	}

	/**
	 * Create a new status for a given instruction.
	 * 
	 * @param instruction
	 *        the instruction, or {@literal null}
	 * @param state
	 *        the new state
	 * @return the status, never {@literal null}
	 */
	public static InstructionStatus createStatus(Instruction instruction, InstructionState state) {
		return createStatus(instruction, state, Instant.now(), null);
	}

	/**
	 * Create a new status for a given instruction.
	 * 
	 * @param instruction
	 *        the instruction, or {@literal null}
	 * @param state
	 *        the new state
	 * @param resultParameters
	 *        the optional result parameters
	 * @return the status, never {@literal null}
	 */
	public static InstructionStatus createStatus(Instruction instruction, InstructionState state,
			Map<String, ?> resultParameters) {
		return createStatus(instruction, state, Instant.now(), resultParameters);
	}

	/**
	 * Create a new status for a given instruction.
	 * 
	 * @param instruction
	 *        the instruction, or {@literal null}
	 * @param state
	 *        the new state
	 * @param date
	 *        the status date
	 * @param resultParameters
	 *        the optional result parameters
	 * @return the status, never {@literal null}
	 */
	public static InstructionStatus createStatus(Instruction instruction, InstructionState state,
			Instant date, Map<String, ?> resultParameters) {
		final InstructionStatus status = (instruction != null ? instruction.getStatus() : null);
		return (status != null ? status.newCopyWithState(state, resultParameters)
				: new BasicInstructionStatus(instruction != null ? instruction.getId() : null, state,
						date != null ? date : Instant.now(), null, resultParameters));
	}

	/**
	 * Create a new local instruction for
	 * 
	 * @param topic
	 *        the instruction topic
	 * @param paramName
	 *        an optional parameter name
	 * @param paramValue
	 *        if {@code paramName} provided then the corresponding parameter
	 *        value
	 * @return the new instruction, never {@literal null}
	 */
	public static Instruction createLocalInstruction(String topic, String paramName, String paramValue) {
		BasicInstruction instr = new BasicInstruction(null, topic, Instant.now(),
				Instruction.LOCAL_INSTRUCTION_ID, null);
		if ( paramName != null && paramValue != null ) {
			instr.addParameter(paramName, paramValue);
		}
		return instr;
	}

	/**
	 * Create a new local instruction for
	 * 
	 * @param controlId
	 *        the ID of the control to set the control value to
	 * @param controlValue
	 *        the value to set the control to
	 * @return the new instruction, never {@literal null}
	 */
	public static Instruction createSetControlValueLocalInstruction(String controlId,
			Object controlValue) {
		return createLocalInstruction(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER, controlId,
				controlValue.toString());
	}

}

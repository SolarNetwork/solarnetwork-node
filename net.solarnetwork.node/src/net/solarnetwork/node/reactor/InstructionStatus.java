/* ==================================================================
 * InstructionStatus.java - Feb 28, 2011 10:50:38 AM
 * 
 * Copyright 2007 SolarNetwork.net Dev Team
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

import java.util.Date;
import java.util.Map;
import net.solarnetwork.node.reactor.support.BasicInstructionStatus;

/**
 * Status information for a single Instruction.
 * 
 * @author matt
 * @version 1.2
 */
public interface InstructionStatus {

	enum InstructionState {

		/**
		 * The instruction has been received, but has not been looked at yet. It
		 * will be looked at as soon as possible.
		 */
		Received,

		/**
		 * The instruction has been received and is being executed currently.
		 */
		Executing,

		/**
		 * The instruction was received but has been declined and will not be
		 * executed.
		 */
		Declined,

		/**
		 * The instruction was received and has been executed.
		 */
		Completed;

	}

	/**
	 * A standard result parameter key for a message (typically an error
	 * message).
	 * 
	 * @since 1.1
	 */
	String MESSAGE_RESULT_PARAM = "message";

	/**
	 * A standard result parameter key for an error code.
	 * 
	 * @since 1.1
	 */
	String ERROR_CODE_RESULT_PARAM = "code";

	/**
	 * Get the ID of the instruction this state is associated with.
	 * 
	 * @return the primary key
	 */
	Long getInstructionId();

	/**
	 * Get the current instruction state.
	 * 
	 * @return the current instruction state
	 */
	InstructionState getInstructionState();

	/**
	 * Get the acknowledged instruction state.
	 * 
	 * <p>
	 * This is the state that has been posted back to SolarNet.
	 * </p>
	 * 
	 * @return the acknowledged instruction state, or <em>null</em> if never
	 *         acknowledged
	 */
	InstructionState getAcknowledgedInstructionState();

	/**
	 * Get the date/time the instruction state was queried.
	 * 
	 * @return the status date
	 */
	Date getStatusDate();

	/**
	 * Get result parameters.
	 * 
	 * @return the result parameters, or {@literal null} if none available
	 * @since 1.1
	 */
	Map<String, ?> getResultParameters();

	/**
	 * Create a new InstructionStatus copy with a new state.
	 * 
	 * @param newState
	 *        the new state
	 * @return the new instance
	 */
	InstructionStatus newCopyWithState(InstructionState newState);

	/**
	 * Create a new InstructionStatus copy with a new state and result
	 * parameters.
	 * 
	 * @param newState
	 *        the new state
	 * @param resultParameters
	 *        the result parameters
	 * @return the new instance
	 * @since 1.1
	 */
	InstructionStatus newCopyWithState(InstructionState newState, Map<String, ?> resultParameters);

	/**
	 * Create a new InstructionStatus copy with a new acknowledged state.
	 * 
	 * @param newState
	 *        the new state
	 * @return the new instance
	 */
	InstructionStatus newCopyWithAcknowledgedState(InstructionState newState);

	/**
	 * Create a new InstructionStatus copy with a new acknowledged state.
	 * 
	 * @param newState
	 *        the new state
	 * @param resultParameters
	 *        the result parameters
	 * @return the new instance
	 * @since 1.1
	 */
	InstructionStatus newCopyWithAcknowledgedState(InstructionState newState,
			Map<String, ?> resultParameters);

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
	 * @since 1.2
	 */
	static InstructionStatus createStatus(Instruction instruction, InstructionState state, Date date,
			Map<String, ?> resultParameters) {
		final InstructionStatus status = (instruction != null ? instruction.getStatus() : null);
		return (status != null ? status.newCopyWithState(state, resultParameters)
				: new BasicInstructionStatus(instruction != null ? instruction.getId() : null, state,
						date != null ? date : new Date(), null, resultParameters));
	}

}

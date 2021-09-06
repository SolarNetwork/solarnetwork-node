/* ==================================================================
 * InstructionStatus.java - 6/09/2021 10:14:51 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import java.util.Map;

/**
 * Status information for a single Instruction.
 * 
 * @author matt
 * @version 2.0
 */
public interface InstructionStatus extends net.solarnetwork.domain.InstructionStatus {

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
	 * Create a new InstructionStatus copy with a new state.
	 * 
	 * @param newState
	 *        the new state
	 * @return the new instance
	 */
	@Override
	default InstructionStatus newCopyWithState(InstructionState newState) {
		return newCopyWithState(newState, null);
	}

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
	@Override
	InstructionStatus newCopyWithState(InstructionState newState, Map<String, ?> resultParameters);

	/**
	 * Create a new InstructionStatus copy with a new acknowledged state.
	 * 
	 * @param newState
	 *        the new state
	 * @return the new instance
	 */
	default InstructionStatus newCopyWithAcknowledgedState(InstructionState newState) {
		return newCopyWithAcknowledgedState(newState, null);
	}

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

}

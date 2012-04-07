/* ==================================================================
 * InstructionStatus.java - Feb 28, 2011 10:50:38 AM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.reactor;

import java.util.Date;

/**
 * Status information for a single Instruction.
 * 
 * @author matt
 * @version $Revision$
 */
public interface InstructionStatus {

	enum InstructionState {
		
		/**
		 * The instruction has been received, but has not been looked at yet. 
		 * It will be looked at as soon as possible.
		 */
		Received,
		
		/** 
		 * The instruction has been received and is being executed currently.
		 */
		Executing,
		
		/**
		 * The instruction was received but has been declined and will not be executed.
		 */
		Declined,
		
		/**
		 * The instruction was received and has been executed.
		 */
		Completed;
		
	}
	
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
	 * <p>This is the state that has been posted back to SolarNet.</p>
	 * 
	 * @return the acknowledged instruction state, or <em>null</em> if never acknowledged
	 */
	InstructionState getAcknowledgedInstructionState();
	
	/**
	 * Get the date/time the instruction state was queried.
	 * 
	 * @return the status date
	 */
	Date getStatusDate();
	
	/**
	 * Create a new InstructionStatus copy with a new state and date.
	 * 
	 * @param newState the new state
	 * @return the new instance
	 */
	InstructionStatus newCopyWithState(InstructionState newState);
	
	/**
	 * Create a new InstructionStatus copy with a new acknowledged state.
	 * 
	 * @param newState the new state
	 * @return the new instance
	 */
	InstructionStatus newCopyWithAcknowledgedState(InstructionState newState);
	
}

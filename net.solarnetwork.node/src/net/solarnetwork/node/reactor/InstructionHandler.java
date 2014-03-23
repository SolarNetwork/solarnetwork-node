/* ==================================================================
 * InstructionHandler.java - Oct 1, 2011 11:01:07 AM
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

import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;

/**
 * API to be implemented by a service that can handle instructions.
 * 
 * @author matt
 * @version 1.1
 */
public interface InstructionHandler {

	/**
	 * The instruction topic for setting control parameters. By convention the
	 * instruction should have a parameter whose key is the ID of the control to
	 * change and whose value is some control-specific data.
	 */
	String TOPIC_SET_CONTROL_PARAMETER = "SetControlParameter";

	/**
	 * The instruction topic for balancing power generation to power demand. By
	 * convention the instruction should have a parameter whose key is the ID of
	 * the control that should respond to the balancing request and whose value
	 * is an integer percentage (0 - 100) of the maximum desired power
	 * generation capacity.
	 */
	String TOPIC_DEMAND_BALANCE = "DemandBalanceGeneration";

	/**
	 * Test if a topic is handled by this handler.
	 * 
	 * @param topic
	 *        the topic
	 * @return <em>true</em> only if this handler can execute the job for the
	 *         given topic
	 */
	boolean handlesTopic(String topic);

	/**
	 * Process an instruction.
	 * 
	 * @param instruction
	 *        the instruction to process
	 * @return the state for the instruction, or <em>null</em> if the
	 *         instruction was not handled
	 */
	InstructionState processInstruction(Instruction instruction);

}

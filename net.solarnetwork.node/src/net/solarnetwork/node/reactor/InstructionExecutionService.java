/* ==================================================================
 * InstructionExecutionService.java - 8/06/2018 7:21:47 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

/**
 * API for executing instructions.
 * 
 * @author matt
 * @version 1.0
 * @since 1.58
 */
public interface InstructionExecutionService {

	/**
	 * Execute a single instruction.
	 * 
	 * @param instruction
	 *        the instruction to execute
	 * @return the resulting status for the instruction, or {@code null} if not
	 *         handled
	 */
	InstructionStatus executeInstruction(Instruction instruction);

}

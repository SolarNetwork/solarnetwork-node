/* ==================================================================
 * ReactorService.java - Feb 28, 2011 10:15:26 AM
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
import java.util.Map;

/**
 * API for reacting to SolarNet service instruction requests.
 * 
 * @author matt
 * @version 1.1
 */
public interface ReactorService {

	/**
	 * Process an instruction.
	 * 
	 * @param instruction
	 *        the instruction to process
	 * @return the status for the instruction
	 */
	InstructionStatus processInstruction(Instruction instruction);

	/**
	 * Parse a set of encoded instructions.
	 * 
	 * @param instructorId
	 *        the ID of the instructor
	 * @param data
	 *        the InputStream to parse for Instruction instances
	 * @param dataType
	 *        the data type
	 * @param properties
	 *        optional parsing properties and metadata
	 * @return the parsed instructions, never {@literal null}
	 * @since 1.1
	 */
	List<Instruction> parseInstructions(String instructorId, Object data, String dataType,
			Map<String, ?> properties);

	/**
	 * Attempt to parse and process an Instruction.
	 * 
	 * @param instructorId
	 *        the ID of the instructor
	 * @param data
	 *        the InputStream to parse for Instruction instances
	 * @param dataType
	 *        the data type
	 * @param properties
	 *        optional parsing properties and metadata
	 * @return the status for any parsed Instruction instances, or an empty list
	 *         if none parsed, never {@literal null}
	 */
	List<InstructionStatus> processInstruction(String instructorId, Object data, String dataType,
			Map<String, ?> properties);

	/**
	 * Store an Instruction instance in local storage and return its primary
	 * key.
	 * 
	 * @param instruction
	 * @return the local primary key
	 * @since 1.1
	 */
	Long storeInstruction(Instruction instruction);
}

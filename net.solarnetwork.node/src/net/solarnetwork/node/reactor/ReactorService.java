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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.reactor;

import java.util.List;
import java.util.Map;

/**
 * API for reacting to SolarNet service instruction requests.
 * 
 * @author matt
 * @version $Revision$
 */
public interface ReactorService {

	/**
	 * Process an instruction.
	 * 
	 * @param instruction the instruction to process
	 * @return the status for the instruction
	 */
	InstructionStatus processInstruction(Instruction instruction);
	
	/**
	 * Attempt to parse and process an Instruction.
	 * 
	 * @param instructorId the ID of the instructor
	 * @param data the InputStream to parse for Instruction instances
	 * @param dataType the data type
	 * @param properties optional parsing properties and metadata
	 * @return the status for any parsed Instruction instances, or an empty list if none parsed
	 */
	List<InstructionStatus> processInstruction(String instructorId, Object data, 
			String dataType, Map<String, ?> properties);
}

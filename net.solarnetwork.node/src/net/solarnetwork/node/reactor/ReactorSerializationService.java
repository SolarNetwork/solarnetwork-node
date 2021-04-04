/* ==================================================================
 * ReactorSerializationService.java - Mar 1, 2011 9:51:04 AM
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * API for parsing instruction data encoded in some way back into an Instruction
 * instance.
 * 
 * @author matt
 * @version 1.0
 */
public interface ReactorSerializationService {

	/**
	 * Parse some data object and return new Instruction instances.
	 * 
	 * @param instructorId
	 *        the instructor ID
	 * @param in
	 *        the input data to parse
	 * @param type
	 *        the type of data {@code in} is encoded as (text/xml, text/json,
	 *        etc.)
	 * @param properties
	 *        optional properties and metadata for decoding
	 * @return the parsed Instruction instances, or an empty list if unable to
	 *         parse anything
	 * @throws IllegalArgumentException
	 *         if the {@code in} object is not supported
	 */
	List<Instruction> decodeInstructions(String instructorId, Object in, String type,
			Map<String, ?> properties);

	/**
	 * Encode a collection of Instruction objects as a data object.
	 * 
	 * @param instructions
	 *        the instructions to encode
	 * @param type
	 *        the type of data desired (text/xml, text/json, etc.)
	 * @param properties
	 *        optional properties and metadata for encoding
	 * @return the encoded instruction
	 * @throws IllegalArgumentException
	 *         if the {@code in} object is not supported
	 */
	Object encodeInstructions(Collection<Instruction> instructions, String type,
			Map<String, ?> properties);

}

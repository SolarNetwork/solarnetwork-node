/* ==================================================================
 * Instruction.java - Feb 28, 2011 10:16:35 AM
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
 * API for a single, immutable instruction with associated parameters.
 * 
 * <p>
 * An instruction is like a single name (the {@code topic}) with an arbitrary
 * number of named key/value parameters. All parameter names and values are
 * Strings. Each parameter can have any number of associated values.
 * </p>
 * 
 * <p>
 * An Instruction is considered equal to another Instruction only if the
 * {@code instructionId} and {@code instructorId} values are equal.
 * </p>
 * 
 * @author matt
 * @version $Revision$
 */
public interface Instruction {

	/** An ID to use for locally-initiated instructions. */
	String LOCAL_INSTRUCTION_ID = "LOCAL";

	/**
	 * Get a locally-assigned unique ID.
	 * 
	 * @return local unique ID
	 */
	Long getId();

	/**
	 * Get the topic of the instruction -- a unique identifier for the
	 * instruction type.
	 * 
	 * @return the topic name
	 */
	String getTopic();

	/**
	 * Get the date/time the instruction was requested.
	 * 
	 * @return the date
	 */
	Date getInstructionDate();

	/**
	 * Get the SolarNet-assigned unique ID for this instruction.
	 * 
	 * @return unique ID
	 */
	String getRemoteInstructionId();

	/**
	 * Get the unique ID for the sender of the instruction, for example the DN
	 * of the sender's certificate.
	 * 
	 * @return the instructor ID
	 */
	String getInstructorId();

	/**
	 * Get an Iterator of all unique instruction parameter names.
	 * 
	 * @return iterator
	 */
	Iterable<String> getParameterNames();

	/**
	 * Test if a specific parameter has an associated value available.
	 * 
	 * @param parameterName
	 *        the parameter name to test for
	 * @return <em>true</em> if the parameter name has at least one value
	 *         available
	 */
	boolean isParameterAvailable(String parameterName);

	/**
	 * Get a single parameter value for a specific parameter name.
	 * 
	 * @param parameterName
	 *        the parameter name to get the value for
	 * @return the first available parameter name, or <em>null</em> if not
	 *         available
	 */
	String getParameterValue(String parameterName);

	/**
	 * Get all parameter values for a specific parameter name;
	 * 
	 * @param parameterName
	 *        the parameter name to get the values for
	 * @return all available parameter values, or <em>null</em> if not available
	 */
	String[] getAllParameterValues(String parameterName);

	/**
	 * Get the instruction status.
	 * 
	 * @return the status
	 */
	InstructionStatus getStatus();
}

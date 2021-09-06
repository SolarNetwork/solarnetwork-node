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
 */

package net.solarnetwork.node.reactor;

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
 * @version 2.0
 */
public interface Instruction extends net.solarnetwork.domain.Instruction {

	/**
	 * An ID to use for locally-initiated instructions, in
	 * {@link #getInstructorId()}.
	 */
	String LOCAL_INSTRUCTION_ID = "LOCAL";

	/**
	 * Get the unique ID for the sender of the instruction, for example the DN
	 * of the sender's certificate.
	 * 
	 * @return the instructor ID
	 */
	String getInstructorId();

	/**
	 * Get the instruction state.
	 * 
	 * @return the state, or {@literal null} if none available
	 */
	@Override
	InstructionStatus getStatus();

}

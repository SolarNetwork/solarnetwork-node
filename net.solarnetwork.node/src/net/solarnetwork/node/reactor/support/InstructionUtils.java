/* ==================================================================
 * InstructionUtils.java - Jul 20, 2013 6:16:07 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.reactor.support;

import java.util.Date;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;

/**
 * Utilities for dealing with common Instruction patterns.
 * 
 * @author matt
 * @version 1.1
 */
public final class InstructionUtils {

	private InstructionUtils() {
		// can't create me
	}

	/**
	 * Given a collection of {@link InstructionHandler} objects, find one that
	 * can handle a specific {@link Instruction}.
	 * 
	 * <p>
	 * This method will iterate over the provided {@code handlers} and ask each
	 * one if they handle the topic of the provided {@code instruction}. If the
	 * handler does, it is asked to process the instruction. The first non-null
	 * result returned by a handler will be returned.
	 * </p>
	 * 
	 * @param handlers
	 *        the collection of handlers to search over
	 * @param instruction
	 *        the instruction to ask the handlers to perform
	 * @return the first non-null result of
	 *         {@link InstructionHandler#processInstruction(Instruction)}, or
	 *         <em>null</em> if no handler returns a value
	 */
	public static InstructionState handleInstruction(Iterable<InstructionHandler> handlers,
			Instruction instruction) {
		InstructionState result = null;
		for ( InstructionHandler handler : handlers ) {
			if ( handler.handlesTopic(instruction.getTopic()) ) {
				result = handler.processInstruction(instruction);
			}
			if ( result != null ) {
				break;
			}
		}
		return result;
	}

	/**
	 * Given a collection of {@link InstructionHandler} objects, find the first
	 * one that can handle the
	 * {@link InstructionHandler#TOPIC_SET_CONTROL_PARAMETER} topic for the
	 * given {@code controlId} and {@code controlValue}. The
	 * {@link Instruction#getRemoteInstructionId()} and
	 * {@link Instruction#getInstructorId()} values are set to
	 * {@link Instruction#LOCAL_INSTRUCTION_ID}.
	 * 
	 * @param handlers
	 *        the collection of handlers to search over
	 * @param controlId
	 *        the ID of the control to set the control value to
	 * @param controlValue
	 *        the value to set the control to
	 * @return the first non-null result of
	 *         {@link InstructionHandler#processInstruction(Instruction)}, or
	 *         <em>null</em> if no handler returns a value
	 */
	public static InstructionState setControlParameter(final Iterable<InstructionHandler> handlers,
			final String controlId, final String controlValue) {
		BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER,
				new Date(), Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(controlId, String.valueOf(controlValue));
		return InstructionUtils.handleInstruction(handlers, instr);
	}

}

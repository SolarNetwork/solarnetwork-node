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

package net.solarnetwork.node.reactor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Utilities for dealing with common Instruction patterns.
 * 
 * @author matt
 * @version 1.2
 * @since 2.0
 */
public final class InstructionUtils {

	private InstructionUtils() {
		// can't create me
	}

	// inner class to lazy-init LOCAL_ID with system time seed
	private static class LocalId {

		private static final AtomicLong LOCAL_ID = initLocalId();

		private static AtomicLong initLocalId() {
			return new AtomicLong(System.currentTimeMillis());
		}
	}

	/**
	 * Generate a new local ID.
	 * 
	 * <p>
	 * Local IDs are sequentially generated, but seeded by the current date when
	 * this method is first invoked. This is to help reduce the risk of
	 * generating duplicate IDs across JVM restarts, but is dependent on the
	 * system clock to achieve that.
	 * </p>
	 * 
	 * @return a new local ID
	 */
	private static Long localId() {
		return LocalId.LOCAL_ID.getAndIncrement();
	}

	/**
	 * Create a new status for a given instruction.
	 * 
	 * @param instruction
	 *        the instruction, or {@literal null}
	 * @param state
	 *        the new state
	 * @return the status, never {@literal null}
	 */
	public static InstructionStatus createStatus(Instruction instruction, InstructionState state) {
		return createStatus(instruction, state, Instant.now(), null);
	}

	/**
	 * Create a new status for a given instruction.
	 * 
	 * @param instruction
	 *        the instruction, or {@literal null}
	 * @param state
	 *        the new state
	 * @param resultParameters
	 *        the optional result parameters
	 * @return the status, never {@literal null}
	 */
	public static InstructionStatus createStatus(Instruction instruction, InstructionState state,
			Map<String, ?> resultParameters) {
		return createStatus(instruction, state, Instant.now(), resultParameters);
	}

	/**
	 * Create a new status for a given instruction.
	 * 
	 * @param instruction
	 *        the instruction, or {@literal null}
	 * @param state
	 *        the new state
	 * @param date
	 *        the status date
	 * @param resultParameters
	 *        the optional result parameters
	 * @return the status, never {@literal null}
	 */
	public static InstructionStatus createStatus(Instruction instruction, InstructionState state,
			Instant date, Map<String, ?> resultParameters) {
		final InstructionStatus status = (instruction != null ? instruction.getStatus() : null);
		return (status != null ? status.newCopyWithState(state, resultParameters)
				: new BasicInstructionStatus(instruction != null ? instruction.getId() : null, state,
						date != null ? date : Instant.now(), null, resultParameters));
	}

	/**
	 * Create a new local instruction with an optional parameter.
	 * 
	 * <p>
	 * The returned {@link Instruction#getInstructorId()} value will be
	 * {@link Instruction#LOCAL_INSTRUCTION_ID}.
	 * </p>
	 * 
	 * @param topic
	 *        the instruction topic
	 * @param params
	 *        an optional map of parameters
	 * @return the new instruction, never {@literal null}
	 */
	public static Instruction createLocalInstruction(String topic, Map<String, String> params) {
		BasicInstruction instr = new BasicInstruction(localId(), topic, Instant.now(),
				Instruction.LOCAL_INSTRUCTION_ID, null);
		if ( params != null ) {
			for ( Entry<String, String> me : params.entrySet() ) {
				instr.addParameter(me.getKey(), me.getValue());
			}
		}
		return instr;
	}

	/**
	 * Create a new local instruction with an optional parameter.
	 * 
	 * <p>
	 * The returned {@link Instruction#getInstructorId()} value will be
	 * {@link Instruction#LOCAL_INSTRUCTION_ID}.
	 * </p>
	 * 
	 * @param topic
	 *        the instruction topic
	 * @param paramName
	 *        an optional parameter name
	 * @param paramValue
	 *        if {@code paramName} provided then the corresponding parameter
	 *        value
	 * @return the new instruction, never {@literal null}
	 */
	public static Instruction createLocalInstruction(String topic, String paramName, String paramValue) {
		BasicInstruction instr = new BasicInstruction(localId(), topic, Instant.now(),
				Instruction.LOCAL_INSTRUCTION_ID, null);
		if ( paramName != null && paramValue != null ) {
			instr.addParameter(paramName, paramValue);
		}
		return instr;
	}

	/**
	 * Create a new local instruction for
	 * 
	 * <p>
	 * The returned {@link Instruction#getInstructorId()} value will be
	 * {@link Instruction#LOCAL_INSTRUCTION_ID}.
	 * </p>
	 * 
	 * @param controlId
	 *        the ID of the control to set the control value to
	 * @param controlValue
	 *        the value to set the control to
	 * @return the new instruction, never {@literal null}
	 */
	public static Instruction createSetControlValueLocalInstruction(String controlId,
			Object controlValue) {
		return createLocalInstruction(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER, controlId,
				controlValue.toString());
	}

	/**
	 * Create a result parameter map for an error message and/or code.
	 * 
	 * @param message
	 *        the message
	 * @param code
	 *        the code
	 * @return the map, never {@literal null}
	 * @since 1.1
	 */
	public static Map<String, Object> createErrorResultParameters(String message, String code) {
		Map<String, Object> result = new LinkedHashMap<>(2);
		if ( message != null && !message.isEmpty() ) {
			result.put(InstructionStatus.MESSAGE_RESULT_PARAM, message);
		}
		if ( code != null && !code.isEmpty() ) {
			result.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, code);
		}
		return result;
	}

	/**
	 * Create a local instruction from another instruction.
	 * 
	 * @param instr
	 *        the instruction
	 * @return the new instruction with a local ID assigned
	 * @since 1.2
	 */
	public static Instruction localInstructionFrom(net.solarnetwork.domain.Instruction instr) {
		return new BasicInstruction(BasicInstruction.from(instr, Instruction.LOCAL_INSTRUCTION_ID),
				localId(), null);
	}

}

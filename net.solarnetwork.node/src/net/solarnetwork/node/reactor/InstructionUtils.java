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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Utilities for dealing with common Instruction patterns.
 *
 * @author matt
 * @version 1.3
 * @since 2.0
 */
public final class InstructionUtils {

	private InstructionUtils() {
		// can't create me
	}

	/**
	 * Create a new status for a given instruction.
	 *
	 * @param instruction
	 *        the instruction, or {@code null}
	 * @param state
	 *        the new state
	 * @return the status, never {@code null}
	 */
	public static InstructionStatus createStatus(Instruction instruction, InstructionState state) {
		return createStatus(instruction, state, Instant.now(), null);
	}

	/**
	 * Create a new status for a given instruction.
	 *
	 * @param instruction
	 *        the instruction, or {@code null}
	 * @param state
	 *        the new state
	 * @param resultParameters
	 *        the optional result parameters
	 * @return the status, never {@code null}
	 */
	public static InstructionStatus createStatus(Instruction instruction, InstructionState state,
			@Nullable Map<String, ?> resultParameters) {
		return createStatus(instruction, state, Instant.now(), resultParameters);
	}

	/**
	 * Create a new status for a given instruction.
	 *
	 * @param instruction
	 *        the instruction, or {@code null}
	 * @param state
	 *        the new state
	 * @param date
	 *        the status date
	 * @param resultParameters
	 *        the optional result parameters
	 * @return the status, never {@code null}
	 */
	public static InstructionStatus createStatus(Instruction instruction, InstructionState state,
			@Nullable Instant date, @Nullable Map<String, ?> resultParameters) {
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
	 * @param instructionDate
	 *        the instruction date; if {@code null} then the current instant
	 *        will be used
	 * @param params
	 *        an optional map of parameters
	 * @return the new instruction, never {@code null}
	 * @since 1.3
	 */
	public static Instruction createLocalInstruction(String topic, @Nullable Instant instructionDate,
			@Nullable Map<String, String> params) {
		BasicInstruction instr = new BasicInstruction(net.solarnetwork.domain.Instruction.localId(),
				topic, (instructionDate != null ? instructionDate : Instant.now()),
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
	 * @param params
	 *        an optional map of parameters
	 * @return the new instruction, never {@code null}
	 */
	public static Instruction createLocalInstruction(String topic,
			@Nullable Map<String, String> params) {
		return createLocalInstruction(topic, null, params);
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
	 * @return the new instruction, never {@code null}
	 */
	public static Instruction createLocalInstruction(String topic, @Nullable String paramName,
			@Nullable String paramValue) {
		Map<String, String> params = (paramName != null && paramValue != null
				? Map.of(paramName, paramValue)
				: null);
		return createLocalInstruction(topic, params);
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
	 * @return the new instruction, never {@code null}
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
	 * @return the map, never {@code null}
	 * @since 1.1
	 */
	public static Map<String, Object> createErrorResultParameters(@Nullable String message,
			@Nullable String code) {
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
		return new BasicInstruction(
				nonnull(BasicInstruction.from(instr, Instruction.LOCAL_INSTRUCTION_ID), "Instruction"),
				net.solarnetwork.domain.Instruction.localId(), null);
	}

}

/* ==================================================================
 * SimpleExecuteInstructionsService.java - 1/08/2024 2:23:41 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.runtime;

import static net.solarnetwork.node.reactor.InstructionUtils.createErrorResultParameters;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * {@link InstructionHandler} for executing an instruction that contains a list
 * of other instructions to execute.
 *
 * @author matt
 * @version 1.0
 * @since 3.16
 */
public class SimpleExecuteInstructionsService extends BaseIdentifiable implements InstructionHandler {

	/** The instruction topic for executing a set of instructions. */
	public static final String TOPIC_EXECUTE_INSTRUCTIONS = "ExecuteInstructions";

	/** The instruction parameter for a {@link SuccessMode}. */
	public static final String PARAM_SUCCESS_MODE = "successMode";

	/**
	 * The instruction parameter for a boolean "fail fast" flag.
	 *
	 * <p>
	 * When {@literal true} then instruction processing should stop immediately
	 * after any nested instruction fails to complete successfully. When
	 * {@literal false} (the default) then all instructions should be executed,
	 * regardless of any individual failures.
	 * </p>
	 *
	 * <p>
	 * If the {@code successMode} is {@code Some} this parameter is ignored.
	 * </p>
	 */
	public static final String PARAM_FAIL_FAST = "failFast";

	/**
	 * The instruction parameter for a JSON array of {@code Instruction}
	 * objects.
	 */
	public static final String PARAM_INSTRUCTIONS = "instructions";

	/**
	 * The instruction result parameter for a JSON array of
	 * {@code InstructionStatus} objects.
	 */
	public static final String RESULT_PARAM_STATUSES = "statuses";

	/**
	 * A "success mode" to determine now to treat the overall success of the
	 * {@code ExecuteInstructions} instruction in relation to the success of its
	 * embedded instructions.
	 *
	 * <p>
	 * The default mode is {@code Every}. An instruction is considered
	 * "successful" if it is handled and returns a status in either
	 * {@link InstructionState#Completed} or {@link InstructionState#Executing}
	 * state.
	 * </p>
	 *
	 * @author matt
	 * @version 1.0
	 */
	public static enum SuccessMode {

		/** Every instruction completes successfully. */
		Every,

		/** At least one instruction completes successfully. */
		Some,

		;

		/**
		 * Get an enumeration value for a parameter value.
		 *
		 * @param val
		 *        the parameter value to get the enumeration value for
		 * @param defaultValue
		 *        a fallback value to use of {@code val} is not a valid
		 *        enumeration value
		 * @return the resolved enumeration value
		 */
		public static SuccessMode forParameterValue(String val, SuccessMode defaultValue) {
			for ( SuccessMode mode : values() ) {
				if ( mode.name().equalsIgnoreCase(val) ) {
					return mode;
				}
			}
			return defaultValue;
		}
	}

	private final ObjectMapper objectMapper;
	private final OptionalService<InstructionExecutionService> instructionExecutionService;

	/**
	 * Constructor.
	 *
	 * @param objectMapper
	 *        the object mapper to use
	 * @param instructionExecutionService
	 *        the service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleExecuteInstructionsService(final ObjectMapper objectMapper,
			final OptionalService<InstructionExecutionService> instructionExecutionService) {
		super();
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.instructionExecutionService = requireNonNullArgument(instructionExecutionService,
				"instructionExecutionService");
	}

	@Override
	public boolean handlesTopic(String topic) {
		return TOPIC_EXECUTE_INSTRUCTIONS.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(TOPIC_EXECUTE_INSTRUCTIONS) ) {
			return null;
		}

		final InstructionExecutionService service = service(instructionExecutionService);
		if ( service == null ) {
			return createStatus(instruction, InstructionState.Declined, createErrorResultParameters(
					"InstructionExecutionService not available.", "EIS.0001"));
		}

		final boolean failFast = StringUtils
				.parseBoolean(instruction.getParameterValue(PARAM_FAIL_FAST));
		final SuccessMode successMode = SuccessMode
				.forParameterValue(instruction.getParameterValue(PARAM_SUCCESS_MODE), SuccessMode.Every);
		final net.solarnetwork.domain.Instruction[] instructions;
		try {
			instructions = parseInstructions(instruction.getParameterValue(PARAM_INSTRUCTIONS));
		} catch ( IOException e ) {
			return createStatus(instruction, InstructionState.Declined, createErrorResultParameters(
					"Failed to parse instructions parameter as JSON array of Instruction objects: "
							+ e.getMessage(),
					"EIS.0002"));
		}
		final int instructionCount = (instructions != null ? instructions.length : 0);
		if ( instructionCount < 1 ) {
			return createStatus(instruction, InstructionState.Declined, createErrorResultParameters(
					"No instructions given on instructions parameter.", "EIS.0003"));
		}
		final List<InstructionStatus> results = new ArrayList<>(instructionCount);
		boolean anySuccess = false;
		boolean anyFailure = false;
		for ( int i = 0; i < instructionCount; i++ ) {
			final net.solarnetwork.domain.Instruction instr = instructions[i];
			if ( instr == null ) {
				// skip a "null" instruction
				results.add(null);
				continue;
			}
			final BasicInstruction nodeInstr = BasicInstruction.from(instr,
					instruction.getInstructorId());
			InstructionStatus s = service.executeInstruction(nodeInstr);
			if ( s == null ) {
				// not handled: failure
				s = createStatus(nodeInstr, InstructionState.Declined,
						createErrorResultParameters("Instruction not handled.", "EIS.0004"));
			}
			results.add(s);
			final boolean success = (s.getInstructionState() == InstructionState.Completed
					|| s.getInstructionState() == InstructionState.Executing);
			if ( success ) {
				anySuccess = true;
			} else {
				anyFailure = true;
				if ( failFast && successMode == SuccessMode.Every ) {
					break;
				}
			}
		}
		final boolean overallSuccess = (anyFailure ? successMode == SuccessMode.Some && anySuccess
				: true);
		return createStatus(instruction,
				(overallSuccess ? InstructionState.Completed : InstructionState.Declined),
				Collections.singletonMap(RESULT_PARAM_STATUSES, results));
	}

	private net.solarnetwork.domain.Instruction[] parseInstructions(String parameterValue)
			throws IOException {
		if ( parameterValue == null || parameterValue.isEmpty() ) {
			return null;
		}
		return objectMapper.readValue(parameterValue, net.solarnetwork.domain.Instruction[].class);
	}

}

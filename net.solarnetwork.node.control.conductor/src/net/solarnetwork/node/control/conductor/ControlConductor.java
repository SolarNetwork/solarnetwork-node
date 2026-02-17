/* ==================================================================
 * ControlConductor.java - 4/04/2023 6:29:17 am
 *
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.conductor;

import static java.lang.String.format;
import static net.solarnetwork.node.reactor.InstructionUtils.createErrorResultParameters;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.service.PlaceholderService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.support.ExpressionServiceExpression;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Control that orchestrates a set of control tasks.
 *
 * @author matt
 * @version 1.1
 */
public class ControlConductor extends BaseIdentifiable
		implements SettingSpecifierProvider, InstructionHandler {

	/** The instruction topic to orchestrate a set of controls. */
	public static final String TOPIC_ORCHESTRATE_CONTROLS = "OrchestrateControls";

	/**
	 * The instruction parameter for the date to orchestrate the controls at.
	 *
	 * <p>
	 * The format of this parameter is the same as
	 * {@link Instruction#PARAM_EXECUTION_DATE}.
	 * </p>
	 */
	public static final String PARAM_ORCHESTRATE_DATE = "date";

	private final OptionalService<ReactorService> reactorService;
	private final OptionalService<InstructionExecutionService> instructionService;
	private OptionalService<DatumService> datumService;
	private OptionalService<OperationalModesService> opModesService;
	private ControlTaskConfig[] taskConfigs;

	/**
	 * Constructor.
	 *
	 * @param reactorService
	 *        the reactor service
	 * @param instructionService
	 *        the instruction execution service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ControlConductor(OptionalService<ReactorService> reactorService,
			OptionalService<InstructionExecutionService> instructionService) {
		super();
		this.reactorService = requireNonNullArgument(reactorService, "reactorService");
		this.instructionService = requireNonNullArgument(instructionService, "instructionService");
	}

	@Override
	public boolean handlesTopic(String topic) {
		return TOPIC_ORCHESTRATE_CONTROLS.equals(topic) || TOPIC_SIGNAL.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null ) {
			return null;
		}
		if ( !handlesTopic(instruction.getTopic()) ) {
			return null;
		}

		if ( TOPIC_ORCHESTRATE_CONTROLS.equals(instruction.getTopic()) ) {
			return handleOrchestrateControlsInstruction(instruction);
		}

		return handleSignalInstruction(instruction);

	}

	private InstructionStatus handleOrchestrateControlsInstruction(Instruction instruction) {
		// the "service" parameter must much our UID
		String service = instruction.getParameterValue(PARAM_SERVICE);
		if ( service == null || !service.equalsIgnoreCase(getUid()) ) {
			return null;
		}

		if ( taskConfigs == null || taskConfigs.length < 1 ) {
			return createStatus(instruction, InstructionState.Declined,
					createErrorResultParameters("No tasks configured.", "CC.0001"));
		}

		// schedule a Signal instruction for each task at the appropriate execution time
		// based on the orchestration date + task offset

		final List<Instruction> taskInstructions = new ArrayList<>(taskConfigs.length);
		final Instant start = orchestrateDate(instruction);
		final PlaceholderService placeholderService = service(getPlaceholderService());
		final Map<String, String> instructionParams = instruction.getParameterMap();

		int taskIndex = 0;
		for ( ControlTaskConfig task : taskConfigs ) {
			taskIndex += 1;
			if ( !task.isValid() ) {
				return createStatus(instruction, InstructionState.Declined, createErrorResultParameters(
						format("Task %d does not have a valid configuration.", taskIndex), "CC.0002"));
			}
			Instant taskDate = task.executionTime(start, placeholderService, instructionParams);
			if ( taskDate == null ) {
				return createStatus(instruction, InstructionState.Declined,
						createErrorResultParameters(
								format("Task %d does not have a valid execution offset.", taskIndex),
								"CC.0003"));
			}

			Map<String, String> taskInstructionParams = new HashMap<>(8);
			taskInstructionParams.putAll(instructionParams);
			taskInstructionParams.put(Instruction.PARAM_PARENT_INSTRUCTION_ID,
					instruction.getIdentifier());
			taskInstructionParams.put(Instruction.PARAM_PARENT_INSTRUCTOR_ID,
					instruction.getInstructorId());
			taskInstructionParams.put(Instruction.PARAM_EXECUTION_DATE,
					String.valueOf(taskDate.toEpochMilli()));
			taskInstructionParams.put(getUid(), String.valueOf(taskIndex));

			Instruction taskInstr = InstructionUtils.createLocalInstruction(TOPIC_SIGNAL,
					taskInstructionParams);
			taskInstructions.add(taskInstr);
		}

		final ReactorService rs = OptionalService.service(reactorService);
		if ( rs == null ) {
			return createStatus(instruction, InstructionState.Declined, createErrorResultParameters(
					"No ReactorService avaialble to schedule the control tasks.", "CC.0004"));
		}

		taskIndex = 0;
		for ( Instruction taskInstruction : taskInstructions ) {
			taskIndex += 1;
			log.info("Scheduling {} instruction on behalf of {} instruction [{}] task [{}.{}] @ {}",
					TOPIC_SIGNAL, instruction.getTopic(), instruction.getIdentifier(), getUid(),
					taskIndex, taskInstruction.getExecutionDate());

			rs.storeInstruction(taskInstruction);
		}

		return InstructionUtils.createStatus(instruction, InstructionState.Completed,
				createErrorResultParameters(format("Scheduled %d control tasks.", taskIndex), null));
	}

	private InstructionStatus handleSignalInstruction(Instruction instruction) {
		final String taskId = instruction.getParameterValue(getUid());
		final String taskIdentifier = format("%s.%s", getUid(), taskId);

		int taskIndex;
		try {
			taskIndex = Integer.parseInt(taskId);
		} catch ( NumberFormatException e ) {
			return createStatus(instruction, InstructionState.Declined, createErrorResultParameters(
					format("Invalid task ID [%s]", taskIdentifier), "CC.0005"));
		}
		ControlTaskConfig taskConfig;
		try {
			taskConfig = taskConfigs[taskIndex - 1];
		} catch ( ArrayIndexOutOfBoundsException e ) {
			return createStatus(instruction, InstructionState.Declined,
					createErrorResultParameters(
							format("Task configuration not available for task ID [%s]", taskIdentifier),
							"CC.0006"));
		}

		Map<String, String> instrParams = instruction.getParameterMap();
		String controlId = resolvePlaceholders(taskConfig.getControlId(), instrParams);
		if ( controlId == null ) {
			return createStatus(instruction, InstructionState.Declined, createErrorResultParameters(
					format("Task configuration [%s] does not provide control ID.", taskIdentifier),
					"CC.0007"));

		}

		Object controlValue = resolvePlaceholders(taskConfig.getValue(), instrParams);
		if ( controlValue != null ) {
			ExpressionServiceExpression expr = taskConfig
					.valueExpression(services(getExpressionServices()));
			if ( expr != null ) {
				ExpressionRoot root = new ExpressionRoot(null, null, instrParams, service(datumService),
						service(opModesService));
				root.setLocalStateDao(getLocalStateDao());
				controlValue = expr.getService().evaluateExpression(expr.getExpression(), null, root,
						null, Object.class);
			}
		}
		if ( controlValue == null ) {
			return createStatus(instruction, InstructionState.Declined, createErrorResultParameters(
					format("Task configuration [%s] does not provide control value.", taskIdentifier),
					"CC.0008"));
		}

		final InstructionExecutionService ies = OptionalService.service(instructionService);
		if ( ies == null ) {
			return createStatus(instruction, InstructionState.Declined,
					createErrorResultParameters(
							format("No InstructionExecutionService avaialble to execute task [%s].",
									taskIdentifier),
							"CC.0009"));
		}

		log.info("Executing {} instruction for orchestrated task [{}] to set control [{}] to [{}]",
				TOPIC_SET_CONTROL_PARAMETER, taskIdentifier, controlId, controlValue);

		Instruction setControlValueInstr = InstructionUtils
				.createSetControlValueLocalInstruction(controlId, controlValue);
		InstructionStatus status = ies.executeInstruction(setControlValueInstr);
		if ( status == null ) {
			return createStatus(instruction, InstructionState.Declined,
					createErrorResultParameters(
							format("No handler available to process %s instruction for task [%s].",
									TOPIC_SET_CONTROL_PARAMETER, taskIdentifier),
							"CC.0010"));
		}
		return InstructionUtils.createStatus(instruction, status.getInstructionState(),
				status.getResultParameters());
	}

	private Instant orchestrateDate(Instruction instruction) {
		Instant start = instruction.timestampParameterValue(PARAM_ORCHESTRATE_DATE);
		if ( start == null ) {
			start = instruction.getInstructionDate();
			if ( start == null ) {
				start = Instant.now();
			}
		}
		return start;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.conductor";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		result.addAll(baseIdentifiableSettings(""));

		ControlTaskConfig[] confs = getTaskConfigs();
		List<ControlTaskConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.emptyList());
		result.add(SettingUtils.dynamicListSettingSpecifier("taskConfigs", confsList,
				new SettingUtils.KeyedListCallback<ControlTaskConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(ControlTaskConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								ControlTaskConfig.settings(key + ".",
										services(getExpressionServices())));
						return Collections.singletonList(configGroup);
					}
				}));

		return result;
	}

	/**
	 * Get the task configurations.
	 *
	 * @return the configurations
	 */
	public ControlTaskConfig[] getTaskConfigs() {
		return taskConfigs;
	}

	/**
	 * Set the task configurations.
	 *
	 * @param taskConfigs
	 *        the configurations to set
	 */
	public void setTaskConfigs(ControlTaskConfig[] taskConfigs) {
		this.taskConfigs = taskConfigs;
	}

	/**
	 * Get the number of configured {@code taskConfigs} elements.
	 *
	 * @return the number of {@code taskConfigs} elements
	 */
	public int getTaskConfigsCount() {
		ControlTaskConfig[] confs = this.taskConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code taskConfigs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link ControlTaskConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code taskConfigs} elements.
	 */
	public void setTaskConfigsCount(int count) {
		this.taskConfigs = ArrayUtils.arrayWithLength(this.taskConfigs, count, ControlTaskConfig.class,
				null);
	}

	/**
	 * Get the datum service.
	 *
	 * @return the datum service
	 */
	public OptionalService<DatumService> getDatumService() {
		return datumService;
	}

	/**
	 * Set the datum service.
	 *
	 * @param datumService
	 *        the datum service
	 */
	public void setDatumService(OptionalService<DatumService> datumService) {
		this.datumService = datumService;
	}

	/**
	 * Get the operational modes service.
	 *
	 * @return the opModesService
	 */
	public OptionalService<OperationalModesService> getOpModesService() {
		return opModesService;
	}

	/**
	 * Set the operational modes service.
	 *
	 * @param opModesService
	 *        the opModesService to set
	 */
	public void setOpModesService(OptionalService<OperationalModesService> opModesService) {
		this.opModesService = opModesService;
	}

}

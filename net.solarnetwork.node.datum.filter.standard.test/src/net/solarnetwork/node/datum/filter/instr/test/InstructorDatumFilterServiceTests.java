/* ==================================================================
 * InstructorDatumFilterServiceTests.java - 10/07/2026 7:37:28 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.instr.test;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Received;
import static net.solarnetwork.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import java.time.Instant;
import java.time.InstantSource;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.InstructionStatus;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.dao.LocalStateDao;
import net.solarnetwork.node.datum.filter.instr.InstructionConfig;
import net.solarnetwork.node.datum.filter.instr.InstructorConfig;
import net.solarnetwork.node.datum.filter.instr.InstructorDatumFilterService;
import net.solarnetwork.node.domain.LocalState;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.service.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link InstructorDatumFilterService} class.
 *
 * @author matt
 * @version 1.0
 */
public class InstructorDatumFilterServiceTests {

	private static final String INPUT_SIGNAL_PROP_NAME = "presentSignal";
	private static final String SIGNAL_SERVICE_UID = "Signal Handler " + randomString();
	private static final String PRESENT_SIGNAL_INSTRUCTION_ID_LOCAL_STATE_KEY = "present-signal-instruction-id";
	private static final String INSTRUCTION_STATUS_RESULT_PARAM_NAME = "result";
	private static final String RESULT_PROP_NAME = "instructionResult";

	private InstantSource clock;
	private ReactorService reactorService;
	private InstructionExecutionService instructionExecutionService;
	private LocalStateDao localStateDao;
	private ExpressionService exprService;
	private InstructorDatumFilterService xform;

	@Before
	public void setup() {
		clock = EasyMock.createMock(InstantSource.class);
		reactorService = EasyMock.createMock(ReactorService.class);
		instructionExecutionService = EasyMock.createMock(InstructionExecutionService.class);
		localStateDao = EasyMock.createMock(LocalStateDao.class);
		xform = new InstructorDatumFilterService(clock, new StaticOptionalService<>(reactorService),
				new StaticOptionalService<>(instructionExecutionService));
		xform.setUid("Test");
		exprService = new SpelExpressionService();
		xform.setExpressionServices(new StaticOptionalServiceCollection<>(List.of(exprService)));
		xform.setLocalStateDao(new StaticOptionalService<>(localStateDao));
	}

	@After
	public void teardown() {
		EasyMock.verify(clock, reactorService, instructionExecutionService, localStateDao);
	}

	private void replayAll() {
		EasyMock.replay(clock, reactorService, instructionExecutionService, localStateDao);
	}

	private SimpleDatum createTestSimpleDatum(String sourceId, String prop, Number val) {
		SimpleDatum datum = SimpleDatum.nodeDatum(sourceId);
		datum.getSamples().putInstantaneousSampleValue(prop, val);
		return datum;
	}

	@Test
	public void generateInstruction_saveInstructionIdToLocalState() {
		// GIVEN
		final InstructorConfig config = new InstructorConfig();
		config.getPredicate().setExpression("""
				%s == 1
				""".formatted(INPUT_SIGNAL_PROP_NAME));
		config.getPredicate().setExpressionServiceId(exprService.getUid());

		final InstructionConfig instrConfig = new InstructionConfig();
		instrConfig.setTopic(InstructionHandler.TOPIC_SIGNAL);

		final ExpressionConfig signalParamConfig = new ExpressionConfig();
		signalParamConfig.setName(InstructionHandler.PARAM_SERVICE);
		signalParamConfig.setExpression(SIGNAL_SERVICE_UID);

		final ExpressionConfig signalInputParamConfig = new ExpressionConfig();
		signalInputParamConfig.setName(InstructionHandler.PARAM_SERVICE_ARGUMENT);
		signalInputParamConfig.setExpression("""
				%s
				""".formatted(INPUT_SIGNAL_PROP_NAME));
		signalInputParamConfig.setExpressionServiceId(exprService.getUid());

		instrConfig.setParameters(new ExpressionConfig[] { signalParamConfig, signalInputParamConfig });

		final ExpressionConfig saveInstructionIdResponseConfig = new ExpressionConfig();
		saveInstructionIdResponseConfig.setExpression("""
				saveLocalState("%s", instruction.id)
				""".formatted(PRESENT_SIGNAL_INSTRUCTION_ID_LOCAL_STATE_KEY));
		saveInstructionIdResponseConfig.setExpressionServiceId(exprService.getUid());
		instrConfig.setResponses(new ExpressionConfig[] { saveInstructionIdResponseConfig });

		config.setInstructions(new InstructionConfig[] { instrConfig });

		xform.setInstructorConfigs(new InstructorConfig[] { config });

		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		expect(clock.instant()).andReturn(now);

		final Capture<Instruction> instructionCaptor = Capture.newInstance(CaptureType.ALL);
		// persist instruction
		reactorService.storeInstruction(capture(instructionCaptor));

		// execute instruction
		expect(instructionExecutionService.executeInstruction(capture(instructionCaptor)))
				.andAnswer(() -> {
					final Instruction instr = (Instruction) EasyMock.getCurrentArguments()[0];
					return InstructionUtils.createStatus(instr, Completed);
				});

		// persist state
		reactorService.storeInstruction(capture(instructionCaptor));

		final Capture<LocalState> localStateCaptor = Capture.newInstance();
		expect(localStateDao.compareAndChange(capture(localStateCaptor))).andAnswer(() -> {
			return (LocalState) EasyMock.getCurrentArguments()[0];
		});

		// WHEN
		replayAll();
		final SimpleDatum d = createTestSimpleDatum(randomString(), INPUT_SIGNAL_PROP_NAME, 1);
		final Map<String, Object> parameters = new LinkedHashMap<>();
		final DatumSamplesOperations result = xform.filter(d, d.getSamples(), parameters);

		// THEN
		// @formatter:off
		then(result)
			.as("Result provided")
			.isNotNull()
			;

		final List<Instruction> instructions = instructionCaptor.getValues();
		then(instructions)
			.as("Instruction persisted, executed, and updated")
			.hasSize(3)
			.allSatisfy(instruction -> {
				then(instruction)
					.as("Instruction topic from config")
					.returns(instrConfig.getTopic(), from(Instruction::getTopic))
					.extracting(Instruction::getParameterMap, map(String.class, String.class))
					.containsExactlyInAnyOrderEntriesOf(Map.of(
						InstructionHandler.PARAM_SERVICE, SIGNAL_SERVICE_UID,
						InstructionHandler.PARAM_SERVICE_ARGUMENT, "1"
					))
					;
			})
			.satisfies(list -> {
				final Instruction firstInstruction = list.get(0);
				then(list)
					.asInstanceOf(list(Instruction.class))
					.as("The same instruction is persisted/executed/updated")
					.allSatisfy(instruction -> {
						then(instruction)
							.as("Instructor ID same as first instruction")
							.returns(firstInstruction.getInstructorId(), from(Instruction::getInstructorId))
							.as("Instruction ID same as first instruction")
							.returns(firstInstruction.getId(), from(Instruction::getId))
							;
					})
					;

				then(list).element(0)
					.as("Persisted instruction has no status")
					.returns(null, from(Instruction::getStatus))
					;
				then(list).element(2)
					.extracting(Instruction::getStatus)
					.as("Updated instruction has status")
					.isNotNull()
					.returns(Completed, from(InstructionStatus::getInstructionState))
					;
			})
			;

		then(localStateCaptor.getValue())
			.as("LocalState for instruction ID saved")
			.isNotNull()
			.as("LocalState key from expression")
			.returns(PRESENT_SIGNAL_INSTRUCTION_ID_LOCAL_STATE_KEY, from(LocalState::getKey))
			.as("LocalState value is instruction ID")
			.returns(instructions.get(0).getId(), from(LocalState::getValue))
			;
		// @formatter:on
	}

	@Test
	public void generateInstruction_saveResultAsDatumProp() {
		// GIVEN
		final InstructorConfig config = new InstructorConfig();
		config.getPredicate().setExpression("""
				%s == 1
				""".formatted(INPUT_SIGNAL_PROP_NAME));
		config.getPredicate().setExpressionServiceId(exprService.getUid());

		final InstructionConfig instrConfig = new InstructionConfig();
		instrConfig.setTopic(InstructionHandler.TOPIC_SIGNAL);

		final ExpressionConfig signalParamConfig = new ExpressionConfig();
		signalParamConfig.setName(InstructionHandler.PARAM_SERVICE);
		signalParamConfig.setExpression(SIGNAL_SERVICE_UID);

		instrConfig.setParameters(new ExpressionConfig[] { signalParamConfig });

		final ExpressionConfig resultResponseConfig = new ExpressionConfig();
		resultResponseConfig.setPropertyKey(RESULT_PROP_NAME);
		resultResponseConfig.setPropertyType(DatumSamplesType.Status);
		resultResponseConfig.setExpression("""
				instructionResult != null && instructionResult.completed
				? instructionResult.resultParameters["%s"]
				: null
				""".formatted(INSTRUCTION_STATUS_RESULT_PARAM_NAME));
		resultResponseConfig.setExpressionServiceId(exprService.getUid());
		instrConfig.setResponses(new ExpressionConfig[] { resultResponseConfig });

		config.setInstructions(new InstructionConfig[] { instrConfig });

		xform.setInstructorConfigs(new InstructorConfig[] { config });

		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		expect(clock.instant()).andReturn(now);

		final String instructionStatusResult = randomString();
		final Capture<Instruction> instructionCaptor = Capture.newInstance(CaptureType.ALL);
		// persist instruction
		reactorService.storeInstruction(capture(instructionCaptor));

		// execute instruction
		expect(instructionExecutionService.executeInstruction(capture(instructionCaptor)))
				.andAnswer(() -> {
					final Instruction instr = (Instruction) EasyMock.getCurrentArguments()[0];
					return InstructionUtils.createStatus(instr, Completed,
							Map.of(INSTRUCTION_STATUS_RESULT_PARAM_NAME, instructionStatusResult));
				});

		// persist state
		reactorService.storeInstruction(capture(instructionCaptor));

		// WHEN
		replayAll();
		final SimpleDatum d = createTestSimpleDatum(randomString(), INPUT_SIGNAL_PROP_NAME, 1);
		final Map<String, Object> parameters = new LinkedHashMap<>();
		final DatumSamplesOperations result = xform.filter(d, d.getSamples(), parameters);

		// THEN
		// @formatter:off
		then(result)
			.as("Result provided")
			.isNotNull()
			.as("New samples instance returned")
			.isNotSameAs(d.getSamples())
			.as("DatumSamples instance returned")
			.isInstanceOf(DatumSamples.class)
			.asInstanceOf(type(DatumSamples.class))
			.satisfies(s -> {
				then(s.getInstantaneous())
					.as("Given instantaneous datum properties remain")
					.containsExactlyInAnyOrderEntriesOf(d.getSamples().getInstantaneous())
					;
				then(s.getStatus())
					.as("Response expression result saved to status datum property")
					.containsExactlyInAnyOrderEntriesOf(Map.of(
						RESULT_PROP_NAME, instructionStatusResult
					))
					;
			})
			;

		final List<Instruction> instructions = instructionCaptor.getValues();
		then(instructions)
		.as("Instruction persisted, executed, and updated")
		.hasSize(3)
		.allSatisfy(instruction -> {
			then(instruction)
				.as("Instruction topic from config")
				.returns(instrConfig.getTopic(), from(Instruction::getTopic))
				.extracting(Instruction::getParameterMap, map(String.class, String.class))
				.containsExactlyInAnyOrderEntriesOf(Map.of(
					InstructionHandler.PARAM_SERVICE, SIGNAL_SERVICE_UID
				))
				;
		})
		.satisfies(list -> {
			final Instruction firstInstruction = list.get(0);
			then(list)
				.asInstanceOf(list(Instruction.class))
				.as("The same instruction is persisted/executed/updated")
				.allSatisfy(instruction -> {
					then(instruction)
						.as("Instructor ID same as first instruction")
						.returns(firstInstruction.getInstructorId(), from(Instruction::getInstructorId))
						.as("Instruction ID same as first instruction")
						.returns(firstInstruction.getId(), from(Instruction::getId))
						;
				})
				;

			then(list).element(0)
				.as("Persisted instruction has no status")
				.returns(null, from(Instruction::getStatus))
				;
			then(list).element(2)
				.extracting(Instruction::getStatus)
				.as("Updated instruction has status")
				.isNotNull()
				.returns(Completed, from(InstructionStatus::getInstructionState))
				;
		})
		;
		// @formatter:on
	}

	@Test
	public void generateDeferredInstruction() {
		// GIVEN
		final InstructorConfig config = new InstructorConfig();
		config.getPredicate().setExpression("""
				%s == 1
				""".formatted(INPUT_SIGNAL_PROP_NAME));
		config.getPredicate().setExpressionServiceId(exprService.getUid());

		final Instant execAt = Instant.now().truncatedTo(SECONDS).plus(1, HOURS);

		final InstructionConfig instrConfig = new InstructionConfig();
		instrConfig.setTopic(InstructionHandler.TOPIC_SIGNAL);

		final ExpressionConfig signalParamConfig = new ExpressionConfig();
		signalParamConfig.setName(InstructionHandler.PARAM_SERVICE);
		signalParamConfig.setExpression(SIGNAL_SERVICE_UID);

		// add executeDate parameter
		final ExpressionConfig execAtParamConfig = new ExpressionConfig();
		execAtParamConfig.setName(Instruction.PARAM_EXECUTION_DATE);
		execAtParamConfig.setExpression(execAt.toString());

		instrConfig.setParameters(new ExpressionConfig[] { signalParamConfig, execAtParamConfig });

		config.setInstructions(new InstructionConfig[] { instrConfig });

		xform.setInstructorConfigs(new InstructorConfig[] { config });

		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		expect(clock.instant()).andReturn(now);

		final Capture<Instruction> instructionCaptor = Capture.newInstance(CaptureType.ALL);
		// persist deferred instruction
		expect(reactorService.processInstruction(capture(instructionCaptor))).andAnswer(() -> {
			Instruction instr = instructionCaptor.getValue();
			return InstructionUtils.createStatus(instr, Received);
		});

		// WHEN
		replayAll();
		final SimpleDatum d = createTestSimpleDatum(randomString(), INPUT_SIGNAL_PROP_NAME, 1);
		final Map<String, Object> parameters = new LinkedHashMap<>();
		final DatumSamplesOperations result = xform.filter(d, d.getSamples(), parameters);

		// THEN
		// @formatter:off
		then(result)
			.as("Result provided")
			.isNotNull()
			.as("Same samples instance returned")
			.isSameAs(d.getSamples())
			;

		final List<Instruction> instructions = instructionCaptor.getValues();
		then(instructions)
			.as("Instruction persisted (as Received with deferred date)")
			.hasSize(1)
			.element(0)
			.as("Instruction topic from config")
			.returns(instrConfig.getTopic(), from(Instruction::getTopic))
			.satisfies(instr -> {
				then(instr.getParameterMap())
					.containsExactlyInAnyOrderEntriesOf(Map.of(
						InstructionHandler.PARAM_SERVICE, SIGNAL_SERVICE_UID,
						Instruction.PARAM_EXECUTION_DATE, execAt.toString()
					))
					;
			})
			.extracting(Instruction::getStatus)
			.as("Persisted instruction had no status")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void generateDeferredInstruction_saveInstructionIdAsDatumProperty() {
		// GIVEN
		final InstructorConfig config = new InstructorConfig();
		config.getPredicate().setExpression("""
				%s == 1
				""".formatted(INPUT_SIGNAL_PROP_NAME));
		config.getPredicate().setExpressionServiceId(exprService.getUid());

		final Instant execAt = Instant.now().truncatedTo(SECONDS).plus(1, HOURS);

		final InstructionConfig instrConfig = new InstructionConfig();
		instrConfig.setTopic(InstructionHandler.TOPIC_SIGNAL);

		final ExpressionConfig signalParamConfig = new ExpressionConfig();
		signalParamConfig.setName(InstructionHandler.PARAM_SERVICE);
		signalParamConfig.setExpression(SIGNAL_SERVICE_UID);

		// add executeDate parameter
		final ExpressionConfig execAtParamConfig = new ExpressionConfig();
		execAtParamConfig.setName(Instruction.PARAM_EXECUTION_DATE);
		execAtParamConfig.setExpression(execAt.toString());

		instrConfig.setParameters(new ExpressionConfig[] { signalParamConfig, execAtParamConfig });

		final ExpressionConfig resultResponseConfig = new ExpressionConfig();
		resultResponseConfig.setPropertyKey(RESULT_PROP_NAME);
		resultResponseConfig.setPropertyType(DatumSamplesType.Status);
		resultResponseConfig.setExpression("instruction.id");
		resultResponseConfig.setExpressionServiceId(exprService.getUid());
		instrConfig.setResponses(new ExpressionConfig[] { resultResponseConfig });

		config.setInstructions(new InstructionConfig[] { instrConfig });

		xform.setInstructorConfigs(new InstructorConfig[] { config });

		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		expect(clock.instant()).andReturn(now);

		final AtomicReference<net.solarnetwork.node.reactor.InstructionStatus> statusRef = new AtomicReference<>();
		final Capture<Instruction> instructionCaptor = Capture.newInstance(CaptureType.ALL);
		// persist deferred instruction
		expect(reactorService.processInstruction(capture(instructionCaptor))).andAnswer(() -> {
			Instruction instr = instructionCaptor.getValue();
			var status = InstructionUtils.createStatus(instr, Received);
			statusRef.set(status);
			return status;
		});

		// WHEN
		replayAll();
		final SimpleDatum d = createTestSimpleDatum(randomString(), INPUT_SIGNAL_PROP_NAME, 1);
		final Map<String, Object> parameters = new LinkedHashMap<>();
		final DatumSamplesOperations result = xform.filter(d, d.getSamples(), parameters);

		// THEN
		// @formatter:off
		final List<Instruction> instructions = instructionCaptor.getValues();
		then(instructions)
			.as("Instruction persisted (as Received with deferred date)")
			.hasSize(1)
			.element(0)
			.as("Instruction topic from config")
			.returns(instrConfig.getTopic(), from(Instruction::getTopic))
			.satisfies(instr -> {
				then(instr.getParameterMap())
					.containsExactlyInAnyOrderEntriesOf(Map.of(
						InstructionHandler.PARAM_SERVICE, SIGNAL_SERVICE_UID,
						Instruction.PARAM_EXECUTION_DATE, execAt.toString()
					))
					;
			})
			.extracting(Instruction::getStatus)
			.as("Persisted instruction had no status")
			.isNull()
			;

		final net.solarnetwork.node.reactor.InstructionStatus deferredStatus = statusRef.get();

		then(result)
			.as("Result provided")
			.isNotNull()
			.as("New samples instance returned")
			.isNotSameAs(d.getSamples())
			.as("DatumSamples instance returned")
			.isInstanceOf(DatumSamples.class)
			.asInstanceOf(type(DatumSamples.class))
			.satisfies(s -> {
				then(s.getInstantaneous())
					.as("Given instantaneous datum properties remain")
					.containsExactlyInAnyOrderEntriesOf(d.getSamples().getInstantaneous())
					;
				then(s.getStatus())
					.as("Response expression result saved to status datum property")
					.containsExactlyInAnyOrderEntriesOf(Map.of(
						RESULT_PROP_NAME, deferredStatus.getInstructionId()
					))
					;
			})
			;
		// @formatter:on
	}

}

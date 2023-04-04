/* ==================================================================
 * ControlConductorTests.java - 4/04/2023 2:05:53 pm
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

package net.solarnetwork.node.control.conductor.test;

import static java.lang.String.format;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.node.control.conductor.ControlConductor.TOPIC_ORCHESTRATE_CONTROLS;
import static net.solarnetwork.node.control.conductor.ControlTaskConfig.taskConfig;
import static net.solarnetwork.node.reactor.InstructionUtils.createLocalInstruction;
import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.node.control.conductor.ControlConductor;
import net.solarnetwork.node.control.conductor.ControlTaskConfig;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.PlaceholderService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.service.StaticOptionalServiceCollection;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link ControlConductor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ControlConductorTests {

	private ReactorService reactorService;
	private DatumService datumService;
	private PlaceholderService placeholderService;
	private ControlConductor conductor;

	@Before
	public void setup() {
		reactorService = EasyMock.createMock(ReactorService.class);
		datumService = EasyMock.createMock(DatumService.class);
		placeholderService = EasyMock.createMock(PlaceholderService.class);

		conductor = new ControlConductor(new StaticOptionalService<>(reactorService));
		conductor.setDatumService(new StaticOptionalService<>(datumService));
		conductor.setPlaceholderService(new StaticOptionalService<>(placeholderService));
		conductor.setExpressionServices(spelExpressionServices());
		conductor.setUid(UUID.randomUUID().toString());
	}

	private void replayAll() {
		EasyMock.replay(reactorService, datumService, placeholderService);
	}

	@After
	public void teardown() {
		EasyMock.verify(reactorService, datumService, placeholderService);
	}

	private static OptionalServiceCollection<ExpressionService> spelExpressionServices() {
		return new StaticOptionalServiceCollection<>(
				Collections.singletonList(new SpelExpressionService()));
	}

	@Test
	public void scheduleTasks() {
		// GIVEN
		// @formatter:off
		ControlTaskConfig[] tasks = new ControlTaskConfig[] {
				// task 1: use expression to set mode using negative ISO offset
				taskConfig("/control/1", "-PT2H", "mode == 'a' ? 1 : 2", SpelExpressionService.class.getName()),
				
				// task 2: set parameter mode using ms offset
				taskConfig("/control/1", "0", "{mode}"),
				
				// task 3: set hard-coded mode using parameter offset
				taskConfig("/control/1/", "{duration}", "0"),
		};
		// @formatter:on
		conductor.setTaskConfigs(tasks);

		final Instant execDate = Instant.now().truncatedTo(ChronoUnit.HOURS).plus(4, ChronoUnit.HOURS);
		final String execDateIso = DateTimeFormatter.ISO_INSTANT.format(execDate);
		final Map<String, String> orchestrateParams = new HashMap<>(4);
		orchestrateParams.put(InstructionHandler.PARAM_SERVICE, conductor.getUid());
		orchestrateParams.put(Instruction.PARAM_EXECUTION_DATE, execDateIso);
		orchestrateParams.put("mode", "3");
		orchestrateParams.put("duration", "PT1H");

		final Instruction orchestrate = createLocalInstruction(TOPIC_ORCHESTRATE_CONTROLS,
				orchestrateParams);

		// resolve placeholders in task offset settings
		expect(placeholderService.resolvePlaceholders(eq("-PT2H"), anyObject())).andReturn("-PT2H");
		expect(placeholderService.resolvePlaceholders(eq("0"), anyObject())).andReturn("0");
		expect(placeholderService.resolvePlaceholders(eq("{duration}"),
				assertWith(new Assertion<Map<String, Object>>() {

					@Override
					public void check(Map<String, Object> argument) throws Throwable {
						assertThat("Parameters from instruction", argument,
								is(equalTo(orchestrateParams)));
					}
				}))).andReturn("PT1H");

		// save task instructions
		Capture<Instruction> savedInstructionsCaptor = Capture.newInstance(CaptureType.ALL);
		reactorService.storeInstruction(capture(savedInstructionsCaptor));
		expectLastCall().times(3);

		// WHEN
		replayAll();
		InstructionStatus result = conductor.processInstruction(orchestrate);

		// THEN
		assertThat("Instruction result returned", result, is(notNullValue()));
		assertThat("Instruction state is OK", result.getInstructionState(), is(equalTo(Completed)));

		List<Instruction> savedInstructions = savedInstructionsCaptor.getValues();
		assertThat("Instructions saved for each task", savedInstructions, hasSize(tasks.length));

		int i = 0;
		for ( Instruction instr : savedInstructions ) {
			i += 1;
			assertThat(format("Task %d instruction is Signal", i), instr.getTopic(),
					is(equalTo(InstructionHandler.TOPIC_SIGNAL)));
			assertThat(format("Task %d instruction param parent instructor ID", i),
					instr.getParameterValue(Instruction.PARAM_PARENT_INSTRUCTOR_ID),
					is(equalTo(Instruction.LOCAL_INSTRUCTION_ID)));
			assertThat(format("Task %d instruction param parent instruction ID", i),
					instr.getParameterValue(Instruction.PARAM_PARENT_INSTRUCTION_ID),
					is(equalTo(orchestrate.getId().toString())));
		}

		Instruction signal = savedInstructions.get(0);
		assertThat("Task 1 execution date", signal.getExecutionDate(),
				is(equalTo(execDate.minus(2, ChronoUnit.HOURS))));
		assertThat("Task 1 execution Signal target", signal.getParameterValue(conductor.getUid()),
				is(equalTo("1")));

		signal = savedInstructions.get(1);
		assertThat("Task 2 execution date", signal.getExecutionDate(), is(equalTo(execDate)));
		assertThat("Task 2 execution Signal target", signal.getParameterValue(conductor.getUid()),
				is(equalTo("2")));

		signal = savedInstructions.get(2);
		assertThat("Task 3 execution date", signal.getExecutionDate(),
				is(equalTo(execDate.plus(1, ChronoUnit.HOURS))));
		assertThat("Task 3 execution Signal target", signal.getParameterValue(conductor.getUid()),
				is(equalTo("3")));
	}

	// {mode=[3], duration=[PT1H], service=[0502632b-da9f-4be6-ad8c-c3e9f2dd625c], parentInstructionId=[LOCAL], parentInstructorId=[LOCAL], executionDate=[1680588000000], 0502632b-da9f-4be6-ad8c-c3e9f2dd625c=[1]}

	@Test
	public void handleSignal_valueExpression() {
		// GIVEN
		final String expectedControlValue = UUID.randomUUID().toString();
		final ControlTaskConfig task = taskConfig("/control/1", "-PT2H",
				format("mode == 'a' ? '%s' : '0'", expectedControlValue),
				SpelExpressionService.class.getName());
		final ControlTaskConfig[] tasks = new ControlTaskConfig[] { task };
		conductor.setTaskConfigs(tasks);

		final Instant execDate = Instant.now().truncatedTo(ChronoUnit.HOURS).plus(4, ChronoUnit.HOURS);
		final String execDateIso = DateTimeFormatter.ISO_INSTANT.format(execDate);

		final Map<String, String> signalParams = new HashMap<>(4);
		signalParams.put(Instruction.PARAM_EXECUTION_DATE, execDateIso);
		signalParams.put("mode", "a");
		signalParams.put("duration", "PT1H");
		signalParams.put(conductor.getUid(), "1");

		final Instruction signal = createLocalInstruction(InstructionHandler.TOPIC_SIGNAL, signalParams);

		// resolve placeholders in task control ID
		final String resolvedControlId = UUID.randomUUID().toString();
		expect(placeholderService.resolvePlaceholders(eq(task.getControlId()),
				assertWith(new Assertion<Map<String, Object>>() {

					@Override
					public void check(Map<String, Object> argument) throws Throwable {
						assertThat("Parameters from instruction", argument, is(equalTo(signalParams)));
					}
				}))).andReturn(resolvedControlId);

		// resolve placeholders in task value
		expect(placeholderService.resolvePlaceholders(eq(task.getValue()),
				assertWith(new Assertion<Map<String, Object>>() {

					@Override
					public void check(Map<String, Object> argument) throws Throwable {
						assertThat("Parameters from instruction", argument, is(equalTo(signalParams)));
					}
				}))).andReturn(task.getValue());

		// process task execution instruction
		Capture<Instruction> savedInstructionsCaptor = Capture.newInstance();
		expect(reactorService.processInstruction(capture(savedInstructionsCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						return InstructionUtils.createStatus(savedInstructionsCaptor.getValue(),
								Completed);
					}
				});

		// WHEN
		replayAll();
		InstructionStatus result = conductor.processInstruction(signal);

		// THEN
		assertThat("Instruction result returned", result, is(notNullValue()));
		assertThat("Instruction state is OK", result.getInstructionState(), is(equalTo(Completed)));

		Instruction execInstr = savedInstructionsCaptor.getValue();
		assertThat("Task execution instruction is SetControlParameter", execInstr.getTopic(),
				is(equalTo(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER)));
		assertThat("Task execution instruction control ID param",
				execInstr.getParameterValue(resolvedControlId), is(equalTo(expectedControlValue)));
	}

}

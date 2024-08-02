/* ==================================================================
 * SimpleExecuteInstructionsServiceTests.java - 1/08/2024 3:09:00 pm
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

package net.solarnetwork.node.runtime.test;

import static net.solarnetwork.node.runtime.SimpleExecuteInstructionsService.PARAM_INSTRUCTIONS;
import static net.solarnetwork.node.runtime.SimpleExecuteInstructionsService.PARAM_SUCCESS_MODE;
import static net.solarnetwork.node.runtime.SimpleExecuteInstructionsService.RESULT_PARAM_STATUSES;
import static net.solarnetwork.node.runtime.SimpleExecuteInstructionsService.TOPIC_EXECUTE_INSTRUCTIONS;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.runtime.SimpleExecuteInstructionsService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link SimpleExecuteInstructionsService} class.
 *
 * @author matt
 * @version 1.0
 */
public class SimpleExecuteInstructionsServiceTests {

	private InstructionExecutionService instructionExecutionService;
	private SimpleExecuteInstructionsService service;

	@Before
	public void setup() {
		instructionExecutionService = EasyMock.createMock(InstructionExecutionService.class);
		service = new SimpleExecuteInstructionsService(JsonUtils.newDatumObjectMapper(),
				new StaticOptionalService<>(instructionExecutionService));
	}

	private void replayAll() {
		EasyMock.replay(instructionExecutionService);
	}

	@After
	public void teardown() {
		EasyMock.verify(instructionExecutionService);
	}

	private String resource(String name) {
		try {
			return FileCopyUtils.copyToString(
					new InputStreamReader(getClass().getResourceAsStream(name), StandardCharsets.UTF_8));
		} catch ( IOException e ) {
			throw new RuntimeException("Error loading string resource [" + name + "]", e);
		}
	}

	private void assertSetControlParameterInstruction(String desc, Instruction actual,
			String expectedControlId, String expectedControlValue) {
		assertThat(desc + " topic", actual.getTopic(),
				is(equalTo(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER)));
		assertThat(desc + " control parameter", actual.getParameterMap(),
				hasEntry(expectedControlId, expectedControlValue));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<InstructionStatus> resultStatuses(InstructionStatus status) {
		return (List) status.getResultParameters().get(RESULT_PARAM_STATUSES);
	}

	@Test
	public void ok() {
		// GIVEN
		final Capture<Instruction> instructionCaptor = Capture.newInstance(CaptureType.ALL);
		final List<InstructionStatus> returnedStatuses = new ArrayList<>(2);
		expect(instructionExecutionService.executeInstruction(capture(instructionCaptor)))
				.andAnswer(() -> {
					Instruction instr = instructionCaptor.getValues()
							.get(instructionCaptor.getValues().size() - 1);
					InstructionStatus s = InstructionUtils.createStatus(instr,
							InstructionState.Completed);
					returnedStatuses.add(s);
					return s;
				}).times(2);

		Map<String, String> params = new LinkedHashMap<>(4);
		params.put(PARAM_INSTRUCTIONS, resource("exec-instructions-01.json"));
		Instruction exec = InstructionUtils.createLocalInstruction(TOPIC_EXECUTE_INSTRUCTIONS, params);

		// WHEN
		replayAll();
		InstructionStatus result = service.processInstruction(exec);

		// THEN
		assertThat("Two instructions processed", instructionCaptor.getValues(), hasSize(2));

		assertSetControlParameterInstruction("Instruction 1", instructionCaptor.getValues().get(0),
				"/foo/bar", "1");
		assertSetControlParameterInstruction("Instruction 2", instructionCaptor.getValues().get(1),
				"/bim/bam", "2");

		assertThat("Result provided", result, is(notNullValue()));
		assertThat("Result state is Completed because all instructions ended in Completed state",
				result.getInstructionState(), is(equalTo(InstructionState.Completed)));

		assertThat("Result statuses provided", result.getResultParameters(),
				hasEntry(equalTo(RESULT_PARAM_STATUSES), instanceOf(List.class)));
		List<InstructionStatus> resultStatuses = resultStatuses(result);
		assertThat("Two result statuses provided, one for each executed instruction", resultStatuses,
				hasSize(2));
		assertThat("Result status 1 is from InstructionExecutionService", resultStatuses.get(0),
				sameInstance(returnedStatuses.get(0)));
		assertThat("Result status 2 is from InstructionExecutionService", resultStatuses.get(1),
				sameInstance(returnedStatuses.get(1)));
	}

	@Test
	public void ok_partial() {
		// GIVEN
		final Capture<Instruction> instructionCaptor = Capture.newInstance(CaptureType.ALL);
		final List<InstructionStatus> returnedStatuses = new ArrayList<>(2);
		final AtomicInteger count = new AtomicInteger();
		expect(instructionExecutionService.executeInstruction(capture(instructionCaptor)))
				.andAnswer(() -> {
					Instruction instr = instructionCaptor.getValues()
							.get(instructionCaptor.getValues().size() - 1);
					// first one fails, second succeeds
					InstructionStatus s = InstructionUtils.createStatus(instr,
							count.incrementAndGet() == 1 ? InstructionState.Declined
									: InstructionState.Completed);
					returnedStatuses.add(s);
					return s;
				}).times(2);

		Map<String, String> params = new LinkedHashMap<>(4);
		params.put(PARAM_SUCCESS_MODE, SimpleExecuteInstructionsService.SuccessMode.Some.name());
		params.put(PARAM_INSTRUCTIONS, resource("exec-instructions-01.json"));
		Instruction exec = InstructionUtils.createLocalInstruction(TOPIC_EXECUTE_INSTRUCTIONS, params);

		// WHEN
		replayAll();
		InstructionStatus result = service.processInstruction(exec);

		// THEN
		assertThat("Two instructions processed", instructionCaptor.getValues(), hasSize(2));

		assertSetControlParameterInstruction("Instruction 1", instructionCaptor.getValues().get(0),
				"/foo/bar", "1");
		assertSetControlParameterInstruction("Instruction 2", instructionCaptor.getValues().get(1),
				"/bim/bam", "2");

		assertThat("Result provided", result, is(notNullValue()));
		assertThat(
				"Result state in Some mode is Completed because at least one ended in Completed state",
				result.getInstructionState(), is(equalTo(InstructionState.Completed)));

		assertThat("Result statuses provided", result.getResultParameters(),
				hasEntry(equalTo(RESULT_PARAM_STATUSES), instanceOf(List.class)));
		List<InstructionStatus> resultStatuses = resultStatuses(result);
		assertThat("Two result statuses provided, one for each executed instruction", resultStatuses,
				hasSize(2));
		assertThat("Result status 1 is from InstructionExecutionService", resultStatuses.get(0),
				sameInstance(returnedStatuses.get(0)));
		assertThat("Result status 2 is from InstructionExecutionService", resultStatuses.get(1),
				sameInstance(returnedStatuses.get(1)));
	}

	@Test
	public void err_partial() {
		// GIVEN
		final Capture<Instruction> instructionCaptor = Capture.newInstance(CaptureType.ALL);
		final List<InstructionStatus> returnedStatuses = new ArrayList<>(2);
		final AtomicInteger count = new AtomicInteger();
		expect(instructionExecutionService.executeInstruction(capture(instructionCaptor)))
				.andAnswer(() -> {
					Instruction instr = instructionCaptor.getValues()
							.get(instructionCaptor.getValues().size() - 1);
					// first one fails, second succeeds
					InstructionStatus s = InstructionUtils.createStatus(instr,
							count.incrementAndGet() == 1 ? InstructionState.Declined
									: InstructionState.Completed);
					returnedStatuses.add(s);
					return s;
				}).times(2);

		Map<String, String> params = new LinkedHashMap<>(4);
		params.put(PARAM_SUCCESS_MODE, SimpleExecuteInstructionsService.SuccessMode.Every.name());
		params.put(PARAM_INSTRUCTIONS, resource("exec-instructions-01.json"));
		Instruction exec = InstructionUtils.createLocalInstruction(TOPIC_EXECUTE_INSTRUCTIONS, params);

		// WHEN
		replayAll();
		InstructionStatus result = service.processInstruction(exec);

		// THEN
		assertThat("Two instructions processed", instructionCaptor.getValues(), hasSize(2));

		assertSetControlParameterInstruction("Instruction 1", instructionCaptor.getValues().get(0),
				"/foo/bar", "1");
		assertSetControlParameterInstruction("Instruction 2", instructionCaptor.getValues().get(1),
				"/bim/bam", "2");

		assertThat("Result provided", result, is(notNullValue()));
		assertThat("Result state in Every mode is Declined because at least one ended in Declined state",
				result.getInstructionState(), is(equalTo(InstructionState.Declined)));

		assertThat("Result statuses provided", result.getResultParameters(),
				hasEntry(equalTo(RESULT_PARAM_STATUSES), instanceOf(List.class)));
		List<InstructionStatus> resultStatuses = resultStatuses(result);
		assertThat("Two result statuses provided, one for each executed instruction", resultStatuses,
				hasSize(2));
		assertThat("Result status 1 is from InstructionExecutionService", resultStatuses.get(0),
				sameInstance(returnedStatuses.get(0)));
		assertThat("Result status 2 is from InstructionExecutionService", resultStatuses.get(1),
				sameInstance(returnedStatuses.get(1)));
	}

}

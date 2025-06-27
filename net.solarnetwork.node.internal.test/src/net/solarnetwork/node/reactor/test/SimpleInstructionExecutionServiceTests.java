/* ==================================================================
 * SimpleInstructionExecutionServiceTests.java - 8/06/2018 9:55:42 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.reactor.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.reactor.SimpleInstructionExecutionService;

/**
 * Test cases for the {@link SimpleInstructionExecutionService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleInstructionExecutionServiceTests {

	private static final String TEST_TOPIC = "test.topic";

	private static abstract class TestTopicInstructionHandler implements InstructionHandler {

		@Override
		public boolean handlesTopic(String topic) {
			return TEST_TOPIC.equals(topic);
		}
	}

	@Test
	public void nullInstruction() {
		// when
		SimpleInstructionExecutionService service = new SimpleInstructionExecutionService(
				Collections.emptyList());
		InstructionStatus status = service.executeInstruction(null);

		// then
		assertThat("Result status", status, nullValue());
	}

	@Test
	public void noHandlersConfigured() {
		// given
		SimpleInstructionExecutionService service = new SimpleInstructionExecutionService(
				Collections.emptyList());
		final Instant now = Instant.now();
		BasicInstruction instr = new BasicInstruction(1L, TEST_TOPIC, now,
				Instruction.LOCAL_INSTRUCTION_ID, null);

		// when
		InstructionStatus status = service.executeInstruction(instr);

		// then
		assertThat("Result status", status, nullValue());
	}

	@Test
	public void handleNullStatusInstruction() {
		// given
		InstructionHandler handler = new TestTopicInstructionHandler() {

			@Override
			public InstructionStatus processInstruction(Instruction instruction) {
				return InstructionUtils.createStatus(instruction, InstructionState.Completed);
			}

		};
		SimpleInstructionExecutionService service = new SimpleInstructionExecutionService(
				Collections.singletonList(handler));

		final Instant now = Instant.now();
		BasicInstruction instr = new BasicInstruction(1L, TEST_TOPIC, now,
				Instruction.LOCAL_INSTRUCTION_ID, null);

		// when
		InstructionStatus status = service.executeInstruction(instr);

		// then
		assertThat("Result status", status, notNullValue());
		assertThat("State", status.getInstructionState(), equalTo(InstructionState.Completed));
		assertThat("Acknoledged state", status.getAcknowledgedInstructionState(), nullValue());
	}

	@Test(expected = RuntimeException.class)
	public void handlerThrowsException() {
		// given
		InstructionHandler handler = new TestTopicInstructionHandler() {

			@Override
			public InstructionStatus processInstruction(Instruction instruction) {
				throw new UnsupportedOperationException("boo");
			}

		};

		SimpleInstructionExecutionService service = new SimpleInstructionExecutionService(
				Collections.singletonList(handler));

		final Instant now = Instant.now();
		BasicInstruction instr = new BasicInstruction(1L, TEST_TOPIC, now,
				Instruction.LOCAL_INSTRUCTION_ID, null);

		// when
		service.executeInstruction(instr);
	}

	@Test
	public void handlerSkipsAnotherHandlerCompletes() {
		// given
		InstructionHandler handler = new TestTopicInstructionHandler() {

			@Override
			public InstructionStatus processInstruction(Instruction instruction) {
				return null;
			}

		};
		InstructionHandler handler2 = new TestTopicInstructionHandler() {

			@Override
			public InstructionStatus processInstruction(Instruction instruction) {
				return InstructionUtils.createStatus(instruction, InstructionState.Completed);
			}

		};

		SimpleInstructionExecutionService service = new SimpleInstructionExecutionService(
				Arrays.asList(handler, handler2));

		final Instant now = Instant.now();
		BasicInstruction instr = new BasicInstruction(1L, TEST_TOPIC, now,
				Instruction.LOCAL_INSTRUCTION_ID, null);

		// when
		InstructionStatus status = service.executeInstruction(instr);

		// then
		assertThat("Result status", status, notNullValue());
		assertThat("State", status.getInstructionState(), equalTo(InstructionState.Completed));
		assertThat("Acknoledged state", status.getAcknowledgedInstructionState(), nullValue());
	}

}

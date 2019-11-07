/* ==================================================================
 * DefaultInstructionExecutionServiceTests.java - 8/06/2018 9:55:42 AM
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

package net.solarnetwork.node.reactor.support.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.reactor.FeedbackInstructionHandler;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.reactor.support.BasicInstructionStatus;
import net.solarnetwork.node.reactor.support.DefaultInstructionExecutionService;

/**
 * Test cases for the {@link DefaultInstructionExecutionService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultInstructionExecutionServiceTests {

	private static final String TEST_TOPIC = "test.topic";

	private DefaultInstructionExecutionService service;

	private static abstract class TestTopicInstructionHandler implements InstructionHandler {

		@Override
		public boolean handlesTopic(String topic) {
			return TEST_TOPIC.equals(topic);
		}
	}

	private static abstract class TestTopicFeedbackInstructionHandler
			implements FeedbackInstructionHandler {

		@Override
		public InstructionState processInstruction(Instruction instruction) {
			InstructionStatus status = processInstructionWithFeedback(instruction);
			return (status != null ? status.getInstructionState() : null);
		}

		@Override
		public boolean handlesTopic(String topic) {
			return TEST_TOPIC.equals(topic);
		}
	}

	@Before
	public void setup() {
		service = new DefaultInstructionExecutionService();
	}

	@Test
	public void nullInstruction() {
		// when
		InstructionStatus status = service.executeInstruction(null);

		// then
		assertThat("Result status", status, nullValue());
	}

	@Test
	public void noHandlersConfigured() {
		// given
		final Date now = new Date();
		BasicInstruction instr = new BasicInstruction(TEST_TOPIC, now, Instruction.LOCAL_INSTRUCTION_ID,
				null, null);

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
			public InstructionState processInstruction(Instruction instruction) {
				return InstructionState.Completed;
			}

		};

		service.setHandlers(Arrays.asList(handler));

		final Date now = new Date();
		BasicInstruction instr = new BasicInstruction(TEST_TOPIC, now, Instruction.LOCAL_INSTRUCTION_ID,
				null, null);

		// when
		InstructionStatus status = service.executeInstruction(instr);

		// then
		assertThat("Result status", status, notNullValue());
		assertThat("State", status.getInstructionState(), equalTo(InstructionState.Completed));
		assertThat("Acknoledged state", status.getAcknowledgedInstructionState(), nullValue());
	}

	@Test
	public void handlerThrowsException() {
		// given
		InstructionHandler handler = new TestTopicInstructionHandler() {

			@Override
			public InstructionState processInstruction(Instruction instruction) {
				throw new UnsupportedOperationException("boo");
			}

		};

		service.setHandlers(Arrays.asList(handler));

		final Date now = new Date();
		BasicInstruction instr = new BasicInstruction(TEST_TOPIC, now, Instruction.LOCAL_INSTRUCTION_ID,
				null, null);

		// when
		InstructionStatus status = service.executeInstruction(instr);

		// then
		assertThat("Result status", status, nullValue());
	}

	@Test
	public void handlerThrowsExceptionAnotherHandlerCompletes() {
		// given
		InstructionHandler handler = new TestTopicInstructionHandler() {

			@Override
			public InstructionState processInstruction(Instruction instruction) {
				throw new UnsupportedOperationException("boo");
			}

		};
		InstructionHandler handler2 = new TestTopicInstructionHandler() {

			@Override
			public InstructionState processInstruction(Instruction instruction) {
				return InstructionState.Completed;
			}

		};

		service.setHandlers(Arrays.asList(handler, handler2));

		final Date now = new Date();
		BasicInstruction instr = new BasicInstruction(TEST_TOPIC, now, Instruction.LOCAL_INSTRUCTION_ID,
				null, null);

		// when
		InstructionStatus status = service.executeInstruction(instr);

		// then
		assertThat("Result status", status, notNullValue());
		assertThat("State", status.getInstructionState(), equalTo(InstructionState.Completed));
		assertThat("Acknoledged state", status.getAcknowledgedInstructionState(), nullValue());
	}

	@Test
	public void handlerSkipsAnotherHandlerCompletes() {
		// given
		InstructionHandler handler = new TestTopicInstructionHandler() {

			@Override
			public InstructionState processInstruction(Instruction instruction) {
				return null;
			}

		};
		InstructionHandler handler2 = new TestTopicInstructionHandler() {

			@Override
			public InstructionState processInstruction(Instruction instruction) {
				return InstructionState.Completed;
			}

		};

		service.setHandlers(Arrays.asList(handler, handler2));

		final Date now = new Date();
		BasicInstruction instr = new BasicInstruction(TEST_TOPIC, now, Instruction.LOCAL_INSTRUCTION_ID,
				null, null);

		// when
		InstructionStatus status = service.executeInstruction(instr);

		// then
		assertThat("Result status", status, notNullValue());
		assertThat("State", status.getInstructionState(), equalTo(InstructionState.Completed));
		assertThat("Acknoledged state", status.getAcknowledgedInstructionState(), nullValue());
	}

	@Test
	public void feedbackHandlerTakesPrecedence() {
		// given
		InstructionHandler handler = new TestTopicInstructionHandler() {

			@Override
			public InstructionState processInstruction(Instruction instruction) {
				throw new UnsupportedOperationException("boo");
			}

		};

		FeedbackInstructionHandler feedbackHandler = new TestTopicFeedbackInstructionHandler() {

			@Override
			public InstructionStatus processInstructionWithFeedback(Instruction instruction) {
				return new BasicInstructionStatus(instruction.getId(), InstructionState.Completed,
						new Date());
			}
		};

		service.setHandlers(Arrays.asList(handler));
		service.setFeedbackHandlers(Arrays.asList(feedbackHandler));

		final Date now = new Date();
		BasicInstruction instr = new BasicInstruction(TEST_TOPIC, now, Instruction.LOCAL_INSTRUCTION_ID,
				null, null);

		// when
		InstructionStatus status = service.executeInstruction(instr);

		// then
		assertThat("Result status", status, notNullValue());
		assertThat("State", status.getInstructionState(), equalTo(InstructionState.Completed));
		assertThat("Acknoledged state", status.getAcknowledgedInstructionState(), nullValue());
	}

}

/* ==================================================================
 * DefaultOutstationServceTests.java - 26/02/2019 10:33:04 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.dnp3.impl.test;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.support.TaskExecutorAdapter;
import com.automatak.dnp3.AnalogOutputInt32;
import com.automatak.dnp3.ControlRelayOutputBlock;
import com.automatak.dnp3.Database;
import com.automatak.dnp3.enums.CommandStatus;
import com.automatak.dnp3.enums.OperateType;
import com.automatak.dnp3.enums.OperationType;
import com.automatak.dnp3.enums.TripCloseCode;
import net.solarnetwork.node.io.dnp3.ChannelService;
import net.solarnetwork.node.io.dnp3.domain.ControlConfig;
import net.solarnetwork.node.io.dnp3.domain.ControlType;
import net.solarnetwork.node.io.dnp3.impl.DefaultOutstationService;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.test.CapturingExecutorService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link DefaultOutstationService} class.
 *
 * @author matt
 * @version 3.0
 */
public class DefaultOutstationServceTests {

	private ChannelService channelService;
	private InstructionExecutionService instructionService;
	private Database database;

	@Before
	public void setup() {
		channelService = EasyMock.createMock(ChannelService.class);
		instructionService = EasyMock.createMock(InstructionExecutionService.class);
		database = EasyMock.createMock(Database.class);
	}

	@After
	public void teardown() {
		verifyAll();
	}

	private void replayAll() {
		EasyMock.replay(channelService, instructionService, database);
	}

	private void verifyAll() {
		EasyMock.verify(channelService, instructionService, database);
	}

	private TestOutstationService createOutstationService() {
		TestOutstationService service = new TestOutstationService(
				new StaticOptionalService<>(channelService),
				new StaticOptionalService<>(instructionService));

		return service;
	}

	private static class TestOutstationService extends DefaultOutstationService {

		public TestOutstationService(OptionalService<ChannelService> dnp3Channel,
				OptionalService<InstructionExecutionService> instructionService) {
			super(dnp3Channel, instructionService);
		}

		@Override
		public com.automatak.dnp3.CommandHandler getCommandHandler() {
			return super.getCommandHandler();
		}
	}

	@Test
	public void handleCROBMissingIndex() {
		// given
		TestOutstationService service = createOutstationService();

		final String controlId = "/foo/switch";
		ControlConfig cConfig = new ControlConfig(controlId, ControlType.Binary);
		service.setControlConfigs(new ControlConfig[] { cConfig });

		// when
		replayAll();
		ControlRelayOutputBlock crob = new ControlRelayOutputBlock(OperationType.LATCH_ON,
				TripCloseCode.CLOSE, false, (short) 1, 0, 0, CommandStatus.SUCCESS);
		CommandStatus status = service.getCommandHandler().operate(crob, 123123123, database,
				OperateType.DirectOperate);

		// then
		assertThat("Command rejected because control index out of range", status,
				equalTo(CommandStatus.NOT_AUTHORIZED));
	}

	@Test
	public void handleCROBLatchOn() {
		// GIVEN
		TestOutstationService service = createOutstationService();

		final String controlId = "/foo/switch";
		ControlConfig cConfig = new ControlConfig(controlId, ControlType.Binary);
		service.setControlConfigs(new ControlConfig[] { cConfig });

		Capture<Instruction> instrCaptor = Capture.newInstance();
		expect(instructionService.executeInstruction(capture(instrCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						return createStatus(instrCaptor.getValue(), Completed);
					}
				});

		// WHEN
		replayAll();
		ControlRelayOutputBlock crob = new ControlRelayOutputBlock(OperationType.LATCH_ON,
				TripCloseCode.CLOSE, false, (short) 1, 0, 0, CommandStatus.SUCCESS);
		CommandStatus status = service.getCommandHandler().operate(crob, 0, database,
				OperateType.DirectOperate);

		// THEN
		assertThat("Command OK", status, equalTo(CommandStatus.SUCCESS));

		Instruction instr = instrCaptor.getValue();
		assertThat("Instruction", instr.getTopic(),
				equalTo(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER));
		assertThat("Control ID param", instr.getParameterValue(controlId),
				equalTo(Boolean.TRUE.toString()));
	}

	@Test
	public void handleCROBLatchOff() {
		// given
		TestOutstationService service = createOutstationService();

		final String controlId = "/foo/switch";
		ControlConfig cConfig = new ControlConfig(controlId, ControlType.Binary);
		service.setControlConfigs(new ControlConfig[] { cConfig });

		Capture<Instruction> instrCaptor = Capture.newInstance();
		expect(instructionService.executeInstruction(capture(instrCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						return createStatus(instrCaptor.getValue(), Completed);
					}
				});

		// when
		replayAll();
		ControlRelayOutputBlock crob = new ControlRelayOutputBlock(OperationType.LATCH_OFF,
				TripCloseCode.CLOSE, false, (short) 1, 0, 0, CommandStatus.SUCCESS);
		CommandStatus status = service.getCommandHandler().operate(crob, 0, database,
				OperateType.DirectOperate);

		// then
		assertThat("Command OK", status, equalTo(CommandStatus.SUCCESS));

		Instruction instr = instrCaptor.getValue();
		assertThat("Instruction", instr.getTopic(),
				equalTo(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER));
		assertThat("Control ID param", instr.getParameterValue(controlId),
				equalTo(Boolean.FALSE.toString()));
	}

	@Test
	public void handleCROBLatchOnWithTaskExecutor() throws InterruptedException {
		// given
		TestOutstationService service = createOutstationService();
		CapturingExecutorService executor = new CapturingExecutorService(newSingleThreadExecutor());
		service.setTaskExecutor(new TaskExecutorAdapter(executor));

		final String controlId = "/foo/switch";
		ControlConfig cConfig = new ControlConfig(controlId, ControlType.Binary);
		service.setControlConfigs(new ControlConfig[] { cConfig });

		Capture<Instruction> instrCaptor = Capture.newInstance();
		expect(instructionService.executeInstruction(capture(instrCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						return createStatus(instrCaptor.getValue(), Completed);
					}
				});

		// when
		replayAll();
		ControlRelayOutputBlock crob = new ControlRelayOutputBlock(OperationType.LATCH_ON,
				TripCloseCode.CLOSE, false, (short) 1, 0, 0, CommandStatus.SUCCESS);
		CommandStatus status = service.getCommandHandler().operate(crob, 0, database,
				OperateType.DirectOperate);

		// wait for bg task
		executor.shutdown();
		executor.awaitTermination(2, TimeUnit.SECONDS);

		// then
		assertThat("Command OK", status, equalTo(CommandStatus.SUCCESS));

		Instruction instr = instrCaptor.getValue();
		assertThat("Instruction", instr.getTopic(),
				equalTo(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER));
		assertThat("Control ID param", instr.getParameterValue(controlId),
				equalTo(Boolean.TRUE.toString()));

		assertThat("Instruction handled in background", executor.getCapturedFutures(), hasSize(1));
	}

	@Test
	public void handleAnalogInt32() {
		// given
		TestOutstationService service = createOutstationService();

		final String controlId = "/foo/limiter";
		ControlConfig cConfig = new ControlConfig(controlId, ControlType.Analog);
		service.setControlConfigs(new ControlConfig[] { cConfig });

		Capture<Instruction> instrCaptor = Capture.newInstance();
		expect(instructionService.executeInstruction(capture(instrCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						return createStatus(instrCaptor.getValue(), Completed);
					}
				});

		// when
		replayAll();
		AnalogOutputInt32 cmd = new AnalogOutputInt32(321456, CommandStatus.SUCCESS);
		CommandStatus status = service.getCommandHandler().operate(cmd, 0, database,
				OperateType.DirectOperate);

		// then
		assertThat("Command OK", status, equalTo(CommandStatus.SUCCESS));

		Instruction instr = instrCaptor.getValue();
		assertThat("Instruction", instr.getTopic(),
				equalTo(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER));
		assertThat("Control ID param", instr.getParameterValue(controlId),
				equalTo(String.valueOf(cmd.value)));
	}

	@Test
	public void handleAnalogInt32WithTaskExecutor() throws InterruptedException {
		// given
		TestOutstationService service = createOutstationService();
		CapturingExecutorService executor = new CapturingExecutorService(newSingleThreadExecutor());
		service.setTaskExecutor(new TaskExecutorAdapter(executor));

		final String controlId = "/foo/limiter";
		ControlConfig cConfig = new ControlConfig(controlId, ControlType.Analog);
		service.setControlConfigs(new ControlConfig[] { cConfig });

		Capture<Instruction> instrCaptor = Capture.newInstance();
		expect(instructionService.executeInstruction(capture(instrCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						return createStatus(instrCaptor.getValue(), Completed);
					}
				});

		// when
		replayAll();
		AnalogOutputInt32 cmd = new AnalogOutputInt32(321456, CommandStatus.SUCCESS);
		CommandStatus status = service.getCommandHandler().operate(cmd, 0, database,
				OperateType.DirectOperate);

		// wait for bg task
		executor.shutdown();
		executor.awaitTermination(2, TimeUnit.SECONDS);

		// then
		assertThat("Command OK", status, equalTo(CommandStatus.SUCCESS));

		Instruction instr = instrCaptor.getValue();
		assertThat("Instruction", instr.getTopic(),
				equalTo(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER));
		assertThat("Control ID param", instr.getParameterValue(controlId),
				equalTo(String.valueOf(cmd.value)));

		assertThat("Instruction handled in background", executor.getCapturedFutures(), hasSize(1));
	}

}

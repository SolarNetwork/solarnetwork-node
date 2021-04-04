/* ==================================================================
 * OutstationServceTests.java - 26/02/2019 10:33:04 am
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
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SET_CONTROL_PARAMETER;
import static net.solarnetwork.node.reactor.InstructionStatus.InstructionState.Completed;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.support.TaskExecutorAdapter;
import com.automatak.dnp3.AnalogOutputInt32;
import com.automatak.dnp3.ControlRelayOutputBlock;
import com.automatak.dnp3.enums.CommandStatus;
import com.automatak.dnp3.enums.ControlCode;
import com.automatak.dnp3.enums.OperateType;
import net.solarnetwork.node.io.dnp3.ChannelService;
import net.solarnetwork.node.io.dnp3.domain.ControlConfig;
import net.solarnetwork.node.io.dnp3.domain.ControlType;
import net.solarnetwork.node.io.dnp3.impl.OutstationService;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.test.CapturingExecutorService;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.OptionalServiceCollection;
import net.solarnetwork.util.StaticOptionalService;
import net.solarnetwork.util.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link OutstationService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class OutstationServceTests {

	private ChannelService channelService;
	private InstructionHandler instructionHandler;

	@Before
	public void setup() {
		channelService = EasyMock.createMock(ChannelService.class);
		instructionHandler = EasyMock.createMock(InstructionHandler.class);
	}

	@After
	public void teardown() {
		verifyAll();
	}

	private void replayAll() {
		EasyMock.replay(channelService, instructionHandler);
	}

	private void verifyAll() {
		EasyMock.verify(channelService, instructionHandler);
	}

	private TestOutstationService createOutstationService() {
		return createOutstationService(Collections.singleton(instructionHandler));
	}

	private TestOutstationService createOutstationService(Collection<InstructionHandler> handlers) {
		TestOutstationService service = new TestOutstationService(
				new StaticOptionalService<ChannelService>(channelService),
				new StaticOptionalServiceCollection<>(handlers));

		return service;
	}

	private static class TestOutstationService extends OutstationService {

		public TestOutstationService(OptionalService<ChannelService> dnp3Channel,
				OptionalServiceCollection<InstructionHandler> instructionHandlers) {
			super(dnp3Channel, instructionHandlers);
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
		ControlConfig cConfig = new ControlConfig(null, controlId, ControlType.Binary);
		service.setControlConfigs(new ControlConfig[] { cConfig });

		// when
		replayAll();
		ControlRelayOutputBlock crob = new ControlRelayOutputBlock(ControlCode.LATCH_ON, (short) 1, 0, 0,
				CommandStatus.SUCCESS);
		CommandStatus status = service.getCommandHandler().operateCROB(crob, 123123123,
				OperateType.DirectOperate);

		// then
		assertThat("Command rejected because control index out of range", status,
				equalTo(CommandStatus.NOT_AUTHORIZED));
	}

	@Test
	public void handleCROBLatchOn() {
		// given
		TestOutstationService service = createOutstationService();

		final String controlId = "/foo/switch";
		ControlConfig cConfig = new ControlConfig(null, controlId, ControlType.Binary);
		service.setControlConfigs(new ControlConfig[] { cConfig });

		expect(instructionHandler.handlesTopic(TOPIC_SET_CONTROL_PARAMETER)).andReturn(true);

		Capture<Instruction> instrCaptor = new Capture<>();
		expect(instructionHandler.processInstruction(capture(instrCaptor))).andReturn(Completed);

		// when
		replayAll();
		ControlRelayOutputBlock crob = new ControlRelayOutputBlock(ControlCode.LATCH_ON, (short) 1, 0, 0,
				CommandStatus.SUCCESS);
		CommandStatus status = service.getCommandHandler().operateCROB(crob, 0,
				OperateType.DirectOperate);

		// then
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
		ControlConfig cConfig = new ControlConfig(null, controlId, ControlType.Binary);
		service.setControlConfigs(new ControlConfig[] { cConfig });

		expect(instructionHandler.handlesTopic(TOPIC_SET_CONTROL_PARAMETER)).andReturn(true);

		Capture<Instruction> instrCaptor = new Capture<>();
		expect(instructionHandler.processInstruction(capture(instrCaptor))).andReturn(Completed);

		// when
		replayAll();
		ControlRelayOutputBlock crob = new ControlRelayOutputBlock(ControlCode.LATCH_OFF, (short) 1, 0,
				0, CommandStatus.SUCCESS);
		CommandStatus status = service.getCommandHandler().operateCROB(crob, 0,
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
		ControlConfig cConfig = new ControlConfig(null, controlId, ControlType.Binary);
		service.setControlConfigs(new ControlConfig[] { cConfig });

		expect(instructionHandler.handlesTopic(TOPIC_SET_CONTROL_PARAMETER)).andReturn(true);

		Capture<Instruction> instrCaptor = new Capture<>();
		expect(instructionHandler.processInstruction(capture(instrCaptor))).andReturn(Completed);

		// when
		replayAll();
		ControlRelayOutputBlock crob = new ControlRelayOutputBlock(ControlCode.LATCH_ON, (short) 1, 0, 0,
				CommandStatus.SUCCESS);
		CommandStatus status = service.getCommandHandler().operateCROB(crob, 0,
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
		ControlConfig cConfig = new ControlConfig(null, controlId, ControlType.Analog);
		service.setControlConfigs(new ControlConfig[] { cConfig });

		expect(instructionHandler.handlesTopic(TOPIC_SET_CONTROL_PARAMETER)).andReturn(true);

		Capture<Instruction> instrCaptor = new Capture<>();
		expect(instructionHandler.processInstruction(capture(instrCaptor))).andReturn(Completed);

		// when
		replayAll();
		AnalogOutputInt32 cmd = new AnalogOutputInt32(321456, CommandStatus.SUCCESS);
		CommandStatus status = service.getCommandHandler().operateAOI32(cmd, 0,
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
		ControlConfig cConfig = new ControlConfig(null, controlId, ControlType.Analog);
		service.setControlConfigs(new ControlConfig[] { cConfig });

		expect(instructionHandler.handlesTopic(TOPIC_SET_CONTROL_PARAMETER)).andReturn(true);

		Capture<Instruction> instrCaptor = new Capture<>();
		expect(instructionHandler.processInstruction(capture(instrCaptor))).andReturn(Completed);

		// when
		replayAll();
		AnalogOutputInt32 cmd = new AnalogOutputInt32(321456, CommandStatus.SUCCESS);
		CommandStatus status = service.getCommandHandler().operateAOI32(cmd, 0,
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

/* ==================================================================
 * AcExportManagerTests.java - 3/09/2019 10:02:59 am
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

package net.solarnetwork.node.control.stabiliti30c.test;

import static java.lang.System.arraycopy;
import static net.solarnetwork.node.io.modbus.ModbusReadFunction.ReadHoldingRegister;
import static net.solarnetwork.node.io.modbus.ModbusWriteFunction.WriteHoldingRegister;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.EventAdmin;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.control.stabiliti30c.AcExportManager;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cAcControlMethod;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cDcControlMethod;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cRegister;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.test.DataUtils;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link AcExportManager} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AcExportManagerTests {

	private static final String TEST_CONTROL_ID = "/test/power/1";
	private static final int TEST_UNIT_ID = 35;
	private static final long TEST_SAMPLE_CACHE_MS = 123L;
	private static final Charset UTF8 = Charset.forName("UTF-8");

	private static final int[] TEST_SERIAL_NUM = new int[] { 1, 2, 3, 4, 5, 6, 7, 8 };
	private static final int[] TEST_COMMS_VERS = new int[] { 1, 2 };

	private ModbusNetwork modbus;
	private ModbusConnection conn;
	private EventAdmin eventAdmin;
	private MessageSource messageSource;
	private AcExportManager service;

	@Before
	public void setup() {
		modbus = EasyMock.createMock(ModbusNetwork.class);
		conn = EasyMock.createMock(ModbusConnection.class);
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		messageSource = EasyMock.createMock(MessageSource.class);
		service = new AcExportManager();
		service.setModbusNetwork(new StaticOptionalService<ModbusNetwork>(modbus));
		service.setUnitId(TEST_UNIT_ID);
		service.setControlId(TEST_CONTROL_ID);
		service.setEventAdmin(new StaticOptionalService<EventAdmin>(eventAdmin));
		service.setMessageSource(messageSource);
		service.setSampleCacheMs(TEST_SAMPLE_CACHE_MS);
	}

	private class TestModbusNetwork extends AbstractModbusNetwork {

		@Override
		public <T> T performAction(int unitId, ModbusConnectionAction<T> action) throws IOException {
			return action.doWithConnection(conn);
		}

	}

	@After
	public void teardown() {
		EasyMock.verify(modbus, conn, eventAdmin, messageSource);
	}

	private void replayAll() {
		EasyMock.replay(modbus, conn, eventAdmin, messageSource);
	}

	private void resetAll() {
		EasyMock.reset(modbus, conn, eventAdmin, messageSource);
	}

	@SuppressWarnings("unchecked")
	private <T> ModbusConnectionAction<T> anyAction(Class<T> type) {
		return EasyMock.anyObject(ModbusConnectionAction.class);
	}

	private void expectReadDeviceInfo(int[] data, int[] serialNumber, int[] commsVersion) {
		final int[] read1 = new int[49];
		arraycopy(data, Stabiliti30cRegister.StatusFaultActive0.getAddress(), read1, 0, 49);
		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.StatusFaultActive0.getAddress(), 49)).andReturn(read1);

		final int[] read2 = new int[55];
		arraycopy(data, Stabiliti30cRegister.PowerControlP1RealPower.getAddress(), read2, 0, 55);
		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.PowerControlP1RealPower.getAddress(), 55)).andReturn(read2);

		final int[] read3 = new int[53];
		arraycopy(data, Stabiliti30cRegister.PowerControlP2Power.getAddress(), read3, 0, 53);
		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.PowerControlP2Power.getAddress(), 53)).andReturn(read3);

		final int[] read4 = new int[50];
		arraycopy(data, Stabiliti30cRegister.PowerControlP3Power.getAddress(), read4, 0, 50);
		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.PowerControlP3Power.getAddress(), 50)).andReturn(read4);

		expect(conn.readWordsUnsigned(ReadHoldingRegister, Stabiliti30cRegister.StatusInfo.getAddress(),
				1)).andReturn(new int[] { data[Stabiliti30cRegister.StatusInfo.getAddress()] });

		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.InfoSerialNumber.getAddress(), 8)).andReturn(serialNumber);

		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.InfoCommsVersion.getAddress(), 2)).andReturn(commsVersion);
	}

	private void expectReadControlData(int[] data, int controlCommand) {
		final int[] read5 = new int[51];
		arraycopy(data, Stabiliti30cRegister.ControlWatchdogSeconds.getAddress(), read5, 0, 51);
		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.ControlWatchdogSeconds.getAddress(), 51)).andReturn(read5);

		final int[] read6 = new int[26];
		arraycopy(data, Stabiliti30cRegister.ControlP2ControlMethod.getAddress(), read6, 0, 26);
		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.ControlP2ControlMethod.getAddress(), 26)).andReturn(read6);

		final int[] read7 = new int[26];
		arraycopy(data, Stabiliti30cRegister.ControlP3ControlMethod.getAddress(), read7, 0, 26);
		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.ControlP3ControlMethod.getAddress(), 26)).andReturn(read7);

		final int[] read8 = new int[2];
		arraycopy(data, Stabiliti30cRegister.ControlManualModeStart.getAddress(), read8, 0, 2);
		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.ControlManualModeStart.getAddress(), 2)).andReturn(read8);

		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.ControlCommand.getAddress(), 1))
						.andReturn(new int[] { controlCommand });
	}

	private void expectReadRuntimeData(int[] data) {
		final int[] read8 = new int[49];
		arraycopy(data, Stabiliti30cRegister.StatusFaultActive0.getAddress(), read8, 0, 4);
		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.StatusFaultActive0.getAddress(), 4)).andReturn(read8);

		final int[] read2a = new int[55];
		arraycopy(data, Stabiliti30cRegister.PowerControlP1RealPower.getAddress(), read2a, 0, 55);
		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.PowerControlP1RealPower.getAddress(), 55)).andReturn(read2a);

		final int[] read3a = new int[53];
		arraycopy(data, Stabiliti30cRegister.PowerControlP2Power.getAddress(), read3a, 0, 53);
		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.PowerControlP2Power.getAddress(), 53)).andReturn(read3a);

		final int[] read4a = new int[50];
		arraycopy(data, Stabiliti30cRegister.PowerControlP3Power.getAddress(), read4a, 0, 50);
		expect(conn.readWordsUnsigned(ReadHoldingRegister,
				Stabiliti30cRegister.PowerControlP3Power.getAddress(), 50)).andReturn(read4a);

		expect(conn.readWordsUnsigned(ReadHoldingRegister, Stabiliti30cRegister.StatusInfo.getAddress(),
				1)).andReturn(new int[] { data[Stabiliti30cRegister.StatusInfo.getAddress()] });
	}

	private void expectStartupResetDeviceState() throws IOException {
		conn.writeWords(eq(WriteHoldingRegister),
				eq(Stabiliti30cRegister.ControlP1ControlMethod.getAddress()),
				aryEq(new int[] { Stabiliti30cAcControlMethod.Net.getCode() }));

		conn.writeWords(eq(WriteHoldingRegister),
				eq(Stabiliti30cRegister.ControlP2ControlMethod.getAddress()),
				aryEq(new int[] { Stabiliti30cDcControlMethod.Idle.getCode() }));

		conn.writeWords(eq(WriteHoldingRegister),
				eq(Stabiliti30cRegister.ControlP3ControlMethod.getAddress()),
				aryEq(new int[] { Stabiliti30cDcControlMethod.Mppt.getCode() }));

		conn.writeWords(eq(WriteHoldingRegister),
				eq(Stabiliti30cRegister.ControlP1RealPowerSetpoint.getAddress()),
				aryEq(new int[] { 0 }));

		conn.writeWords(eq(WriteHoldingRegister),
				eq(Stabiliti30cRegister.ControlManualModeStop.getAddress()), aryEq(new int[] { 1 }));
	}

	private void expectResetSafeDeviceState() throws IOException {
		conn.writeWords(eq(WriteHoldingRegister),
				eq(Stabiliti30cRegister.ControlP1ControlMethod.getAddress()),
				aryEq(new int[] { Stabiliti30cAcControlMethod.Idle.getCode() }));

		conn.writeWords(eq(WriteHoldingRegister),
				eq(Stabiliti30cRegister.ControlP2ControlMethod.getAddress()),
				aryEq(new int[] { Stabiliti30cDcControlMethod.Idle.getCode() }));

		conn.writeWords(eq(WriteHoldingRegister),
				eq(Stabiliti30cRegister.ControlP3ControlMethod.getAddress()),
				aryEq(new int[] { Stabiliti30cDcControlMethod.Idle.getCode() }));
	}

	@Test
	public void startupResetsDeviceStateToKnownValues() throws IOException {
		// GIVEN
		expect(modbus.performAction(eq(TEST_UNIT_ID), anyAction(Void.class)))
				.andDelegateTo(new TestModbusNetwork());

		// first reset state to known safe values...
		expectResetSafeDeviceState();

		// then reset state to known starting values...
		expectStartupResetDeviceState();

		final int[] initialData = DataUtils.parseModbusHexRegisterLines(new BufferedReader(
				new InputStreamReader(getClass().getResourceAsStream("stability-data-01.txt"), UTF8)));

		// read info and controls from device into state data first time startup...
		expectReadDeviceInfo(initialData, TEST_SERIAL_NUM, TEST_COMMS_VERS);
		expectReadControlData(initialData, 0);

		// read controls from device again...
		expectReadControlData(initialData, 0);

		// then read configuration from device into state data...
		expectReadRuntimeData(initialData);

		// WHEN
		replayAll();
		service.startup();

		// THEN
	}

	@Test
	public void handleShedLoadInstruction() throws Exception {
		// GIVEN
		startupResetsDeviceStateToKnownValues();

		resetAll();

		expect(modbus.performAction(eq(TEST_UNIT_ID), anyAction(Void.class)))
				.andDelegateTo(new TestModbusNetwork());

		// depending on speed of test, a new snapshot may be read because of TEST_SAMPLE_CACHE_MS
		eventAdmin.postEvent(anyObject());
		expectLastCall().anyTimes();

		// first reset state to known safe values...
		expectResetSafeDeviceState();

		final int shedPowerAmount = 1000;
		conn.writeWords(eq(WriteHoldingRegister),
				eq(Stabiliti30cRegister.ControlP1RealPowerSetpoint.getAddress()),
				aryEq(new int[] { shedPowerAmount / 10 }));

		conn.writeWords(eq(WriteHoldingRegister),
				eq(Stabiliti30cRegister.ControlManualModeStart.getAddress()), aryEq(new int[] { 1 }));

		// WHEN
		replayAll();

		Date now = new Date();
		BasicInstruction shedLoad = new BasicInstruction(InstructionHandler.TOPIC_SHED_LOAD, now,
				UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
		shedLoad.addParameter(TEST_CONTROL_ID, String.valueOf(shedPowerAmount));
		InstructionState result = service.processInstruction(shedLoad);

		// THEN
		assertThat("Handled OK", result, equalTo(InstructionState.Completed));
	}

	@Test
	public void handleShedLoadInstructionZeroValue() throws IOException {
		// GIVEN
		startupResetsDeviceStateToKnownValues();
		resetAll();

		expect(modbus.performAction(eq(TEST_UNIT_ID), anyAction(Void.class)))
				.andDelegateTo(new TestModbusNetwork());

		// depending on speed of test, a new snapshot may be read because of TEST_SAMPLE_CACHE_MS
		eventAdmin.postEvent(anyObject());
		expectLastCall().anyTimes();

		// first reset state to known safe values...
		expectResetSafeDeviceState();

		// then reset state to known safe values...
		expectStartupResetDeviceState();

		// WHEN
		replayAll();

		Date now = new Date();
		BasicInstruction shedLoad = new BasicInstruction(InstructionHandler.TOPIC_SHED_LOAD, now,
				UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
		shedLoad.addParameter(TEST_CONTROL_ID, String.valueOf(0));
		InstructionState result = service.processInstruction(shedLoad);

		// THEN
		assertThat("Handled OK", result, equalTo(InstructionState.Completed));
	}

	@Test
	public void handleSetControlParameterInstruction() throws IOException {
		// GIVEN
		startupResetsDeviceStateToKnownValues();
		resetAll();

		expect(modbus.performAction(eq(TEST_UNIT_ID), anyAction(Void.class)))
				.andDelegateTo(new TestModbusNetwork());

		// depending on speed of test, a new snapshot may be read because of TEST_SAMPLE_CACHE_MS
		eventAdmin.postEvent(anyObject());
		expectLastCall().anyTimes();

		// first reset state to known safe values...
		expectResetSafeDeviceState();

		final int shedPowerAmount = 1000;
		conn.writeWords(eq(WriteHoldingRegister),
				eq(Stabiliti30cRegister.ControlP1RealPowerSetpoint.getAddress()),
				aryEq(new int[] { shedPowerAmount / 10 }));

		conn.writeWords(eq(WriteHoldingRegister),
				eq(Stabiliti30cRegister.ControlManualModeStart.getAddress()), aryEq(new int[] { 1 }));

		// WHEN
		replayAll();

		Date now = new Date();
		BasicInstruction shedLoad = new BasicInstruction(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER,
				now, UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
		shedLoad.addParameter(TEST_CONTROL_ID, String.valueOf(shedPowerAmount));
		InstructionState result = service.processInstruction(shedLoad);

		// THEN
		assertThat("Handled OK", result, equalTo(InstructionState.Completed));
	}

	@Test
	public void handleSetControlParameterInstructionZeroValue() throws IOException {
		// GIVEN
		startupResetsDeviceStateToKnownValues();
		resetAll();

		expect(modbus.performAction(eq(TEST_UNIT_ID), anyAction(Void.class)))
				.andDelegateTo(new TestModbusNetwork());

		// depending on speed of test, a new snapshot may be read because of TEST_SAMPLE_CACHE_MS
		eventAdmin.postEvent(anyObject());
		expectLastCall().anyTimes();

		// first reset state to known safe values...
		expectResetSafeDeviceState();

		// then reset state to known safe values...
		expectStartupResetDeviceState();

		// WHEN
		replayAll();

		Date now = new Date();
		BasicInstruction shedLoad = new BasicInstruction(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER,
				now, UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
		shedLoad.addParameter(TEST_CONTROL_ID, String.valueOf(0));
		InstructionState result = service.processInstruction(shedLoad);

		// THEN
		assertThat("Handled OK", result, equalTo(InstructionState.Completed));
	}
}

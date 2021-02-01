/* ==================================================================
 * ModbusConnectionHandlerTests.java - 18/09/2020 9:44:25 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.server.impl.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.io.modbus.server.domain.ModbusRegisterData;
import net.solarnetwork.node.io.modbus.server.impl.ModbusConnectionHandler;
import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.io.ModbusTransport;
import net.wimpi.modbus.msg.ModbusMessage;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;

/**
 * Test cases for the {@link ModbusConnectionHandler} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ModbusConnectionHandlerTests {

	private ModbusTransport transport;
	private ConcurrentMap<Integer, ModbusRegisterData> units;
	private ModbusRegisterData registers;
	private ModbusConnectionHandler handler;

	@Before
	public void setup() {
		transport = EasyMock.createMock(ModbusTransport.class);
		units = new ConcurrentHashMap<>(1, 0.9f, 1);
		registers = new ModbusRegisterData();
		units.put(2, registers);
		handler = new ModbusConnectionHandler(transport, units, "Test");
	}

	private void replayAll() {
		EasyMock.replay(transport);
	}

	@After
	public void teardown() {
		EasyMock.verify(transport);
	}

	private void expectReadMessages(Capture<ModbusMessage> captor, ModbusRequest... reqs)
			throws Exception {
		for ( ModbusRequest req : reqs ) {
			expect(transport.readRequest()).andReturn(req);
			transport.writeMessage(capture(captor));
			expect(transport.readRequest()).andThrow(new ModbusIOException(true));
		}
		transport.close();
	}

	@Test
	public void readCoil() throws Exception {
		// GIVEN
		ReadCoilsRequest req = new ReadCoilsRequest(0, 1);
		req.setTransactionID(1);
		req.setUnitID(2);
		Capture<ModbusMessage> msgCaptor = new Capture<>();
		expectReadMessages(msgCaptor, req);

		// WHEN
		replayAll();
		handler.run();

		// THEN
		ModbusMessage msg = msgCaptor.getValue();
		assertThat("Response function code", msg.getFunctionCode(), equalTo(Modbus.READ_COILS));
		assertThat("Response frame", msg.getHexMessage().trim().toUpperCase(),
				equalTo("00 01 00 00 00 04 02 01 01 00"));
	}

	@Test
	public void readCoil_on() throws Exception {
		// GIVEN
		registers.writeCoil(22, true);
		ReadCoilsRequest req = new ReadCoilsRequest(22, 1);
		req.setTransactionID(1);
		req.setUnitID(2);
		Capture<ModbusMessage> msgCaptor = new Capture<>();
		expectReadMessages(msgCaptor, req);

		// WHEN
		replayAll();
		handler.run();

		// THEN
		ModbusMessage msg = msgCaptor.getValue();
		assertThat("Response function code", msg.getFunctionCode(), equalTo(Modbus.READ_COILS));
		assertThat("Response frame", msg.getHexMessage().trim().toUpperCase(),
				equalTo("00 01 00 00 00 04 02 01 01 01"));
	}

	@Test
	public void readCoils() throws Exception {
		// GIVEN
		registers.writeCoil(0, true);
		registers.writeCoil(2, true);
		registers.writeCoil(5, true);
		registers.writeCoil(6, true);
		registers.writeCoil(7, true);
		registers.writeCoil(9, true);
		registers.writeCoil(10, true);
		ReadCoilsRequest req = new ReadCoilsRequest(0, 11);
		req.setTransactionID(1);
		req.setUnitID(2);
		Capture<ModbusMessage> msgCaptor = new Capture<>();
		expectReadMessages(msgCaptor, req);

		// WHEN
		replayAll();
		handler.run();

		// THEN
		ModbusMessage msg = msgCaptor.getValue();
		assertThat("Response function code", msg.getFunctionCode(), equalTo(Modbus.READ_COILS));
		assertThat("Response frame", msg.getHexMessage().trim().toUpperCase(),
				equalTo("00 01 00 00 00 05 02 01 02 E5 06"));
	}

	@Test
	public void readDiscrete() throws Exception {
		// GIVEN
		ReadInputDiscretesRequest req = new ReadInputDiscretesRequest(0, 1);
		req.setTransactionID(1);
		req.setUnitID(2);
		Capture<ModbusMessage> msgCaptor = new Capture<>();
		expectReadMessages(msgCaptor, req);

		// WHEN
		replayAll();
		handler.run();

		// THEN
		ModbusMessage msg = msgCaptor.getValue();
		assertThat("Response function code", msg.getFunctionCode(),
				equalTo(Modbus.READ_INPUT_DISCRETES));
		assertThat("Response frame", msg.getHexMessage().trim().toUpperCase(),
				equalTo("00 01 00 00 00 04 02 02 01 00"));
	}

	@Test
	public void readDiscrete_on() throws Exception {
		// GIVEN
		registers.writeDiscrete(22, true);
		ReadInputDiscretesRequest req = new ReadInputDiscretesRequest(22, 1);
		req.setTransactionID(1);
		req.setUnitID(2);
		Capture<ModbusMessage> msgCaptor = new Capture<>();
		expectReadMessages(msgCaptor, req);

		// WHEN
		replayAll();
		handler.run();

		// THEN
		ModbusMessage msg = msgCaptor.getValue();
		assertThat("Response function code", msg.getFunctionCode(),
				equalTo(Modbus.READ_INPUT_DISCRETES));
		assertThat("Response frame", msg.getHexMessage().trim().toUpperCase(),
				equalTo("00 01 00 00 00 04 02 02 01 01"));
	}

	@Test
	public void readDiscretes() throws Exception {
		// GIVEN
		registers.writeDiscrete(0, true);
		registers.writeDiscrete(2, true);
		registers.writeDiscrete(5, true);
		registers.writeDiscrete(6, true);
		registers.writeDiscrete(7, true);
		registers.writeDiscrete(9, true);
		registers.writeDiscrete(10, true);
		ReadInputDiscretesRequest req = new ReadInputDiscretesRequest(0, 11);
		req.setTransactionID(1);
		req.setUnitID(2);
		Capture<ModbusMessage> msgCaptor = new Capture<>();
		expectReadMessages(msgCaptor, req);

		// WHEN
		replayAll();
		handler.run();

		// THEN
		ModbusMessage msg = msgCaptor.getValue();
		assertThat("Response function code", msg.getFunctionCode(),
				equalTo(Modbus.READ_INPUT_DISCRETES));
		assertThat("Response frame", msg.getHexMessage().trim().toUpperCase(),
				equalTo("00 01 00 00 00 05 02 02 02 E5 06"));
	}

	@Test
	public void readHolding() throws Exception {
		// GIVEN
		registers.writeHolding(17, (short) 123);
		ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(17, 1);
		req.setTransactionID(1);
		req.setUnitID(2);
		Capture<ModbusMessage> msgCaptor = new Capture<>();
		expectReadMessages(msgCaptor, req);

		// WHEN
		replayAll();
		handler.run();

		// THEN
		ModbusMessage msg = msgCaptor.getValue();
		assertThat("Response function code", msg.getFunctionCode(),
				equalTo(Modbus.READ_MULTIPLE_REGISTERS));
		assertThat("Response frame", msg.getHexMessage().trim().toUpperCase(),
				equalTo("00 01 00 00 00 05 02 03 02 00 7B"));
	}

	@Test
	public void readHoldings() throws Exception {
		// GIVEN
		registers.writeHolding(17, (short) 123);
		registers.writeHolding(19, (short) 0xFEDC);
		ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(17, 3);
		req.setTransactionID(1);
		req.setUnitID(2);
		Capture<ModbusMessage> msgCaptor = new Capture<>();
		expectReadMessages(msgCaptor, req);

		// WHEN
		replayAll();
		handler.run();

		// THEN
		ModbusMessage msg = msgCaptor.getValue();
		assertThat("Response function code", msg.getFunctionCode(),
				equalTo(Modbus.READ_MULTIPLE_REGISTERS));
		assertThat("Response frame", msg.getHexMessage().trim().toUpperCase(),
				equalTo("00 01 00 00 00 09 02 03 06 00 7B 00 00 FE DC"));
	}

	@Test
	public void readInput() throws Exception {
		// GIVEN
		registers.writeInput(17, (short) 123);
		ReadInputRegistersRequest req = new ReadInputRegistersRequest(17, 1);
		req.setTransactionID(1);
		req.setUnitID(2);
		Capture<ModbusMessage> msgCaptor = new Capture<>();
		expectReadMessages(msgCaptor, req);

		// WHEN
		replayAll();
		handler.run();

		// THEN
		ModbusMessage msg = msgCaptor.getValue();
		assertThat("Response function code", msg.getFunctionCode(),
				equalTo(Modbus.READ_INPUT_REGISTERS));
		assertThat("Response frame", msg.getHexMessage().trim().toUpperCase(),
				equalTo("00 01 00 00 00 05 02 04 02 00 7B"));
	}

	@Test
	public void readInputs() throws Exception {
		// GIVEN
		registers.writeInput(17, (short) 123);
		registers.writeInput(19, (short) 0xFEDC);
		ReadInputRegistersRequest req = new ReadInputRegistersRequest(17, 3);
		req.setTransactionID(1);
		req.setUnitID(2);
		Capture<ModbusMessage> msgCaptor = new Capture<>();
		expectReadMessages(msgCaptor, req);

		// WHEN
		replayAll();
		handler.run();

		// THEN
		ModbusMessage msg = msgCaptor.getValue();
		assertThat("Response function code", msg.getFunctionCode(),
				equalTo(Modbus.READ_INPUT_REGISTERS));
		assertThat("Response frame", msg.getHexMessage().trim().toUpperCase(),
				equalTo("00 01 00 00 00 09 02 04 06 00 7B 00 00 FE DC"));
	}
}

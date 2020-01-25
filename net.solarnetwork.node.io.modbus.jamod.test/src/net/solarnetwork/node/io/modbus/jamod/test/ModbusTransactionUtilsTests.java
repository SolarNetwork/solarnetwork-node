/* ==================================================================
 * ModbusTransactionUtilsTests.java - 24/03/2018 7:42:41 AM
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

package net.solarnetwork.node.io.modbus.jamod.test;

import static net.solarnetwork.util.ByteUtils.objectArray;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.jamod.ModbusTransactionUtils;
import net.solarnetwork.util.ByteUtils;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusTransaction;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;

/**
 * Test cases for the {@link ModbusTransactionUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ModbusTransactionUtilsTests {

	@Test
	public void readRequestCoil() {
		ModbusRequest req = ModbusTransactionUtils.modbusReadRequest(ModbusReadFunction.ReadCoil, 2,
				true, 3, 4);
		assertThat("Function code", req.getFunctionCode(), equalTo(1));
		assertThat("Unit ID", req.getUnitID(), equalTo(2));
		assertThat("Headless", req.isHeadless(), equalTo(true));
		assertThat("Instance class", req, instanceOf(ReadCoilsRequest.class));
		assertThat("Starting address", ((ReadCoilsRequest) req).getReference(), equalTo(3));
		assertThat("Count", ((ReadCoilsRequest) req).getBitCount(), equalTo(4));
	}

	@Test
	public void readRequestDiscreteInput() {
		ModbusRequest req = ModbusTransactionUtils
				.modbusReadRequest(ModbusReadFunction.ReadDiscreteInput, 2, true, 3, 4);
		assertThat("Function code", req.getFunctionCode(), equalTo(2));
		assertThat("Unit ID", req.getUnitID(), equalTo(2));
		assertThat("Headless", req.isHeadless(), equalTo(true));
		assertThat("Instance class", req, instanceOf(ReadInputDiscretesRequest.class));
		assertThat("Starting address", ((ReadInputDiscretesRequest) req).getReference(), equalTo(3));
		assertThat("Count", ((ReadInputDiscretesRequest) req).getBitCount(), equalTo(4));
	}

	@Test
	public void readRequestHolding() {
		ModbusRequest req = ModbusTransactionUtils
				.modbusReadRequest(ModbusReadFunction.ReadHoldingRegister, 2, true, 3, 4);
		assertThat("Function code", req.getFunctionCode(), equalTo(3));
		assertThat("Unit ID", req.getUnitID(), equalTo(2));
		assertThat("Headless", req.isHeadless(), equalTo(true));
		assertThat("Instance class", req, instanceOf(ReadMultipleRegistersRequest.class));
		assertThat("Starting address", ((ReadMultipleRegistersRequest) req).getReference(), equalTo(3));
		assertThat("Count", ((ReadMultipleRegistersRequest) req).getWordCount(), equalTo(4));
	}

	@Test
	public void readRequestHoldingNotHeadless() {
		ModbusRequest req = ModbusTransactionUtils
				.modbusReadRequest(ModbusReadFunction.ReadHoldingRegister, 2, false, 3, 4);
		assertThat("Function code", req.getFunctionCode(), equalTo(3));
		assertThat("Headless", req.isHeadless(), equalTo(false));
	}

	@Test
	public void readRequestInput() {
		ModbusRequest req = ModbusTransactionUtils
				.modbusReadRequest(ModbusReadFunction.ReadInputRegister, 2, true, 3, 4);
		assertThat("Function code", req.getFunctionCode(), equalTo(4));
		assertThat("Unit ID", req.getUnitID(), equalTo(2));
		assertThat("Headless", req.isHeadless(), equalTo(true));
		assertThat("Instance class", req, instanceOf(ReadInputRegistersRequest.class));
		assertThat("Starting address", ((ReadInputRegistersRequest) req).getReference(), equalTo(3));
		assertThat("Count", ((ReadInputRegistersRequest) req).getWordCount(), equalTo(4));
	}

	@Test
	public void readBytes() throws ModbusException {
		// given
		ModbusTransaction trans = EasyMock.createMock(ModbusTransaction.class);

		Capture<ModbusRequest> requestCaptor = new Capture<>();
		trans.setRequest(capture(requestCaptor));
		trans.execute();

		Register[] regs = new Register[] { new SimpleRegister(0x1234), new SimpleRegister(0x4567),
				new SimpleRegister(0x8900) };
		ReadMultipleRegistersResponse res = new ReadMultipleRegistersResponse(regs);
		expect(trans.getResponse()).andReturn(res);

		// when
		replay(trans);
		byte[] result = ModbusTransactionUtils.readBytes(trans, 1, true,
				ModbusReadFunction.ReadHoldingRegister, 0, 3);

		verify(trans);
		assertThat("Result size 2x reg read size", result.length, equalTo(6));
		assertThat("Result bytes", objectArray(result), arrayContaining((byte) 0x12, (byte) 0x34,
				(byte) 0x45, (byte) 0x67, (byte) 0x89, (byte) 0x00));
	}

	private Register[] registersForBytes(final byte[] data) {
		Register[] regs = new Register[(int) Math.ceil(data.length / (double) 2)];
		for ( int i = 0, w = 0, stop = data.length - 1; i <= stop; i += 2, w++ ) {
			regs[w] = new SimpleRegister(data[i], i < stop ? data[i + 1] : 0);
		}
		return regs;
	}

	@Test
	public void readAsciiString() throws Exception {
		// given
		ModbusTransaction trans = EasyMock.createMock(ModbusTransaction.class);

		Capture<ModbusRequest> requestCaptor = new Capture<>();
		trans.setRequest(capture(requestCaptor));
		trans.execute();

		final String s = "Hello, world.";

		Register[] regs = registersForBytes(s.getBytes("US-ASCII"));
		ReadMultipleRegistersResponse res = new ReadMultipleRegistersResponse(regs);
		expect(trans.getResponse()).andReturn(res);

		// when
		replay(trans);
		String result = ModbusTransactionUtils.readString(trans, 1, true,
				ModbusReadFunction.ReadHoldingRegister, 0, regs.length, true, ByteUtils.ASCII);

		verify(trans);
		assertThat("Result ", result, equalTo(s));
	}

	@Test
	public void readUtf8String() throws Exception {
		// given
		ModbusTransaction trans = EasyMock.createMock(ModbusTransaction.class);

		Capture<ModbusRequest> requestCaptor = new Capture<>();
		trans.setRequest(capture(requestCaptor));
		trans.execute();

		final String s = "\u2766, world.";

		Register[] regs = registersForBytes(s.getBytes("UTF-8"));
		ReadMultipleRegistersResponse res = new ReadMultipleRegistersResponse(regs);
		expect(trans.getResponse()).andReturn(res);

		// when
		replay(trans);
		String result = ModbusTransactionUtils.readString(trans, 1, true,
				ModbusReadFunction.ReadHoldingRegister, 0, regs.length, true, ByteUtils.UTF8);

		verify(trans);
		assertThat("Result ", result, equalTo(s));
	}
}

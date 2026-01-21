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

import static net.solarnetwork.io.modbus.ModbusByteUtils.encodeHexString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.io.modbus.ModbusErrorCode;
import net.solarnetwork.io.modbus.ModbusFunctionCode;
import net.solarnetwork.io.modbus.ModbusMessage;
import net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage;
import net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage;
import net.solarnetwork.node.io.modbus.server.domain.ModbusRegisterData;
import net.solarnetwork.node.io.modbus.server.impl.ModbusConnectionHandler;

/**
 * Test cases for the {@link ModbusConnectionHandler} class.
 *
 * @author matt
 * @version 2.1
 */
public class ModbusConnectionHandlerTests implements Consumer<net.solarnetwork.io.modbus.ModbusMessage> {

	private static final int DEFAULT_UNIT_ID = 2;

	private ConcurrentMap<Integer, ModbusRegisterData> units;
	private ModbusRegisterData registers;
	private ModbusConnectionHandler handler;

	private net.solarnetwork.io.modbus.ModbusMessage msg;

	@Before
	public void setup() {
		units = new ConcurrentHashMap<>(1, 0.9f, 1);
		registers = new ModbusRegisterData();
		units.put(DEFAULT_UNIT_ID, registers);
		handler = new ModbusConnectionHandler(units, () -> "Test");
		msg = null;
	}

	@Override
	public void accept(net.solarnetwork.io.modbus.ModbusMessage t) {
		msg = t;
	}

	@Test
	public void readCoil() throws Exception {
		// GIVEN
		final int address = 3;
		final int count = 1;
		BitsModbusMessage req = BitsModbusMessage.readCoilsRequest(DEFAULT_UNIT_ID, address, count);

		// WHEN
		handler.accept(req, this);

		// THEN
		net.solarnetwork.io.modbus.BitsModbusMessage res = msg
				.unwrap(net.solarnetwork.io.modbus.BitsModbusMessage.class);
		assertThat("Response is bits", res, is(notNullValue()));

		assertThat("Response function code", res.getFunction().functionCode(),
				is(equalTo(ModbusFunctionCode.ReadCoils)));
		assertThat("Response unit ID", res.getUnitId(), is(equalTo(DEFAULT_UNIT_ID)));
		assertThat("Response address", res.getAddress(), is(equalTo(address)));
		assertThat("Response data", res.getBits(), is(equalTo(BigInteger.ZERO)));
	}

	@Test
	public void readCoil_on() throws Exception {
		// GIVEN
		final int address = 22;
		final int count = 1;
		BitsModbusMessage req = BitsModbusMessage.readCoilsRequest(DEFAULT_UNIT_ID, address, count);

		registers.writeCoil(address, true);

		// WHEN
		handler.accept(req, this);

		// THEN
		net.solarnetwork.io.modbus.BitsModbusMessage res = msg
				.unwrap(net.solarnetwork.io.modbus.BitsModbusMessage.class);
		assertThat("Response is bits", res, is(notNullValue()));

		assertThat("Response function code", res.getFunction().functionCode(),
				is(equalTo(ModbusFunctionCode.ReadCoils)));
		assertThat("Response unit ID", res.getUnitId(), is(equalTo(DEFAULT_UNIT_ID)));
		assertThat("Response address", res.getAddress(), is(equalTo(address)));
		assertThat("Response data", res.getBits(), is(equalTo(BigInteger.ONE)));
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

		final int address = 0;
		final int count = 11;
		BitsModbusMessage req = BitsModbusMessage.readCoilsRequest(DEFAULT_UNIT_ID, address, count);

		// WHEN
		handler.accept(req, this);

		// THEN
		net.solarnetwork.io.modbus.BitsModbusMessage res = msg
				.unwrap(net.solarnetwork.io.modbus.BitsModbusMessage.class);
		assertThat("Response is bits", res, is(notNullValue()));

		assertThat("Response function code", res.getFunction().functionCode(),
				is(equalTo(ModbusFunctionCode.ReadCoils)));
		assertThat("Response unit ID", res.getUnitId(), is(equalTo(DEFAULT_UNIT_ID)));
		assertThat("Response address", res.getAddress(), is(equalTo(address)));
		assertThat("Response data", res.getBits(), is(equalTo(BigInteger.valueOf(0x6E5L))));
	}

	@Test
	public void readDiscrete() throws Exception {
		// GIVEN
		final int address = 0;
		final int count = 1;
		BitsModbusMessage req = BitsModbusMessage.readDiscretesRequest(DEFAULT_UNIT_ID, address, count);

		// WHEN
		handler.accept(req, this);

		// THEN
		net.solarnetwork.io.modbus.BitsModbusMessage res = msg
				.unwrap(net.solarnetwork.io.modbus.BitsModbusMessage.class);
		assertThat("Response is bits", res, is(notNullValue()));

		assertThat("Response function code", res.getFunction().functionCode(),
				is(equalTo(ModbusFunctionCode.ReadDiscreteInputs)));
		assertThat("Response unit ID", res.getUnitId(), is(equalTo(DEFAULT_UNIT_ID)));
		assertThat("Response address", res.getAddress(), is(equalTo(address)));
		assertThat("Response data", res.getBits(), is(equalTo(BigInteger.ZERO)));
	}

	@Test
	public void readDiscrete_on() throws Exception {
		// GIVEN
		final int address = 22;
		final int count = 1;
		BitsModbusMessage req = BitsModbusMessage.readDiscretesRequest(DEFAULT_UNIT_ID, address, count);

		registers.writeDiscrete(address, true);

		// WHEN
		handler.accept(req, this);

		// THEN
		net.solarnetwork.io.modbus.BitsModbusMessage res = msg
				.unwrap(net.solarnetwork.io.modbus.BitsModbusMessage.class);
		assertThat("Response is bits", res, is(notNullValue()));

		assertThat("Response function code", res.getFunction().functionCode(),
				is(equalTo(ModbusFunctionCode.ReadDiscreteInputs)));
		assertThat("Response unit ID", res.getUnitId(), is(equalTo(DEFAULT_UNIT_ID)));
		assertThat("Response address", res.getAddress(), is(equalTo(address)));
		assertThat("Response data", res.getBits(), is(equalTo(BigInteger.ONE)));
	}

	@Test
	public void readDiscretes() throws Exception {
		// GIVEN
		final int address = 0;
		final int count = 11;
		BitsModbusMessage req = BitsModbusMessage.readDiscretesRequest(DEFAULT_UNIT_ID, address, count);

		registers.writeDiscrete(0, true);
		registers.writeDiscrete(2, true);
		registers.writeDiscrete(5, true);
		registers.writeDiscrete(6, true);
		registers.writeDiscrete(7, true);
		registers.writeDiscrete(9, true);
		registers.writeDiscrete(10, true);

		// WHEN
		handler.accept(req, this);

		// THEN
		net.solarnetwork.io.modbus.BitsModbusMessage res = msg
				.unwrap(net.solarnetwork.io.modbus.BitsModbusMessage.class);
		assertThat("Response is bits", res, is(notNullValue()));

		assertThat("Response function code", res.getFunction().functionCode(),
				is(equalTo(ModbusFunctionCode.ReadDiscreteInputs)));
		assertThat("Response unit ID", res.getUnitId(), is(equalTo(DEFAULT_UNIT_ID)));
		assertThat("Response address", res.getAddress(), is(equalTo(address)));
		assertThat("Response data", res.getBits(), is(equalTo(BigInteger.valueOf(0x6E5L))));
	}

	@Test
	public void readHolding() throws Exception {
		// GIVEN
		final int address = 17;
		final int count = 1;
		RegistersModbusMessage req = RegistersModbusMessage.readHoldingsRequest(DEFAULT_UNIT_ID, address,
				count);

		registers.writeHolding(address, (short) 123);

		// WHEN
		handler.accept(req, this);

		// THEN
		net.solarnetwork.io.modbus.RegistersModbusMessage res = msg
				.unwrap(net.solarnetwork.io.modbus.RegistersModbusMessage.class);
		assertThat("Response is registers", res, is(notNullValue()));

		assertThat("Response function code", res.getFunction().functionCode(),
				is(equalTo(ModbusFunctionCode.ReadHoldingRegisters)));
		assertThat("Response unit ID", res.getUnitId(), is(equalTo(DEFAULT_UNIT_ID)));
		assertThat("Response address", res.getAddress(), is(equalTo(address)));
		assertThat("Response data", encodeHexString(res.dataCopy(), 0, 2), is(equalTo("007B")));
	}

	@Test
	public void readHoldings() throws Exception {
		// GIVEN
		final int address = 17;
		final int count = 3;
		RegistersModbusMessage req = RegistersModbusMessage.readHoldingsRequest(DEFAULT_UNIT_ID, address,
				count);

		registers.writeHolding(17, (short) 123);
		registers.writeHolding(19, (short) 0xFEDC);

		// WHEN
		handler.accept(req, this);

		// THEN
		net.solarnetwork.io.modbus.RegistersModbusMessage res = msg
				.unwrap(net.solarnetwork.io.modbus.RegistersModbusMessage.class);
		assertThat("Response is registers", res, is(notNullValue()));

		assertThat("Response function code", res.getFunction().functionCode(),
				is(equalTo(ModbusFunctionCode.ReadHoldingRegisters)));
		assertThat("Response unit ID", res.getUnitId(), is(equalTo(DEFAULT_UNIT_ID)));
		assertThat("Response address", res.getAddress(), is(equalTo(address)));
		assertThat("Response data", encodeHexString(res.dataCopy(), 0, 6), is(equalTo("007B0000FEDC")));
	}

	@Test
	public void readHoldings_throttled() throws Exception {
		// GIVEN
		final int address = 17;
		final int count = 1;
		RegistersModbusMessage req1 = RegistersModbusMessage.readHoldingsRequest(DEFAULT_UNIT_ID,
				address, count);
		RegistersModbusMessage req2 = RegistersModbusMessage.readHoldingsRequest(DEFAULT_UNIT_ID,
				address + 2, count);

		handler.setRequestThrottle(1000);
		registers.writeHolding(address, (short) 0x007B);
		registers.writeHolding(address + 2, (short) 0xFEDC);

		// WHEN
		handler.accept(req1, this);

		final long start = System.currentTimeMillis();
		ModbusMessage msg1 = this.msg;
		this.msg = null;

		handler.accept(req2, this);

		final long end = System.currentTimeMillis();
		ModbusMessage msg2 = this.msg;

		// THEN
		net.solarnetwork.io.modbus.RegistersModbusMessage res = msg1
				.unwrap(net.solarnetwork.io.modbus.RegistersModbusMessage.class);
		assertThat("Response is registers", res, is(notNullValue()));

		assertThat("Response function code", res.getFunction().functionCode(),
				is(equalTo(ModbusFunctionCode.ReadHoldingRegisters)));
		assertThat("Response unit ID", res.getUnitId(), is(equalTo(DEFAULT_UNIT_ID)));
		assertThat("Response address", res.getAddress(), is(equalTo(address)));
		assertThat("Response data", encodeHexString(res.dataCopy(), 0, 2), is(equalTo("007B")));

		res = msg2.unwrap(net.solarnetwork.io.modbus.RegistersModbusMessage.class);
		assertThat("Response 2 is registers", res, is(notNullValue()));

		assertThat("Response 2 function code", res.getFunction().functionCode(),
				is(equalTo(ModbusFunctionCode.ReadHoldingRegisters)));
		assertThat("Response 2 unit ID", res.getUnitId(), is(equalTo(DEFAULT_UNIT_ID)));
		assertThat("Response 2 address", res.getAddress(), is(equalTo(address + 2)));
		assertThat("Response 2 data", encodeHexString(res.dataCopy(), 0, 2), is(equalTo("FEDC")));

		assertThat("Response 2 throttled", Math.abs(end - start - handler.getRequestThrottle()),
				is(lessThanOrEqualTo(100L)));
	}

	@Test
	public void readInput() throws Exception {
		// GIVEN
		final int address = 17;
		final int count = 1;
		RegistersModbusMessage req = RegistersModbusMessage.readInputsRequest(DEFAULT_UNIT_ID, address,
				count);

		registers.writeInput(address, (short) 123);

		// WHEN
		handler.accept(req, this);

		// THEN
		net.solarnetwork.io.modbus.RegistersModbusMessage res = msg
				.unwrap(net.solarnetwork.io.modbus.RegistersModbusMessage.class);
		assertThat("Response is registers", res, is(notNullValue()));

		assertThat("Response function code", res.getFunction().functionCode(),
				is(equalTo(ModbusFunctionCode.ReadInputRegisters)));
		assertThat("Response unit ID", res.getUnitId(), is(equalTo(DEFAULT_UNIT_ID)));
		assertThat("Response address", res.getAddress(), is(equalTo(address)));
		assertThat("Response data", encodeHexString(res.dataCopy(), 0, 2), is(equalTo("007B")));
	}

	@Test
	public void readInputs() throws Exception {
		// GIVEN
		final int address = 17;
		final int count = 3;
		RegistersModbusMessage req = RegistersModbusMessage.readInputsRequest(DEFAULT_UNIT_ID, address,
				count);

		registers.writeInput(17, (short) 123);
		registers.writeInput(19, (short) 0xFEDC);

		// WHEN
		handler.accept(req, this);

		// THEN
		net.solarnetwork.io.modbus.RegistersModbusMessage res = msg
				.unwrap(net.solarnetwork.io.modbus.RegistersModbusMessage.class);
		assertThat("Response is registers", res, is(notNullValue()));

		assertThat("Response function code", res.getFunction().functionCode(),
				is(equalTo(ModbusFunctionCode.ReadInputRegisters)));
		assertThat("Response unit ID", res.getUnitId(), is(equalTo(DEFAULT_UNIT_ID)));
		assertThat("Response address", res.getAddress(), is(equalTo(address)));
		assertThat("Response data", encodeHexString(res.dataCopy(), 0, 6), is(equalTo("007B0000FEDC")));
	}

	@Test
	public void readInput_unknownUnitId() throws Exception {
		// GIVEN
		final int unitId = 123;
		final int address = 17;
		final int count = 1;
		var req = RegistersModbusMessage.readInputsRequest(unitId, address, count);

		// WHEN
		handler.accept(req, this);

		// THEN
		net.solarnetwork.io.modbus.RegistersModbusMessage res = msg
				.unwrap(net.solarnetwork.io.modbus.RegistersModbusMessage.class);

		// @formatter:off
		then(res)
			.as("Response is registers")
			.isNotNull()
			.as("Response function code matches request value")
			.returns(ModbusFunctionCode.ReadInputRegisters, from(r -> r.getFunction().functionCode()))
			.as("Response unit ID matches request value")
			.returns(unitId, from(net.solarnetwork.io.modbus.RegistersModbusMessage::getUnitId))
			.as("Response address matches request value")
			.returns(address, from(net.solarnetwork.io.modbus.RegistersModbusMessage::getAddress))
			.as("Response data is zeros")
			.returns(new byte[] {0, 0}, from(net.solarnetwork.io.modbus.RegistersModbusMessage::dataCopy))
			;
		// @formatter:on
	}

	@Test
	public void readInput_unknownUnitId_restricted() throws Exception {
		// GIVEN
		final int unitId = 123;
		final int address = 17;
		final int count = 1;
		var req = RegistersModbusMessage.readInputsRequest(unitId, address, count);

		handler.setRestrictUnitIds(true);

		// WHEN
		handler.accept(req, this);

		// THEN
		// @formatter:off
		then(msg)
			.as("Response provided")
			.isNotNull()
			.as("Response function code matches request value")
			.returns(ModbusFunctionCode.ReadInputRegisters, from(r -> r.getFunction().functionCode()))
			.as("Response unit ID matches request value")
			.returns(unitId, from(net.solarnetwork.io.modbus.ModbusMessage::getUnitId))
			.as("Response is exception")
			.returns(true, from(net.solarnetwork.io.modbus.ModbusMessage::isException))
			.as("Illegal error returned")
			.returns(ModbusErrorCode.IllegalDataAddress, from(net.solarnetwork.io.modbus.ModbusMessage::getError))
			;
		// @formatter:on
	}

	@Test
	public void readInput_unknownAddress() throws Exception {
		// GIVEN
		final int unitId = 123; // create unit dynamically
		final int address = 17;
		final int count = 1;
		var req = RegistersModbusMessage.readInputsRequest(unitId, address, count);

		// WHEN
		handler.accept(req, this);

		// THEN
		net.solarnetwork.io.modbus.RegistersModbusMessage res = msg
				.unwrap(net.solarnetwork.io.modbus.RegistersModbusMessage.class);

		// @formatter:off
		then(res)
			.as("Response is registers")
			.isNotNull()
			.as("Response function code matches request value")
			.returns(ModbusFunctionCode.ReadInputRegisters, from(r -> r.getFunction().functionCode()))
			.as("Response unit ID matches request value")
			.returns(unitId, from(net.solarnetwork.io.modbus.RegistersModbusMessage::getUnitId))
			.as("Response address matches request value")
			.returns(address, from(net.solarnetwork.io.modbus.RegistersModbusMessage::getAddress))
			.as("Response data is zeros")
			.returns(new byte[] {0, 0}, from(net.solarnetwork.io.modbus.RegistersModbusMessage::dataCopy))
			;
		// @formatter:on
	}

	@Test
	public void readInput_unknownAddress_restricted() throws Exception {
		// GIVEN
		final int unitId = 123; // create unit dynamically
		final int address = 17;
		final int count = 1;
		var req = RegistersModbusMessage.readInputsRequest(unitId, address, count);

		handler.setRestrictAddresses(true);

		// WHEN
		handler.accept(req, this);

		// THEN
		// @formatter:off
		then(msg)
			.as("Response provided")
			.isNotNull()
			.as("Response function code matches request value")
			.returns(ModbusFunctionCode.ReadInputRegisters, from(r -> r.getFunction().functionCode()))
			.as("Response unit ID matches request value")
			.returns(unitId, from(net.solarnetwork.io.modbus.ModbusMessage::getUnitId))
			.as("Response is exception")
			.returns(true, from(net.solarnetwork.io.modbus.ModbusMessage::isException))
			.as("Illegal error returned")
			.returns(ModbusErrorCode.IllegalDataAddress, from(net.solarnetwork.io.modbus.ModbusMessage::getError))
			;
		// @formatter:on
	}

}

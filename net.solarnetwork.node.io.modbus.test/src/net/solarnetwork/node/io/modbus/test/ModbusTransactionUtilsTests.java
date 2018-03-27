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

package net.solarnetwork.node.io.modbus.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusTransactionUtils;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;

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

}

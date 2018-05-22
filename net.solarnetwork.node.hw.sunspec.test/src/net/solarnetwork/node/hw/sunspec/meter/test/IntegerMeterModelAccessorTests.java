/* ==================================================================
 * IntegerMeterModelAccessorTests.java - 22/05/2018 1:36:13 PM
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

package net.solarnetwork.node.hw.sunspec.meter.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelRegister;
import net.solarnetwork.node.hw.sunspec.meter.IntegerMeterModelAccessor;
import net.solarnetwork.node.hw.sunspec.meter.IntegerMeterModelRegister;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelAccessor;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataTests;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Test cases for the {@link IntegerMeterModelAccessor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class IntegerMeterModelAccessorTests {

	// @formatter:off	
	private static final int[] INT_METER_MODEL_69 = new int[] {
			0x00CB,
			0x0069,
			0x0038,
			0x0013,
			0x0012,
			0x0012,
			0xFFFF,
			0x04D0,
			0x0531,
			0x03E6,
			0x055A,
			0x0847,
			0x0848,
			0x0840,
			0x084E,
			0xFFFF,
			0x1768,
			0xFFFE,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0002,
			0x0007,
			0x0003,
			0x0002,
			0x0003,
			0x0002,
			0x0007,
			0x0002,
			0x0002,
			0x0003,
			0x0002,
			0x000A,
			0xFAC2,
			0x00FC,
			0x0497,
			0xFFFC,
			0x0000,
			0x2A94,
			0x0000,
			0x276A,
			0x0000,
			0x0150,
			0x0000,
			0x01D9,
			0x0098,
			0xD172,
			0x0035,
			0x7C10,
			0x0029,
			0xAB62,
			0x0039,
			0xAA00,
			0x0002,
			0x0000,
			0x9C67,
			0x0001,
			0x3324,
			0x0000,
			0x02A4,
			0x0000,
			0x0315,
			0x009F,
			0xE2B1,
			0x0038,
			0x8E20,
			0x002B,
			0x9D4B,
			0x003C,
			0x45DC,
			0x0002,
			0x0018,
			0xBDFF,
			0x000E,
			0x7389,
			0x0005,
			0x37F4,
			0x0005,
			0x1281,
			0x0001,
			0x31B4,
			0x0001,
			0x2EC0,
			0x0000,
			0x0171,
			0x0000,
			0x0182,
			0x0000,
			0x0051,
			0x0000,
			0x0050,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0006,
			0xC73F,
			0x0000,
			0x0000,
			0x0001,
			0x9307,
			0x0005,
			0x3437,
			0x0002,
			0x0000,
			0x0018,
	};
	
	private static final int[] END_OF_MODEL_176 = new int[] {
			0xFFFF,
			0x0000,
	};
	
	// @formatter:on

	private static final Logger log = LoggerFactory.getLogger(IntegerMeterModelAccessorTests.class);

	private ModelData getTestDataInstance() {
		final int baseAddress = ModelRegister.BaseAddress.getAddress();
		ModelData data = new ModelData(baseAddress + 2);
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(ModelDataTests.COMMON_MODEL_00, baseAddress);
				m.saveDataArray(INT_METER_MODEL_69, baseAddress + 69);
				m.saveDataArray(END_OF_MODEL_176, baseAddress + 176);
				return true;
			}
		});
		data.addModel(IntegerMeterModelAccessor.FIXED_BLOCK_LENGTH, new IntegerMeterModelAccessor(data,
				40069, MeterModelId.WyeConnectThreePhaseMeterInteger));
		return data;
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void commonModelProperties() {
		CommonModelAccessor data = getTestDataInstance();
		assertThat("Manufacturer", data.getManufacturer(), equalTo("Veris Industries"));
		assertThat("Model name", data.getModelName(), equalTo("E51C2"));
		assertThat("Options", data.getOptions(), equalTo("None"));
		assertThat("Version", data.getVersion(), equalTo("2.103"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("4E390476"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(10));
	}

	@Test
	public void block() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Model base address", model.getBaseAddress(), equalTo(40069));
		assertThat("Model block address", model.getBlockAddress(), equalTo(40071));
		assertThat("Model ID", model.getModelId(),
				equalTo(MeterModelId.WyeConnectThreePhaseMeterInteger));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(105));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(0));
		assertThat("Model length", model.getModelLength(), equalTo(105));
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(0));
	}

	@Test
	public void current() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getCurrent(), equalTo(5.6f));

		IntegerMeterModelAccessor imm = (IntegerMeterModelAccessor) model;
		assertThat("Phase A", imm.getCurrentValue(IntegerMeterModelRegister.CurrentPhaseA),
				equalTo(1.9f));
		assertThat("Phase B", imm.getCurrentValue(IntegerMeterModelRegister.CurrentPhaseB),
				equalTo(1.8f));
		assertThat("Phase C", imm.getCurrentValue(IntegerMeterModelRegister.CurrentPhaseC),
				equalTo(1.8f));
	}

}

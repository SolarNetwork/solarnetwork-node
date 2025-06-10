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

import static net.solarnetwork.domain.AcPhase.PhaseA;
import static net.solarnetwork.domain.AcPhase.PhaseB;
import static net.solarnetwork.domain.AcPhase.PhaseC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import java.io.IOException;
import java.util.BitSet;
import java.util.Set;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
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
 * @version 1.1
 */
public class IntegerMeterModelAccessorTests {

	// @formatter:off
	public static final short[] INT_METER_MODEL_HEADER_69 = new short[] {
			0x00CB,
			0x0069,
	};
	
	public static final short[] INT_METER_MODEL_71 = new short[] {
			0x0038,
			0x0013,
			0x0012,
			0x0012,
			(short)0xFFFF,
			0x04D0,
			0x0531,
			0x03E6,
			0x055A,
			0x0847,
			0x0848,
			0x0840,
			0x084E,
			(short)0xFFFF,
			0x1768,
			(short)0xFFFE,
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
			(short)0xFAC2,
			0x00FC,
			0x0497,
			(short)0xFFFC,
			0x0000,
			0x2A94,
			0x0000,
			0x276A,
			0x0000,
			0x0150,
			0x0000,
			0x01D9,
			0x0098,
			(short)0xD172,
			0x0035,
			0x7C10,
			0x0029,
			(short)0xAB62,
			0x0039,
			(short)0xAA00,
			0x0002,
			0x0000,
			(short)0x9C67,
			0x0001,
			0x3324,
			0x0000,
			0x02A4,
			0x0000,
			0x0315,
			0x009F,
			(short)0xE2B1,
			0x0038,
			(short)0x8E20,
			0x002B,
			(short)0x9D4B,
			0x003C,
			0x45DC,
			0x0002,
			0x0018,
			(short)0xBDFF,
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
			(short)0xC73F,
			0x0000,
			0x0000,
			0x0001,
			(short)0x9307,
			0x0005,
			0x3437,
			0x0002,
			0x0000,
			0x0018,
	};
	
	public static final short[] END_OF_MODEL_176 = new short[] {
			(short)0xFFFF,
			0x0000,
	};
	
	// @formatter:on

	private static final Logger log = LoggerFactory.getLogger(IntegerMeterModelAccessorTests.class);

	private ModelData getTestDataInstance() {
		final int baseAddress = ModelRegister.BaseAddress.getAddress();
		ModelData data = new ModelData(baseAddress + 2);
		try {
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataArray(ModelDataTests.COMMON_MODEL_02, baseAddress + 2);
					m.saveDataArray(INT_METER_MODEL_HEADER_69, baseAddress + 69);
					m.saveDataArray(INT_METER_MODEL_71, baseAddress + 71);
					m.saveDataArray(END_OF_MODEL_176, baseAddress + 176);
					return true;
				}
			});
			data.addModel(IntegerMeterModelAccessor.FIXED_BLOCK_LENGTH, new IntegerMeterModelAccessor(
					data, 40069, MeterModelId.WyeConnectThreePhaseMeterInteger));
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
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
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		MeterModelAccessor meterAccessor = data.findTypedModel(MeterModelAccessor.class);
		assertThat(meterAccessor, instanceOf(IntegerMeterModelAccessor.class));
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
	public void events() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		Set<ModelEvent> events = model.getEvents();
		BitSet bitset = new BitSet();
		events.stream().mapToInt(ModelEvent::getIndex).forEach(i -> bitset.set(i));
		assertThat(bitset.cardinality(), equalTo(2));
		assertThat(bitset.get(3), equalTo(true));
		assertThat(bitset.get(4), equalTo(true));
	}

	@Test
	public void current() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getCurrent(), equalTo(5.6f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getCurrent(), equalTo(1.9f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getCurrent(), equalTo(1.8f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getCurrent(), equalTo(1.8f));
	}

	@Test
	public void voltageLN() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Average", model.getVoltage(), equalTo(123.2f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getVoltage(), equalTo(132.9f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getVoltage(), equalTo(99.8f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getVoltage(), equalTo(137.0f));
	}

	@Test
	public void voltageLL() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		IntegerMeterModelAccessor imm = (IntegerMeterModelAccessor) model;
		assertThat("Average", imm.getVoltageValue(IntegerMeterModelRegister.VoltageLineLineAverage),
				equalTo(211.9f));
		assertThat("Phase A", imm.getVoltageValue(IntegerMeterModelRegister.VoltagePhaseAPhaseB),
				equalTo(212.0f));
		assertThat("Phase B", imm.getVoltageValue(IntegerMeterModelRegister.VoltagePhaseBPhaseC),
				equalTo(211.2f));
		assertThat("Phase C", imm.getVoltageValue(IntegerMeterModelRegister.VoltagePhaseCPhaseA),
				equalTo(212.6f));
	}

	@Test
	public void frequency() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Frequency", model.getFrequency(), equalTo(59.92f));
	}

	@Test
	public void activePower() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getActivePower(), equalTo(0));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActivePower(), equalTo(0));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActivePower(), equalTo(0));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActivePower(), equalTo(0));
	}

	@Test
	public void apparentPower() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getApparentPower(), equalTo(700));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentPower(), equalTo(300));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentPower(), equalTo(200));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentPower(), equalTo(300));
	}

	@Test
	public void reactivePower() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getReactivePower(), equalTo(700));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactivePower(), equalTo(200));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactivePower(), equalTo(200));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactivePower(), equalTo(300));
	}

	@Test
	public void powerFactor() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Average", model.getPowerFactor(), equalTo(0.001f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getPowerFactor(), equalTo(-0.1342f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getPowerFactor(), equalTo(0.0252f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getPowerFactor(), equalTo(0.1175f));
	}

	@Test
	public void activeEnergyExport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getActiveEnergyExported(), equalTo(1090000L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActiveEnergyExported(),
				equalTo(1009000L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActiveEnergyExported(), equalTo(33600L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActiveEnergyExported(), equalTo(47300L));
	}

	@Test
	public void activeEnergyExportReversed() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		model = model.reversed();
		assertThat("Total", model.getActiveEnergyImported(), equalTo(1090000L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActiveEnergyImported(),
				equalTo(1009000L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActiveEnergyImported(), equalTo(33600L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActiveEnergyImported(), equalTo(47300L));
	}

	@Test
	public void activeEnergyImport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getActiveEnergyImported(), equalTo(1001509000L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActiveEnergyImported(),
				equalTo(350516800L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActiveEnergyImported(),
				equalTo(273085000L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActiveEnergyImported(),
				equalTo(377907200L));
	}

	@Test
	public void activeEnergyImportReversed() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		model = model.reversed();
		assertThat("Total", model.getActiveEnergyExported(), equalTo(1001509000L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActiveEnergyExported(),
				equalTo(350516800L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActiveEnergyExported(),
				equalTo(273085000L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActiveEnergyExported(),
				equalTo(377907200L));
	}

	@Test
	public void apparentEnergyExport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getApparentEnergyExported(), equalTo(4003900L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentEnergyExported(),
				equalTo(7862800L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentEnergyExported(),
				equalTo(67600L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentEnergyExported(),
				equalTo(78900L));
	}

	@Test
	public void apparentEnergyExportReversed() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		model = model.reversed();
		assertThat("Total", model.getApparentEnergyImported(), equalTo(4003900L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentEnergyImported(),
				equalTo(7862800L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentEnergyImported(),
				equalTo(67600L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentEnergyImported(),
				equalTo(78900L));
	}

	@Test
	public void apparentEnergyImport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getApparentEnergyImported(), equalTo(1047825700L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentEnergyImported(),
				equalTo(370640000L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentEnergyImported(),
				equalTo(285831500L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentEnergyImported(),
				equalTo(395004400L));
	}

	@Test
	public void apparentEnergyImportReversed() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		model = model.reversed();
		assertThat("Total", model.getApparentEnergyExported(), equalTo(1047825700L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentEnergyExported(),
				equalTo(370640000L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentEnergyExported(),
				equalTo(285831500L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentEnergyExported(),
				equalTo(395004400L));
	}

	@Test
	public void reactiveEnergyImport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getReactiveEnergyImported(), equalTo((0x18BDFFL + 0x131B4L) * 100L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactiveEnergyImported(),
				equalTo((0xE7389L + 0x12EC0L) * 100L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactiveEnergyImported(),
				equalTo((0x537F4L + 0x171L) * 100L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactiveEnergyImported(),
				equalTo((0x51281L + 0x182) * 100L));
	}

	@Test
	public void reactiveEnergyImportReversed() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		model = model.reversed();
		assertThat("Total", model.getReactiveEnergyExported(), equalTo((0x18BDFFL + 0x131B4L) * 100L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactiveEnergyExported(),
				equalTo((0xE7389L + 0x12EC0L) * 100L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactiveEnergyExported(),
				equalTo((0x537F4L + 0x171L) * 100L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactiveEnergyExported(),
				equalTo((0x51281L + 0x182) * 100L));
	}

	@Test
	public void reactiveEnergyExport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getReactiveEnergyExported(), equalTo((0x51L + 0x6C73FL) * 100L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactiveEnergyExported(),
				equalTo((0x50L + 0x0L) * 100L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactiveEnergyExported(),
				equalTo((0x0L + 0x19307L) * 100L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactiveEnergyExported(),
				equalTo((0x0L + 0x53437L) * 100L));
	}

	@Test
	public void reactiveEnergyExportReversed() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		model = model.reversed();
		assertThat("Total", model.getReactiveEnergyImported(), equalTo((0x51L + 0x6C73FL) * 100L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactiveEnergyImported(),
				equalTo((0x50L + 0x0L) * 100L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactiveEnergyImported(),
				equalTo((0x0L + 0x19307L) * 100L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactiveEnergyImported(),
				equalTo((0x0L + 0x53437L) * 100L));
	}
}

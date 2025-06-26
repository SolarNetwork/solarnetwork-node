/* ==================================================================
 * IntegerInverterModelAccessor_103_02Tests.java - 8/10/2018 7:06:15 AM
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

package net.solarnetwork.node.hw.sunspec.inverter.test;

import static net.solarnetwork.domain.AcPhase.PhaseA;
import static net.solarnetwork.domain.AcPhase.PhaseB;
import static net.solarnetwork.domain.AcPhase.PhaseC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.util.Set;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.OperatingState;
import net.solarnetwork.node.hw.sunspec.inverter.IntegerInverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.IntegerInverterModelRegister;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelId;
import net.solarnetwork.node.hw.sunspec.inverter.InverterOperatingState;
import net.solarnetwork.node.hw.sunspec.meter.test.IntegerMeterModelAccessorTests;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Test cases for the {@link IntegerInverterModelAccessor} class.
 * 
 * @author matt
 * @version 1.1
 */
public class IntegerInverterModelAccessor_103_02Tests {

	private static final Logger log = LoggerFactory.getLogger(IntegerMeterModelAccessorTests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-103-02.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void commonModelProperties() {
		CommonModelAccessor data = getTestDataInstance();
		assertThat("Manufacturer", data.getManufacturer(), equalTo("SolarEdge"));
		assertThat("Model name", data.getModelName(), equalTo("SE33.3K"));
		assertThat("Options", data.getOptions(), equalTo(""));
		assertThat("Version", data.getVersion(), equalTo("0003.2251"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("7E1240CC"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(11));
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		InverterModelAccessor meterAccessor = data.findTypedModel(InverterModelAccessor.class);
		assertThat(meterAccessor, instanceOf(IntegerInverterModelAccessor.class));
	}

	@Test
	public void getTypedModel() {
		ModelData data = getTestDataInstance();
		InverterModelAccessor meterAccessor = data.getTypedModel();
		assertThat(meterAccessor, instanceOf(IntegerInverterModelAccessor.class));
	}

	@Test
	public void block() {
		InverterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Model base address", model.getBaseAddress(), equalTo(69));
		assertThat("Model block address", model.getBlockAddress(), equalTo(71));
		assertThat("Model ID", model.getModelId(), equalTo(InverterModelId.ThreePhaseInverterInteger));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(50));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(0));
		assertThat("Model length", model.getModelLength(), equalTo(50));
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(0));
	}

	@Test
	public void current() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Total", model.getCurrent(), equalTo(86.89f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getCurrent(), equalTo(28.95f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getCurrent(), equalTo(29.02f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getCurrent(), equalTo(28.92f));
	}

	@Test
	public void voltageLL() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		IntegerInverterModelAccessor imm = (IntegerInverterModelAccessor) model;
		assertThat("Phase AB", imm.getVoltageValue(IntegerInverterModelRegister.VoltagePhaseAPhaseB),
				equalTo(498.6f));
		assertThat("Phase BC", imm.getVoltageValue(IntegerInverterModelRegister.VoltagePhaseBPhaseC),
				equalTo(495.2f));
		assertThat("Phase CA", imm.getVoltageValue(IntegerInverterModelRegister.VoltagePhaseCPhaseA),
				equalTo(495.1f));
	}

	@Test
	public void voltageLN() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getVoltage(), equalTo(284.9f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getVoltage(), equalTo(285.1f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getVoltage(), equalTo(281.4f));
		assertThat("Average", (double) model.getVoltage(), closeTo(283.8, 0.01));
	}

	@Test
	public void activePower() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActivePower(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActivePower(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActivePower(), nullValue());
		assertThat("Total", model.getActivePower(), equalTo(24767));
	}

	@Test
	public void frequency() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Frequency", model.getFrequency(), equalTo(59.99f));
	}

	@Test
	public void apparentPower() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentPower(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentPower(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentPower(), nullValue());
		assertThat("Total", model.getApparentPower(), equalTo(24797));
	}

	@Test
	public void reactivePower() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactivePower(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactivePower(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactivePower(), nullValue());
		assertThat("Total", model.getReactivePower(), equalTo(-1235));
	}

	@Test
	public void powerFactor() {
		ModelData data = getTestDataInstance();
		InverterModelAccessor model = data.findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getPowerFactor(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getPowerFactor(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getPowerFactor(), nullValue());
		assertThat("Average", model.getPowerFactor(), equalTo(-0.9987f));
		assertThat("Integer PF flag", data.getMetadataValue(IntegerInverterModelAccessor.INTEGER_PF_PCT),
				equalTo(true));
	}

	@Test
	public void powerFactorWithDecimalScale() throws IOException {
		ModelData data = getTestDataInstance();
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				// force PF scale to -4
				m.saveDataArray(new int[] { 0xFFFC }, 92);
				return false;
			}
		});
		InverterModelAccessor model = data.findTypedModel(InverterModelAccessor.class);
		assertThat("Average", model.getPowerFactor(), equalTo(-0.9987f));
		assertThat("Integer PF flag", data.getMetadataValue(IntegerInverterModelAccessor.INTEGER_PF_PCT),
				nullValue());
	}

	@Test
	public void powerFactorWithIntegerScaleChangesToDecimal() throws IOException {
		ModelData data = getTestDataInstance();
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				// force PF value to 100e-2 to seem like decimal scale
				m.saveDataArray(new int[] { 0x0064 }, 91);
				return false;
			}
		});
		InverterModelAccessor model = data.findTypedModel(InverterModelAccessor.class);
		assertThat("Average", model.getPowerFactor(), equalTo(1.0f));
		assertThat("Integer PF flag", data.getMetadataValue(IntegerInverterModelAccessor.INTEGER_PF_PCT),
				nullValue());

		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				// force PF value to 1000e-2 to become integer scale
				m.saveDataArray(new int[] { 0x03E8 }, 91);
				return false;
			}
		});
		assertThat("Average", model.getPowerFactor(), equalTo(0.1f));
		assertThat("Integer PF flag", data.getMetadataValue(IntegerInverterModelAccessor.INTEGER_PF_PCT),
				equalTo(true));
	}

	@Test
	public void activeEnergyExport() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActiveEnergyExported(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActiveEnergyExported(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActiveEnergyExported(), nullValue());
		assertThat("Total", model.getActiveEnergyExported(), equalTo(17534384L));
	}

	@Test
	public void dcCurrent() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getDcCurrent(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getDcCurrent(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getDcCurrent(), nullValue());
		assertThat("Total", model.getDcCurrent(), equalTo(31.20f));
	}

	@Test
	public void dcVoltage() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getDcVoltage(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getDcVoltage(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getDcVoltage(), nullValue());
		assertThat("Average", (double) model.getDcVoltage(), closeTo(805.8, 0.01));
	}

	@Test
	public void dcPower() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getDcPower(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getDcPower(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getDcPower(), nullValue());
		assertThat("Total", model.getDcPower(), equalTo(25144));
	}

	@Test
	public void cabinetTemperature() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Cabinet temperature", model.getCabinetTemperature(), nullValue());
	}

	@Test
	public void heatSinkTemperature() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Heat sink temperature", model.getHeatSinkTemperature(), equalTo(55.31f));
	}

	@Test
	public void transformerTemperature() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Transformer temperature", model.getTransformerTemperature(), nullValue());
	}

	@Test
	public void otherTemperature() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Other temperature", model.getOtherTemperature(), nullValue());
	}

	@Test
	public void operatingState() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		OperatingState state = model.getOperatingState();
		assertThat("Operating state available", state, notNullValue());
		assertThat("State value", state.getCode(), equalTo(InverterOperatingState.Mppt.getCode()));
	}

	@Test
	public void events() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		Set<ModelEvent> events = model.getEvents();
		assertThat("No events", events, hasSize(0));
	}

}

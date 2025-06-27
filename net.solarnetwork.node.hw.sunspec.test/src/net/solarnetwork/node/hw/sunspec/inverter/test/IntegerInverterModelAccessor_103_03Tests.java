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

/**
 * Test cases for the {@link IntegerInverterModelAccessor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class IntegerInverterModelAccessor_103_03Tests {

	private static final Logger log = LoggerFactory.getLogger(IntegerMeterModelAccessorTests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-103-03.txt");
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
		assertThat("Total", model.getCurrent(), equalTo(3.54f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getCurrent(), equalTo(1.21f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getCurrent(), equalTo(1.15f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getCurrent(), equalTo(1.17f));
	}

	@Test
	public void voltageLL() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		IntegerInverterModelAccessor imm = (IntegerInverterModelAccessor) model;
		assertThat("Phase AB", imm.getVoltageValue(IntegerInverterModelRegister.VoltagePhaseAPhaseB),
				equalTo(493.8f));
		assertThat("Phase BC", imm.getVoltageValue(IntegerInverterModelRegister.VoltagePhaseBPhaseC),
				equalTo(489.5f));
		assertThat("Phase CA", imm.getVoltageValue(IntegerInverterModelRegister.VoltagePhaseCPhaseA),
				equalTo(488.9f));
	}

	@Test
	public void voltageLN() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getVoltage(), equalTo(283.5f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getVoltage(), equalTo(284.5f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getVoltage(), equalTo(279.6f));
		assertThat("Average", (double) model.getVoltage(), closeTo(282.53, 0.01));
	}

	@Test
	public void activePower() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActivePower(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActivePower(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActivePower(), nullValue());
		assertThat("Total", model.getActivePower(), equalTo(278));
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
		assertThat("Total", model.getApparentPower(), equalTo(1001));
	}

	@Test
	public void reactivePower() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactivePower(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactivePower(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactivePower(), nullValue());
		assertThat("Total", model.getReactivePower(), equalTo(-962));
	}

	@Test
	public void powerFactor() {
		ModelData data = getTestDataInstance();
		InverterModelAccessor model = data.findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getPowerFactor(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getPowerFactor(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getPowerFactor(), nullValue());
		assertThat("Average", model.getPowerFactor(), equalTo(-0.27711f));
		assertThat("Integer PF flag", data.getMetadataValue(IntegerInverterModelAccessor.INTEGER_PF_PCT),
				equalTo(true));
	}

	@Test
	public void activeEnergyExport() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActiveEnergyExported(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActiveEnergyExported(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActiveEnergyExported(), nullValue());
		assertThat("Total", model.getActiveEnergyExported(), equalTo(18048014L));
	}

	@Test
	public void dcCurrent() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getDcCurrent(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getDcCurrent(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getDcCurrent(), nullValue());
		assertThat("Total", (double) model.getDcCurrent(), closeTo(0.3513, 0.00001));
	}

	@Test
	public void dcVoltage() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getDcVoltage(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getDcVoltage(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getDcVoltage(), nullValue());
		assertThat("Average", (double) model.getDcVoltage(), closeTo(803.2, 0.01));
	}

	@Test
	public void dcPower() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getDcPower(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getDcPower(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getDcPower(), nullValue());
		assertThat("Total", model.getDcPower(), equalTo(282));
	}

	@Test
	public void cabinetTemperature() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Cabinet temperature", model.getCabinetTemperature(), nullValue());
	}

	@Test
	public void heatSinkTemperature() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Heat sink temperature", model.getHeatSinkTemperature(), equalTo(48.77f));
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

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
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import java.util.BitSet;
import java.util.Set;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.meter.IntegerMeterModelAccessor;
import net.solarnetwork.node.hw.sunspec.meter.IntegerMeterModelRegister;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelAccessor;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link IntegerMeterModelAccessor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class IntegerMeterModelAccessor_203_02Tests {

	private static final Logger log = LoggerFactory
			.getLogger(IntegerMeterModelAccessor_203_02Tests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-203-02.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void commonModelProperties() {
		CommonModelAccessor data = getTestDataInstance();
		assertThat("Manufacturer", data.getManufacturer(), startsWith("Elkor"));
		assertThat("Model name", data.getModelName(), equalTo("W2-M1-mA-DL"));
		assertThat("Options", data.getOptions(), equalTo(""));
		assertThat("Version", data.getVersion(), equalTo("11.12"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("14870"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(9));
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
		assertThat("Model base address", model.getBaseAddress(), equalTo(69));
		assertThat("Model block address", model.getBlockAddress(), equalTo(71));
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
		assertThat(bitset.cardinality(), equalTo(1));
		assertThat(bitset.get(3), equalTo(true));
	}

	@Test
	public void current() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getCurrent(), equalTo(3.218f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getCurrent(), equalTo(2.699f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getCurrent(), equalTo(0.519f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getCurrent(), equalTo(0f));
	}

	@Test
	public void voltageLN() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Average", model.getVoltage(), equalTo(83.03f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getVoltage(), equalTo(123.9f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getVoltage(), equalTo(125.32f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getVoltage(), equalTo(0.0f));
	}

	@Test
	public void voltageLL() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		IntegerMeterModelAccessor imm = (IntegerMeterModelAccessor) model;
		assertThat("Average", imm.getVoltageValue(IntegerMeterModelRegister.VoltageLineLineAverage),
				equalTo(154.66f));
		assertThat("Phase A", imm.getVoltageValue(IntegerMeterModelRegister.VoltagePhaseAPhaseB),
				equalTo(215.63f));
		assertThat("Phase B", imm.getVoltageValue(IntegerMeterModelRegister.VoltagePhaseBPhaseC),
				equalTo(125.32f));
		assertThat("Phase C", imm.getVoltageValue(IntegerMeterModelRegister.VoltagePhaseCPhaseA),
				equalTo(123.05f));
	}

	@Test
	public void frequency() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Frequency", model.getFrequency(), equalTo(60.0f));
	}

	@Test
	public void activePower() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getActivePower(), equalTo(976));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActivePower(), equalTo(930));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActivePower(), equalTo(46));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActivePower(), equalTo(0));
	}

	@Test
	public void apparentPower() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getApparentPower(), equalTo(1012));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentPower(), equalTo(952));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentPower(), equalTo(60));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentPower(), equalTo(0));
	}

	@Test
	public void reactivePower() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getReactivePower(), equalTo(-68));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactivePower(), equalTo(-51));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactivePower(), equalTo(-17));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactivePower(), equalTo(0));
	}

	@Test
	public void powerFactor() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Average", model.getPowerFactor(), equalTo(-0.7582f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getPowerFactor(), equalTo(-0.749f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getPowerFactor(), equalTo(-0.8069f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getPowerFactor(), equalTo(1.0f));
	}

	@Test
	public void activeEnergyExport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getActiveEnergyExported(), equalTo(22100L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActiveEnergyExported(), equalTo(19200L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActiveEnergyExported(), equalTo(2900L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActiveEnergyExported(), nullValue());
	}

	@Test
	public void activeEnergyImport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getActiveEnergyImported(), equalTo(300L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActiveEnergyImported(), equalTo(200L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActiveEnergyImported(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActiveEnergyImported(), nullValue());
	}

	@Test
	public void apparentEnergyExport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getApparentEnergyExported(), equalTo(27500L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentEnergyExported(),
				equalTo(23400L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentEnergyExported(),
				equalTo(4000L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentEnergyExported(), nullValue());
	}

	@Test
	public void apparentEnergyImport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getApparentEnergyImported(), equalTo(400L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentEnergyImported(), equalTo(300L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentEnergyImported(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentEnergyImported(), nullValue());
	}

	@Test
	public void reactiveEnergyImport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getReactiveEnergyImported(), equalTo(2900L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactiveEnergyImported(),
				equalTo(2300L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactiveEnergyImported(), equalTo(500L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactiveEnergyImported(), nullValue());
	}

	@Test
	public void reactiveEnergyExport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getReactiveEnergyExported(), equalTo(700L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactiveEnergyExported(), equalTo(600L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactiveEnergyExported(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactiveEnergyExported(), nullValue());
	}

}

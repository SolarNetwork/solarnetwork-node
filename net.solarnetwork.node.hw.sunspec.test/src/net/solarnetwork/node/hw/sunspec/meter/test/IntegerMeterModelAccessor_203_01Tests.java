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
public class IntegerMeterModelAccessor_203_01Tests {

	private static final Logger log = LoggerFactory
			.getLogger(IntegerMeterModelAccessor_203_01Tests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-203-01.txt");
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
		assertThat("Version", data.getVersion(), equalTo("2.115"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("4E4C3699"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(7));
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
		assertThat("Total", model.getCurrent(), equalTo(53.48f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getCurrent(), equalTo(26.81f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getCurrent(), equalTo(26.67f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getCurrent(), equalTo(0f));
	}

	@Test
	public void voltageLN() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Average", model.getVoltage(), equalTo(82.0f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getVoltage(), equalTo(123.0f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getVoltage(), equalTo(122.9f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getVoltage(), equalTo(0.2f));
	}

	@Test
	public void voltageLL() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		IntegerMeterModelAccessor imm = (IntegerMeterModelAccessor) model;
		assertThat("Average", imm.getVoltageValue(IntegerMeterModelRegister.VoltageLineLineAverage),
				equalTo(163.9f));
		assertThat("Phase A", imm.getVoltageValue(IntegerMeterModelRegister.VoltagePhaseAPhaseB),
				equalTo(245.9f));
		assertThat("Phase B", imm.getVoltageValue(IntegerMeterModelRegister.VoltagePhaseBPhaseC),
				equalTo(122.8f));
		assertThat("Phase C", imm.getVoltageValue(IntegerMeterModelRegister.VoltagePhaseCPhaseA),
				equalTo(123.0f));
	}

	@Test
	public void frequency() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Frequency", model.getFrequency(), equalTo(60.01f));
	}

	@Test
	public void activePower() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getActivePower(), equalTo(6540));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActivePower(), equalTo(3280));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActivePower(), equalTo(3260));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActivePower(), equalTo(0));
	}

	@Test
	public void apparentPower() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getApparentPower(), equalTo(6590));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentPower(), equalTo(3300));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentPower(), equalTo(3280));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentPower(), equalTo(0));
	}

	@Test
	public void reactivePower() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getReactivePower(), equalTo(-790));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactivePower(), equalTo(-390));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactivePower(), equalTo(-390));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactivePower(), equalTo(0));
	}

	@Test
	public void powerFactor() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Average", model.getPowerFactor(), equalTo(0.9925f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getPowerFactor(), equalTo(0.9930f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getPowerFactor(), equalTo(0.9930f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getPowerFactor(), equalTo(1.0f));
	}

	@Test
	public void activeEnergyExport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getActiveEnergyExported(), nullValue());
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActiveEnergyExported(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActiveEnergyExported(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActiveEnergyExported(), nullValue());
	}

	@Test
	public void activeEnergyImport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getActiveEnergyImported(), equalTo(906630L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActiveEnergyImported(),
				equalTo(454560L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActiveEnergyImported(), equalTo(1250L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActiveEnergyImported(), nullValue());
	}

	@Test
	public void apparentEnergyExport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getApparentEnergyExported(), equalTo(220L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentEnergyExported(), equalTo(240L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentEnergyExported(), equalTo(160L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentEnergyExported(), nullValue());
	}

	@Test
	public void apparentEnergyImport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getApparentEnergyImported(), equalTo(986930L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentEnergyImported(),
				equalTo(493500L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentEnergyImported(),
				equalTo(491010L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentEnergyImported(), nullValue());
	}

	@Test
	public void reactiveEnergyImport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getReactiveEnergyImported(), equalTo((0x36D2L + 0x28L) * 10L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactiveEnergyImported(),
				equalTo((0x1BDDL + 0x18L) * 10L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactiveEnergyImported(),
				equalTo((0x1AF4L + 0x10L) * 10L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactiveEnergyImported(), nullValue());
	}

	@Test
	public void reactiveEnergyExport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getReactiveEnergyExported(), equalTo((0x0 + 0x1D63L) * 10L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactiveEnergyExported(),
				equalTo((0x0 + 0x0E49L) * 10L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactiveEnergyExported(),
				equalTo((0x0L + 0x0F1AL) * 10L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactiveEnergyExported(), nullValue());
	}

}

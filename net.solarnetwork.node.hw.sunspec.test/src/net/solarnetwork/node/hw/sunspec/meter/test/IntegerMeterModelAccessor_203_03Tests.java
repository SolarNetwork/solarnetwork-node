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
public class IntegerMeterModelAccessor_203_03Tests {

	private static final Logger log = LoggerFactory
			.getLogger(IntegerMeterModelAccessor_203_03Tests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-203-03.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void commonModelProperties() {
		CommonModelAccessor data = getTestDataInstance();
		assertThat("Manufacturer", data.getManufacturer(), startsWith("ACCUENERGY"));
		assertThat("Model name", data.getModelName(), equalTo("AcuRev1310"));
		assertThat("Options", data.getOptions(), equalTo("AcuRev1312"));
		assertThat("Version", data.getVersion(), equalTo("H:2.01 S:2.16"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("E3T18102491"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(1));
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
		assertThat(bitset.cardinality(), equalTo(0));
	}

	@Test
	public void current() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getCurrent(), equalTo(160.61f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getCurrent(), equalTo(54.47f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getCurrent(), equalTo(52.64f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getCurrent(), equalTo(53.5f));
	}

	@Test
	public void voltageLN() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Average", model.getVoltage(), equalTo(123.7f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getVoltage(), equalTo(123.2f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getVoltage(), equalTo(123.9f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getVoltage(), equalTo(124.2f));
	}

	@Test
	public void voltageLL() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		IntegerMeterModelAccessor imm = (IntegerMeterModelAccessor) model;
		assertThat("Average", imm.getVoltageValue(IntegerMeterModelRegister.VoltageLineLineAverage),
				equalTo(214.3f));
		assertThat("Phase A", imm.getVoltageValue(IntegerMeterModelRegister.VoltagePhaseAPhaseB),
				equalTo(213.6f));
		assertThat("Phase B", imm.getVoltageValue(IntegerMeterModelRegister.VoltagePhaseBPhaseC),
				equalTo(215.0f));
		assertThat("Phase C", imm.getVoltageValue(IntegerMeterModelRegister.VoltagePhaseCPhaseA),
				equalTo(214.5f));
	}

	@Test
	public void frequency() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Frequency", model.getFrequency(), equalTo(60.01f));
	}

	@Test
	public void activePower() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getActivePower(), equalTo(132));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActivePower(), equalTo(6323));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActivePower(), equalTo(-1052));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActivePower(), equalTo(-5139));
	}

	@Test
	public void apparentPower() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getApparentPower(), equalTo(19847));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentPower(), equalTo(6720));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentPower(), equalTo(6502));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentPower(), equalTo(6625));
	}

	@Test
	public void reactivePower() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getReactivePower(), equalTo(93));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactivePower(), equalTo(2314));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactivePower(), equalTo(-6428));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactivePower(), equalTo(4206));
	}

	@Test
	public void powerFactor() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Average", model.getPowerFactor(), equalTo(0.007f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getPowerFactor(), equalTo(0.941f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getPowerFactor(), equalTo(-0.161f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getPowerFactor(), equalTo(-0.776f));
	}

	@Test
	public void activeEnergyExport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getActiveEnergyExported(), nullValue());
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActiveEnergyExported(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActiveEnergyExported(), equalTo(6L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActiveEnergyExported(), equalTo(26L));
	}

	@Test
	public void activeEnergyImport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getActiveEnergyImported(), equalTo(0L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActiveEnergyImported(), equalTo(33L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActiveEnergyImported(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActiveEnergyImported(), nullValue());
	}

	@Test
	public void apparentEnergyExport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getApparentEnergyExported(), nullValue());
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentEnergyExported(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentEnergyExported(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentEnergyExported(), nullValue());
	}

	@Test
	public void apparentEnergyImport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getApparentEnergyImported(), equalTo(104L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentEnergyImported(), equalTo(35L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentEnergyImported(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentEnergyImported(), nullValue());
	}

	@Test
	public void reactiveEnergyImport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getReactiveEnergyImported(), equalTo(0L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactiveEnergyImported(), equalTo(11L));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactiveEnergyImported(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactiveEnergyImported(), equalTo(22L));
	}

	@Test
	public void reactiveEnergyExport() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Total", model.getReactiveEnergyExported(), equalTo(0L));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactiveEnergyExported(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactiveEnergyExported(), equalTo(33L));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactiveEnergyExported(), nullValue());
	}

}

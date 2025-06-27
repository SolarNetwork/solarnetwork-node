/* ==================================================================
 * FloatingPointInverterModelAccessorTests.java - 8/10/2019 3:36:49 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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
import static net.solarnetwork.util.NumberUtils.bitSetForBigInteger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.Set;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.OperatingState;
import net.solarnetwork.node.hw.sunspec.inverter.FloatingPointInverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.FloatingPointInverterModelRegister;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterOperatingState;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link FloatingPointInverterModelAccessor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class FloatingPointInverterModelAccessorTests {

	private static final Logger log = LoggerFactory
			.getLogger(FloatingPointInverterModelAccessorTests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-113-01.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void commonModelProperties() {
		CommonModelAccessor data = getTestDataInstance();
		assertThat("Manufacturer", data.getManufacturer(), equalTo("Fronius"));
		assertThat("Model name", data.getModelName(), equalTo("Symo 3.0-3-S"));
		assertThat("Options", data.getOptions(), equalTo("3.4.2-1"));
		assertThat("Version", data.getVersion(), equalTo("0.3.11.10"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("29251001150340235"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(1));
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		InverterModelAccessor meterAccessor = data.findTypedModel(InverterModelAccessor.class);
		assertThat(meterAccessor, instanceOf(FloatingPointInverterModelAccessor.class));
	}

	@Test
	public void getTypedModel() {
		ModelData data = getTestDataInstance();
		InverterModelAccessor meterAccessor = data.getTypedModel();
		assertThat(meterAccessor, instanceOf(FloatingPointInverterModelAccessor.class));
	}

	@Test
	public void current() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Total", model.getCurrent(), equalTo(0.7f));
		assertThat("Phase A", (double) model.accessorForPhase(PhaseA).getCurrent(), closeTo(0.2, 0.1));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getCurrent(), equalTo(0.17f));
		assertThat("Phase C", (double) model.accessorForPhase(PhaseC).getCurrent(), closeTo(0.33, 0.01));
	}

	@Test
	public void voltageLL() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		FloatingPointInverterModelAccessor imm = (FloatingPointInverterModelAccessor) model;
		assertThat("Phase AB", imm.getValue(FloatingPointInverterModelRegister.VoltagePhaseAPhaseB),
				equalTo(431.0f));
		assertThat("Phase BC", imm.getValue(FloatingPointInverterModelRegister.VoltagePhaseBPhaseC),
				equalTo(427.0f));
		assertThat("Phase CA", imm.getValue(FloatingPointInverterModelRegister.VoltagePhaseCPhaseA),
				equalTo(426.2f));
	}

	@Test
	public void voltageLN() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getVoltage(), equalTo(247.40001f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getVoltage(), equalTo(250.90001f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getVoltage(), equalTo(246.1f));
		assertThat("Average", (double) model.getVoltage(), closeTo(248.13, 0.01));
	}

	@Test
	public void activePower() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActivePower(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActivePower(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActivePower(), nullValue());
		assertThat("Total", model.getActivePower(), equalTo(70));
	}

	@Test
	public void frequency() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Frequency", model.getFrequency(), equalTo(50.05f));
	}

	@Test
	public void apparentPower() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getApparentPower(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getApparentPower(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getApparentPower(), nullValue());
		assertThat("Total", model.getApparentPower(), equalTo(70));
	}

	@Test
	public void reactivePower() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getReactivePower(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getReactivePower(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getReactivePower(), nullValue());
		assertThat("Total", model.getReactivePower(), equalTo(0));
	}

	@Test
	public void powerFactor() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getPowerFactor(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getPowerFactor(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getPowerFactor(), nullValue());
		assertThat("Average", model.getPowerFactor(), equalTo(1.0f));
	}

	@Test
	public void activeEnergyExport() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getActiveEnergyExported(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getActiveEnergyExported(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getActiveEnergyExported(), nullValue());
		assertThat("Total", model.getActiveEnergyExported(), equalTo(11937020L));
	}

	@Test
	public void dcCurrent() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getDcCurrent(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getDcCurrent(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getDcCurrent(), nullValue());
		assertThat("Total", (double) model.getDcCurrent(), closeTo(0.15, 0.01));
	}

	@Test
	public void dcVoltage() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getDcVoltage(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getDcVoltage(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getDcVoltage(), nullValue());
		assertThat("Average", (double) model.getDcVoltage(), closeTo(406.9, 0.01));
	}

	@Test
	public void dcPower() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Phase A", model.accessorForPhase(PhaseA).getDcPower(), nullValue());
		assertThat("Phase B", model.accessorForPhase(PhaseB).getDcPower(), nullValue());
		assertThat("Phase C", model.accessorForPhase(PhaseC).getDcPower(), nullValue());
		assertThat("Total", model.getDcPower(), equalTo(61));
	}

	@Test
	public void cabinetTemperature() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Cabinet temperature", model.getCabinetTemperature(), nullValue());
	}

	@Test
	public void heatSinkTemperature() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Heat sink temperature", model.getHeatSinkTemperature(), nullValue());
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

	@Test
	public void vendorOperatingState() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		Integer state = model.getVendorOperatingState();
		assertThat("Vendor operating state", state, is(equalTo(4)));
	}

	@Test
	public void vendorEvents() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		BitSet events = model.getVendorEvents();

		BigInteger expected = new BigInteger("00070008000500060003000400010002", 16);
		assertThat("No vendor events", events, is(equalTo(bitSetForBigInteger(expected))));
	}

}

/* ==================================================================
 * IntegerInverterModelAccessorTests.java - 8/10/2018 7:06:15 AM
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

import static net.solarnetwork.node.domain.ACPhase.PhaseA;
import static net.solarnetwork.node.domain.ACPhase.PhaseB;
import static net.solarnetwork.node.domain.ACPhase.PhaseC;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelDataFactory;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.OperatingState;
import net.solarnetwork.node.hw.sunspec.inverter.IntegerInverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelId;
import net.solarnetwork.node.hw.sunspec.inverter.InverterOperatingState;
import net.solarnetwork.node.hw.sunspec.meter.test.IntegerMeterModelAccessorTests;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.StaticDataReadonlyModbusConnection;
import net.solarnetwork.node.test.DataUtils;

/**
 * Test cases for the {@link IntegerInverterModelAccessor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class IntegerInverterModelAccessorTests {

	private static final Logger log = LoggerFactory.getLogger(IntegerMeterModelAccessorTests.class);

	private static int[] parseTestData(String resource) {
		try {
			return DataUtils.parseModbusHexRegisterLines(new BufferedReader(new InputStreamReader(
					IntegerInverterModelAccessorTests.class.getResourceAsStream(resource))));
		} catch ( IOException e ) {
			log.error("Error reading modbus data resource [{}]", resource, e);
			return new int[0];
		}
	}

	private ModelData getTestDataInstance(int baseAddress, String resource) {
		ModbusConnection conn = new StaticDataReadonlyModbusConnection(parseTestData(resource));
		return ModelDataFactory.getInstance().getModelData(conn);
	}

	private ModelData getTestDataInstance1() {
		return getTestDataInstance(0, "test-data-103-01.txt");
	}

	private ModelData getTestDataInstance2() {
		return getTestDataInstance(0, "test-data-101-01.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance1();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void commonModelProperties() {
		CommonModelAccessor data = getTestDataInstance1();
		assertThat("Manufacturer", data.getManufacturer(), equalTo("SolarEdge"));
		assertThat("Model name", data.getModelName(), equalTo("SE33.3K"));
		assertThat("Options", data.getOptions(), equalTo(""));
		assertThat("Version", data.getVersion(), equalTo("0003.2251"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("7E1240CC"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(11));
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance1();
		InverterModelAccessor meterAccessor = data.findTypedModel(InverterModelAccessor.class);
		assertThat(meterAccessor, instanceOf(IntegerInverterModelAccessor.class));
	}

	@Test
	public void block() {
		InverterModelAccessor model = getTestDataInstance1().getTypedModel();
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
	public void operatingState() {
		InverterModelAccessor model = getTestDataInstance1().getTypedModel();
		OperatingState state = model.getOperatingState();
		assertThat("Operating state available", state, notNullValue());
		assertThat("State value", state.getCode(), equalTo(InverterOperatingState.Sleeping.getCode()));
	}

	@Test
	public void events() {
		InverterModelAccessor model = getTestDataInstance1().getTypedModel();
		Set<ModelEvent> events = model.getEvents();
		assertThat("No events", events, hasSize(0));
	}

	@Test
	public void current() {
		InverterModelAccessor model = getTestDataInstance1().getTypedModel();
		assertThat("Total", model.getCurrent(), equalTo(0f));
		assertThat("Phase A", model.accessorForPhase(PhaseA).getCurrent(), equalTo(0f));
		assertThat("Phase B", model.accessorForPhase(PhaseB).getCurrent(), equalTo(0f));
		assertThat("Phase C", model.accessorForPhase(PhaseC).getCurrent(), equalTo(0f));
	}

	@Test
	public void commonModelProperties2() {
		CommonModelAccessor data = getTestDataInstance2();
		assertThat("Manufacturer", data.getManufacturer(), equalTo("Fronius"));
		assertThat("Model name", data.getModelName(), equalTo("IG+V11.4"));
		assertThat("Options", data.getOptions(), equalTo("2.1.18"));
		assertThat("Version", data.getVersion(), equalTo("5.10.0"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("50.213262"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(5));
	}

}

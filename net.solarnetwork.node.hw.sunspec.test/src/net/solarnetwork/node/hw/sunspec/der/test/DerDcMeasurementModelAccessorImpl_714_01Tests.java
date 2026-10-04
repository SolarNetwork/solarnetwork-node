/* ==================================================================
 * DerDcMeasurementModelAccessorImpl_714_01Tests.java - 5/10/2026 10:42:15 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.der.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.der.DerDcMeasurementModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerDcMeasurementModelAccessor.DcPort;
import net.solarnetwork.node.hw.sunspec.der.DerDcMeasurementModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.der.DerDcPortAlarm;
import net.solarnetwork.node.hw.sunspec.der.DerDcPortStatus;
import net.solarnetwork.node.hw.sunspec.der.DerDcPortType;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link DerDcMeasurementModelAccessorImpl} class.
 *
 * <p>
 * The device that produced the test data writes {@code uint64} values least
 * significant word first, contrary to SunSpec, so the energy values decoded
 * here are not the energy the device meant to report.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class DerDcMeasurementModelAccessorImpl_714_01Tests {

	private static final String TEST_DATA = "test-data-der-01.txt";

	/** The model block address. */
	private static final int BLOCK_ADDRESS = 1222;

	/** The first port block address. */
	private static final int PORT_ADDRESS = BLOCK_ADDRESS + 18;

	private DerDcMeasurementModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(DerDcMeasurementModelAccessor.class);
	}

	private DerDcMeasurementModelAccessor getTestModel(int address, int... words) {
		return ModelDataUtils.getModelDataInstanceWithRegisters(getClass(), TEST_DATA, address, words)
				.findTypedModel(DerDcMeasurementModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerDcMeasurementModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerDcMeasurementModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(1220)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(BLOCK_ADDRESS)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.DcMeasurement)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(18)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(25)));
		assertThat("Model repeating instance count", model.getRepeatingBlockInstanceCount(),
				is(equalTo(1)));
		assertThat("Model length", model.getModelLength(), is(equalTo(43)));
	}

	@Test
	public void values() {
		DerDcMeasurementModelAccessor model = getTestModel();
		assertThat("No ports with alarms", model.getAlarmedPortIndexes(), is(equalTo(Set.of())));
		assertThat("Port count", model.getPortCount(), is(equalTo(1)));
		assertThat("DC current", model.getDCCurrent(), is(equalTo(0.0f)));
		assertThat("DC power", model.getDCPower(), is(equalTo(0)));
		assertThat("DC energy injected, from words 08A1 0000 0000 0000", model.getDCEnergyInjected(),
				is(equalTo(0x08A1000000000000L)));
		assertThat("DC energy absorbed, from words FF42 FFFF FFFF FFFF, larger than a long",
				model.getDCEnergyAbsorbed(), is(nullValue()));
	}

	@Test
	public void ports() {
		List<DcPort> ports = getTestModel().getDcPorts();
		assertThat("Port count", ports, hasSize(1));

		DcPort port = ports.get(0);
		assertThat("Port type", port.getPortType(), is(equalTo(DerDcPortType.EnergyStorageSystem)));
		assertThat("Port ID", port.getPortId(), is(equalTo(0)));
		assertThat("Port name", port.getPortName(), is(equalTo("Battery")));
		assertThat("DC current", port.getDCCurrent(), is(equalTo(0.0f)));
		assertThat("DC voltage", port.getDCVoltage(), is(equalTo(56.09f)));
		assertThat("DC power", port.getDCPower(), is(equalTo(0)));
		assertThat("DC energy injected, from words 08A1 0000 0000 0000", port.getDCEnergyInjected(),
				is(equalTo(0x08A1000000000000L)));
		assertThat("DC energy absorbed, from words FF42 FFFF FFFF FFFF, larger than a long",
				port.getDCEnergyAbsorbed(), is(nullValue()));
		assertThat("Temperature, as reported with a 0 scale factor", port.getTemperature(),
				is(equalTo(199.0f)));
		assertThat("Status", port.getPortStatus(), is(equalTo(DerDcPortStatus.On)));
		assertThat("No alarms", port.getEvents(), is(equalTo(Set.of())));
	}

	@Test
	public void alarmedPortIndexes() {
		// GIVEN
		DerDcMeasurementModelAccessor model = getTestModel(BLOCK_ADDRESS, 0x0000, 0x0005);

		// THEN
		assertThat("Bits 0 and 2 set", model.getAlarmedPortIndexes(), is(equalTo(Set.of(0, 2))));
	}

	@Test
	public void alarmedPortIndexes_mostSignificantBit() {
		// GIVEN
		DerDcMeasurementModelAccessor model = getTestModel(BLOCK_ADDRESS, 0x8000, 0x0001);

		// THEN
		assertThat("Bitfield with MSB set not implemented", model.getAlarmedPortIndexes(),
				is(equalTo(Set.of())));
	}

	@Test
	public void portEvents() {
		// GIVEN
		// bits 0, 2, 7, 12, 19; bit 2 is not defined by SunSpec
		DerDcMeasurementModelAccessor model = getTestModel(PORT_ADDRESS + 23, 0x0008, 0x1085);

		// THEN
		assertThat("Alarms", model.getDcPorts().get(0).getEvents(),
				is(equalTo(Set.of(DerDcPortAlarm.GroundFault, DerDcPortAlarm.OverTemperature,
						DerDcPortAlarm.BlownFuse, DerDcPortAlarm.Reserved))));
	}

	@Test
	public void ports_countLimitedByModelLength() {
		// GIVEN
		DerDcMeasurementModelAccessor model = getTestModel(BLOCK_ADDRESS + 2, 3);

		// THEN
		assertThat("Port count", model.getPortCount(), is(equalTo(3)));
		assertThat("Ports limited to model length", model.getDcPorts(), hasSize(1));
	}

	@Test
	public void ports_countNotImplemented() {
		// GIVEN
		DerDcMeasurementModelAccessor model = getTestModel(BLOCK_ADDRESS + 2, 0xFFFF);

		// THEN
		assertThat("Port count not implemented", model.getPortCount(), is(nullValue()));
		assertThat("Ports from model length", model.getDcPorts(), hasSize(1));
	}

}

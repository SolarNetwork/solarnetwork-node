/* ==================================================================
 * DerEnterServiceModelAccessorImpl_703_01Tests.java - 5/10/2026 9:32:29 am
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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import java.io.IOException;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.der.DerEnterServiceModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerEnterServiceModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.StaticDataMapModbusConnection;

/**
 * Test cases for the {@link DerEnterServiceModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class DerEnterServiceModelAccessorImpl_703_01Tests {

	private static final String TEST_DATA = "test-data-der-01.txt";

	private DerEnterServiceModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(DerEnterServiceModelAccessor.class);
	}

	private static DerEnterServiceModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn)
				.findTypedModel(DerEnterServiceModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerEnterServiceModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerEnterServiceModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(384)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(386)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.EnterService)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(17)));
		assertThat("Model length", model.getModelLength(), is(equalTo(17)));
	}

	@Test
	public void values() {
		DerEnterServiceModelAccessor model = getTestModel();
		assertThat("Permitted", model.isEnterServicePermitted(), is(equalTo(true)));
		assertThat("Voltage high", model.getVoltageHigh(), is(equalTo(106.0f)));
		assertThat("Voltage low", model.getVoltageLow(), is(equalTo(95.0f)));
		assertThat("Frequency high", model.getFrequencyHigh(), is(equalTo(61.0f)));
		assertThat("Frequency low", model.getFrequencyLow(), is(equalTo(59.9f)));
		assertThat("Delay", model.getDelay(), is(equalTo(600L)));
		assertThat("Random delay", model.getRandomDelay(), is(equalTo(1000L)));
		assertThat("Ramp time", model.getRampTime(), is(equalTo(1000L)));
		assertThat("Delay remaining", model.getDelayRemaining(), is(equalTo(0L)));
	}

	@Test
	public void writeValues() throws IOException {
		// GIVEN
		StaticDataMapModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		DerEnterServiceModelAccessor model = discoverModel(conn);

		// WHEN
		model.setEnterServicePermitted(conn, false);
		model.setVoltageHigh(conn, 105.5f);
		model.setVoltageLow(conn, 91.7f);
		model.setFrequencyHigh(conn, 60.5f);
		model.setFrequencyLow(conn, 59.55f);
		model.setDelay(conn, 300);
		model.setRandomDelay(conn, 120);
		model.setRampTime(conn, 60);

		// THEN
		assertThat("Model data updated", model.isEnterServicePermitted(), is(equalTo(false)));

		DerEnterServiceModelAccessor device = discoverModel(conn);
		assertThat("Permitted", device.isEnterServicePermitted(), is(equalTo(false)));
		assertThat("Voltage high", device.getVoltageHigh(), is(equalTo(105.5f)));
		assertThat("Voltage low", device.getVoltageLow(), is(equalTo(91.7f)));
		assertThat("Frequency high", device.getFrequencyHigh(), is(equalTo(60.5f)));
		assertThat("Frequency low", device.getFrequencyLow(), is(equalTo(59.55f)));
		assertThat("Delay", device.getDelay(), is(equalTo(300L)));
		assertThat("Random delay", device.getRandomDelay(), is(equalTo(120L)));
		assertThat("Ramp time", device.getRampTime(), is(equalTo(60L)));
		assertThat("Delay remaining unchanged", device.getDelayRemaining(), is(equalTo(0L)));
	}

	@Test
	public void writeValue_outOfRange() throws IOException {
		// GIVEN
		StaticDataMapModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		DerEnterServiceModelAccessor model = discoverModel(conn);

		// WHEN
		try {
			model.setVoltageHigh(conn, 7000f);
			fail("Scaled value larger than uint16 should be rejected.");
		} catch ( IllegalArgumentException e ) {
			// expected
		}

		// THEN
		assertThat("Device not updated", discoverModel(conn).getVoltageHigh(), is(equalTo(106.0f)));
	}

}

/* ==================================================================
 * InverterPricingSignalModelAccessorImplTests.java - 6/10/2026 10:58:21 am
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

package net.solarnetwork.node.hw.sunspec.inverter.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.inverter.InverterControlModelId;
import net.solarnetwork.node.hw.sunspec.inverter.InverterPricingSignalModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterPricingSignalModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.inverter.InverterPricingSignalType;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link InverterPricingSignalModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class InverterPricingSignalModelAccessorImplTests {

	/** Synthetic data, as there is no capture from a device. */
	private static final String TEST_DATA = "test-data-125-01.txt";

	/** The model block address in the test data. */
	private static final int BLOCK_ADDRESS = 72;

	private InverterPricingSignalModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(InverterPricingSignalModelAccessor.class);
	}

	private InverterPricingSignalModelAccessor getTestModel(int address, int... words) {
		return ModelDataUtils.getModelDataInstanceWithRegisters(getClass(), TEST_DATA, address, words)
				.findTypedModel(InverterPricingSignalModelAccessor.class);
	}

	private static InverterPricingSignalModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn)
				.findTypedModel(InverterPricingSignalModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(InverterPricingSignalModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		InverterPricingSignalModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(70)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(BLOCK_ADDRESS)));
		assertThat("Model ID", model.getModelId(), is(equalTo(InverterControlModelId.PricingSignal)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(8)));
		assertThat("Model length", model.getModelLength(), is(equalTo(8)));
	}

	@Test
	public void values() {
		InverterPricingSignalModelAccessor model = getTestModel();
		assertThat("Pricing enabled", model.isPricingEnabled(), is(equalTo(true)));
		assertThat("Pricing signal type", model.getPricingSignalType(),
				is(equalTo(InverterPricingSignalType.Absolute)));
		assertThat("Pricing signal", model.getPricingSignal(), is(equalTo(23.5f)));
		assertThat("Time window", model.getPricingTimeWindow(), is(equalTo(60)));
		assertThat("Reversion time", model.getPricingReversionTime(), is(equalTo(3600)));
		assertThat("Ramp time", model.getPricingRampTime(), is(equalTo(120)));
	}

	@Test
	public void pricingEnabled_notImplemented() {
		assertThat("Pricing enabled not implemented",
				getTestModel(BLOCK_ADDRESS, 0xFFFF).isPricingEnabled(), is(nullValue()));
	}

	@Test
	public void pricingEnabled_mostSignificantBit() {
		assertThat("Pricing enabled with MSB set not implemented",
				getTestModel(BLOCK_ADDRESS, 0x8001).isPricingEnabled(), is(nullValue()));
	}

	@Test
	public void pricingEnabled_undefinedBits() {
		assertThat("Undefined bits ignored", getTestModel(BLOCK_ADDRESS, 0x0002).isPricingEnabled(),
				is(equalTo(false)));
	}

	@Test
	public void writeValues() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		InverterPricingSignalModelAccessor model = discoverModel(conn);

		// WHEN
		model.setPricingEnabled(conn, false);
		model.setPricingSignalType(conn, InverterPricingSignalType.Relative);
		model.setPricingSignal(conn, -5.25f);
		model.setPricingTimeWindow(conn, 30);
		model.setPricingReversionTime(conn, 900);
		model.setPricingRampTime(conn, 15);

		// THEN
		assertThat("Each point written to its own register, in order", conn.getWrites(),
				is(equalTo(IntStream.rangeClosed(72, 77).mapToObj(a -> List.of(a, 1)).toList())));
		assertThat("Model data updated", model.isPricingEnabled(), is(equalTo(false)));

		InverterPricingSignalModelAccessor device = discoverModel(conn);
		assertThat("Pricing enabled", device.isPricingEnabled(), is(equalTo(false)));
		assertThat("Pricing signal type", device.getPricingSignalType(),
				is(equalTo(InverterPricingSignalType.Relative)));
		assertThat("Pricing signal", device.getPricingSignal(), is(equalTo(-5.25f)));
		assertThat("Time window", device.getPricingTimeWindow(), is(equalTo(30)));
		assertThat("Reversion time", device.getPricingReversionTime(), is(equalTo(900)));
		assertThat("Ramp time", device.getPricingRampTime(), is(equalTo(15)));

		// WHEN
		device.setPricingEnabled(conn, true);

		// THEN
		assertThat("Pricing enabled again", discoverModel(conn).isPricingEnabled(), is(equalTo(true)));
	}

}

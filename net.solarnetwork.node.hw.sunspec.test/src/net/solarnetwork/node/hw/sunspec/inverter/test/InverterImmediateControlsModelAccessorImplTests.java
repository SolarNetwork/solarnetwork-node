/* ==================================================================
 * InverterImmediateControlsModelAccessorImplTests.java - 6/10/2026 10:14:36 am
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
import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.inverter.InverterConnectionControl;
import net.solarnetwork.node.hw.sunspec.inverter.InverterControlModelId;
import net.solarnetwork.node.hw.sunspec.inverter.InverterImmediateControlsModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterImmediateControlsModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.inverter.InverterReactivePowerPercentMode;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link InverterImmediateControlsModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class InverterImmediateControlsModelAccessorImplTests {

	/** A Fronius IG Plus capture. */
	private static final String FRONIUS_TEST_DATA = "test-data-101-01.txt";

	/** An SMA capture. */
	private static final String SMA_TEST_DATA = "test-data-103-05.txt";

	/** A Fronius Symo capture. */
	private static final String FRONIUS_SYMO_TEST_DATA = "test-data-113-01.txt";

	private InverterImmediateControlsModelAccessor getTestModel(String resource) {
		return ModelDataUtils.getModelDataInstance(getClass(), resource)
				.findTypedModel(InverterImmediateControlsModelAccessor.class);
	}

	private static InverterImmediateControlsModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn)
				.findTypedModel(InverterImmediateControlsModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(FRONIUS_TEST_DATA),
				is(instanceOf(InverterImmediateControlsModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		InverterImmediateControlsModelAccessor model = getTestModel(FRONIUS_TEST_DATA);
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(227)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(229)));
		assertThat("Model ID", model.getModelId(),
				is(equalTo(InverterControlModelId.ImmediateControls)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(24)));
		assertThat("Model length", model.getModelLength(), is(equalTo(24)));
	}

	@Test
	public void values_fronius() {
		InverterImmediateControlsModelAccessor model = getTestModel(FRONIUS_TEST_DATA);
		assertThat("Connection time window", model.getConnectionTimeWindow(), is(equalTo(0)));
		assertThat("Connection reversion time", model.getConnectionReversionTime(), is(equalTo(0)));
		assertThat("Connection control", model.getConnectionControl(),
				is(equalTo(InverterConnectionControl.Disconnect)));
		assertThat("Active power limit", model.getActivePowerLimitPercent(), is(equalTo(100.0f)));
		assertThat("Active power limit time window", model.getActivePowerLimitTimeWindow(),
				is(equalTo(0)));
		assertThat("Active power limit reversion time", model.getActivePowerLimitReversionTime(),
				is(equalTo(0)));
		assertThat("Active power limit ramp time", model.getActivePowerLimitRampTime(), is(equalTo(0)));
		assertThat("Active power limit enabled", model.isActivePowerLimitEnabled(), is(equalTo(false)));
		assertThat("Fixed power factor", model.getFixedPowerFactor(), is(equalTo(0.0f)));
		assertThat("Fixed power factor time window", model.getFixedPowerFactorTimeWindow(),
				is(equalTo(0)));
		assertThat("Fixed power factor reversion time", model.getFixedPowerFactorReversionTime(),
				is(equalTo(0)));
		assertThat("Fixed power factor ramp time", model.getFixedPowerFactorRampTime(), is(equalTo(0)));
		assertThat("Fixed power factor enabled", model.isFixedPowerFactorEnabled(), is(equalTo(false)));
		assertThat("Reactive power of maximum active power not implemented",
				model.getReactivePowerPercentOfMaximumActivePower(), is(nullValue()));
		assertThat("Reactive power of maximum reactive power",
				model.getReactivePowerPercentOfMaximumReactivePower(), is(equalTo(0.0f)));
		assertThat("Reactive power of available reactive power not implemented",
				model.getReactivePowerPercentOfAvailableReactivePower(), is(nullValue()));
		assertThat("Reactive power time window", model.getReactivePowerPercentTimeWindow(),
				is(equalTo(0)));
		assertThat("Reactive power reversion time", model.getReactivePowerPercentReversionTime(),
				is(equalTo(0)));
		assertThat("Reactive power ramp time", model.getReactivePowerPercentRampTime(), is(equalTo(0)));
		assertThat("Reactive power mode", model.getReactivePowerPercentMode(),
				is(equalTo(InverterReactivePowerPercentMode.MaximumReactivePowerPercent)));
		assertThat("Reactive power enabled", model.isReactivePowerPercentEnabled(), is(equalTo(false)));
	}

	@Test
	public void values_sma() {
		InverterImmediateControlsModelAccessor model = getTestModel(SMA_TEST_DATA);
		assertThat("Connection time window not implemented", model.getConnectionTimeWindow(),
				is(nullValue()));
		assertThat("Connection reversion time not implemented", model.getConnectionReversionTime(),
				is(nullValue()));
		assertThat("Connection control not implemented", model.getConnectionControl(), is(nullValue()));
		assertThat("Active power limit not implemented", model.getActivePowerLimitPercent(),
				is(nullValue()));
		assertThat("Active power limit ramp time not implemented", model.getActivePowerLimitRampTime(),
				is(nullValue()));
		assertThat("Active power limit enabled", model.isActivePowerLimitEnabled(), is(equalTo(true)));
		assertThat("Fixed power factor not implemented", model.getFixedPowerFactor(), is(nullValue()));
		assertThat("Fixed power factor enabled", model.isFixedPowerFactorEnabled(), is(equalTo(true)));
		assertThat("Reactive power of maximum reactive power not implemented",
				model.getReactivePowerPercentOfMaximumReactivePower(), is(nullValue()));
		assertThat("Reactive power mode", model.getReactivePowerPercentMode(),
				is(equalTo(InverterReactivePowerPercentMode.MaximumActivePowerPercent)));
		assertThat("Reactive power enabled", model.isReactivePowerPercentEnabled(), is(equalTo(false)));
	}

	@Test
	public void values_froniusSymo() {
		InverterImmediateControlsModelAccessor model = getTestModel(FRONIUS_SYMO_TEST_DATA);
		assertThat("Connection control", model.getConnectionControl(),
				is(equalTo(InverterConnectionControl.Connect)));
		assertThat("Active power limit", model.getActivePowerLimitPercent(), is(equalTo(100.0f)));
		assertThat("Active power limit ramp time not implemented", model.getActivePowerLimitRampTime(),
				is(nullValue()));
		assertThat("Fixed power factor", model.getFixedPowerFactor(), is(equalTo(1.0f)));
		assertThat("Fixed power factor ramp time not implemented", model.getFixedPowerFactorRampTime(),
				is(nullValue()));
		assertThat("Reactive power ramp time not implemented", model.getReactivePowerPercentRampTime(),
				is(nullValue()));
	}

	@Test
	public void writeValues() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				FRONIUS_TEST_DATA);
		InverterImmediateControlsModelAccessor model = discoverModel(conn);

		// WHEN
		model.setConnectionTimeWindow(conn, 30);
		model.setConnectionReversionTime(conn, 600);
		model.setConnectionControl(conn, InverterConnectionControl.Connect);
		model.setActivePowerLimitPercent(conn, 75.0f);
		model.setActivePowerLimitTimeWindow(conn, 10);
		model.setActivePowerLimitReversionTime(conn, 900);
		model.setActivePowerLimitRampTime(conn, 5);
		model.setActivePowerLimitEnabled(conn, true);
		model.setFixedPowerFactor(conn, -0.95f);
		model.setFixedPowerFactorTimeWindow(conn, 20);
		model.setFixedPowerFactorReversionTime(conn, 1200);
		model.setFixedPowerFactorRampTime(conn, 15);
		model.setFixedPowerFactorEnabled(conn, true);
		model.setReactivePowerPercentOfMaximumActivePower(conn, -25.0f);
		model.setReactivePowerPercentOfMaximumReactivePower(conn, 40.0f);
		model.setReactivePowerPercentOfAvailableReactivePower(conn, 60.0f);
		model.setReactivePowerPercentTimeWindow(conn, 25);
		model.setReactivePowerPercentReversionTime(conn, 1800);
		model.setReactivePowerPercentRampTime(conn, 35);
		model.setReactivePowerPercentMode(conn,
				InverterReactivePowerPercentMode.AvailableReactivePowerPercent);
		model.setReactivePowerPercentEnabled(conn, true);

		// THEN
		assertThat("Each point written to its own register, in order", conn.getWrites(),
				is(equalTo(IntStream.rangeClosed(229, 249).mapToObj(a -> List.of(a, 1)).toList())));
		assertThat("Model data updated", model.getConnectionControl(),
				is(equalTo(InverterConnectionControl.Connect)));

		InverterImmediateControlsModelAccessor device = discoverModel(conn);
		assertThat("Connection time window", device.getConnectionTimeWindow(), is(equalTo(30)));
		assertThat("Connection reversion time", device.getConnectionReversionTime(), is(equalTo(600)));
		assertThat("Connection control", device.getConnectionControl(),
				is(equalTo(InverterConnectionControl.Connect)));
		assertThat("Active power limit", device.getActivePowerLimitPercent(), is(equalTo(75.0f)));
		assertThat("Active power limit time window", device.getActivePowerLimitTimeWindow(),
				is(equalTo(10)));
		assertThat("Active power limit reversion time", device.getActivePowerLimitReversionTime(),
				is(equalTo(900)));
		assertThat("Active power limit ramp time", device.getActivePowerLimitRampTime(), is(equalTo(5)));
		assertThat("Active power limit enabled", device.isActivePowerLimitEnabled(), is(equalTo(true)));
		assertThat("Fixed power factor", device.getFixedPowerFactor(), is(equalTo(-0.95f)));
		assertThat("Fixed power factor time window", device.getFixedPowerFactorTimeWindow(),
				is(equalTo(20)));
		assertThat("Fixed power factor reversion time", device.getFixedPowerFactorReversionTime(),
				is(equalTo(1200)));
		assertThat("Fixed power factor ramp time", device.getFixedPowerFactorRampTime(),
				is(equalTo(15)));
		assertThat("Fixed power factor enabled", device.isFixedPowerFactorEnabled(), is(equalTo(true)));
		assertThat("Reactive power of maximum active power",
				device.getReactivePowerPercentOfMaximumActivePower(), is(equalTo(-25.0f)));
		assertThat("Reactive power of maximum reactive power",
				device.getReactivePowerPercentOfMaximumReactivePower(), is(equalTo(40.0f)));
		assertThat("Reactive power of available reactive power",
				device.getReactivePowerPercentOfAvailableReactivePower(), is(equalTo(60.0f)));
		assertThat("Reactive power time window", device.getReactivePowerPercentTimeWindow(),
				is(equalTo(25)));
		assertThat("Reactive power reversion time", device.getReactivePowerPercentReversionTime(),
				is(equalTo(1800)));
		assertThat("Reactive power ramp time", device.getReactivePowerPercentRampTime(),
				is(equalTo(35)));
		assertThat("Reactive power mode", device.getReactivePowerPercentMode(),
				is(equalTo(InverterReactivePowerPercentMode.AvailableReactivePowerPercent)));
		assertThat("Reactive power enabled", device.isReactivePowerPercentEnabled(), is(equalTo(true)));
	}

	@Test
	public void writeValues_negativeScaleFactor() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				SMA_TEST_DATA);
		InverterImmediateControlsModelAccessor model = discoverModel(conn);

		// WHEN
		model.setActivePowerLimitPercent(conn, 62.5f);
		model.setFixedPowerFactor(conn, 0.9876f);
		model.setReactivePowerPercentOfMaximumActivePower(conn, 12.34f);

		// THEN
		InverterImmediateControlsModelAccessor device = discoverModel(conn);
		assertThat("Active power limit", device.getActivePowerLimitPercent(), is(equalTo(62.5f)));
		assertThat("Fixed power factor", device.getFixedPowerFactor(), is(equalTo(0.9876f)));
		assertThat("Reactive power of maximum active power",
				device.getReactivePowerPercentOfMaximumActivePower(), is(equalTo(12.34f)));
	}

	@Test
	public void writeValue_outOfRange() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				FRONIUS_TEST_DATA);
		InverterImmediateControlsModelAccessor model = discoverModel(conn);

		// WHEN
		try {
			model.setActivePowerLimitPercent(conn, 70000.0f);
			fail("Scaled value larger than uint16 should be rejected.");
		} catch ( IllegalArgumentException e ) {
			// expected
		}
		try {
			model.setConnectionTimeWindow(conn, -1);
			fail("Negative uint16 value should be rejected.");
		} catch ( IllegalArgumentException e ) {
			// expected
		}

		// THEN
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

}

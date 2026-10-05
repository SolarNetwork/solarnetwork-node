/* ==================================================================
 * InverterBasicStorageControlsModelAccessorImplTests.java - 6/10/2026 10:36:02 am
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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.inverter.InverterBasicStorageControlsModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterBasicStorageControlsModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.inverter.InverterChargeSource;
import net.solarnetwork.node.hw.sunspec.inverter.InverterControlModelId;
import net.solarnetwork.node.hw.sunspec.inverter.InverterStorageControlMode;
import net.solarnetwork.node.hw.sunspec.storage.BatteryChargeStatus;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link InverterBasicStorageControlsModelAccessorImpl}
 * class.
 *
 * @author matt
 * @version 1.0
 */
public class InverterBasicStorageControlsModelAccessorImplTests {

	/** An SMA capture. */
	private static final String TEST_DATA = "test-data-103-05.txt";

	/** The model block address in the test data. */
	private static final int BLOCK_ADDRESS = 371;

	/** Synthetic values for the whole model block. */
	// @formatter:off
	private static final int[] SYNTHETIC_BLOCK = new int[] {
			0x1388, // WChaMax
			0x03E8, // WChaGra
			0x07D0, // WDisChaGra
			0x0003, // StorCtl_Mod
			0x0226, // VAChaMax
			0x0096, // MinRsvPct
			0x0355, // ChaState
			0x04D2, // StorAval
			0x1400, // InBatV
			0x0004, // ChaSt
			0x02EE, // OutWRte
			0xFF06, // InWRte
			0x003C, // InOutWRte_WinTms
			0x0258, // InOutWRte_RvrtTms
			0x001E, // InOutWRte_RmpTms
			0x0001, // ChaGriSet
			0x0000, // WChaMax_SF
			0xFFFE, // WChaDisChaGra_SF
			0x0001, // VAChaMax_SF
			0xFFFF, // MinRsvPct_SF
			0xFFFF, // ChaState_SF
			0xFFFF, // StorAval_SF
			0xFFFE, // InBatV_SF
			0xFFFF, // InOutWRte_SF
	};
	// @formatter:on

	private InverterBasicStorageControlsModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(InverterBasicStorageControlsModelAccessor.class);
	}

	private static InverterBasicStorageControlsModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn)
				.findTypedModel(InverterBasicStorageControlsModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(InverterBasicStorageControlsModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		InverterBasicStorageControlsModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(369)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(BLOCK_ADDRESS)));
		assertThat("Model ID", model.getModelId(),
				is(equalTo(InverterControlModelId.BasicStorageControls)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(24)));
		assertThat("Model length", model.getModelLength(), is(equalTo(24)));
	}

	@Test
	public void values() {
		// the device implements the points, but has no storage
		InverterBasicStorageControlsModelAccessor model = getTestModel();
		assertThat("Maximum charge rate not implemented", model.getActivePowerChargeRateMaximum(),
				is(nullValue()));
		assertThat("Charge ramp rate not implemented", model.getChargeRampRate(), is(nullValue()));
		assertThat("Discharge ramp rate not implemented", model.getDischargeRampRate(), is(nullValue()));
		assertThat("Storage control modes", model.getStorageControlModes(), is(equalTo(Set.of())));
		assertThat("Maximum charge apparent power not implemented",
				model.getApparentPowerChargeRateMaximum(), is(nullValue()));
		assertThat("Minimum reserve not implemented", model.getStateOfChargeReserveMinimum(),
				is(nullValue()));
		assertThat("State of charge not implemented", model.getStateOfCharge(), is(nullValue()));
		assertThat("Storage available not implemented", model.getStorageAvailable(), is(nullValue()));
		assertThat("Battery voltage not implemented", model.getBatteryVoltage(), is(nullValue()));
		assertThat("Charge status not implemented", model.getChargeStatus(), is(nullValue()));
		assertThat("Discharge rate not implemented", model.getDischargeRatePercent(), is(nullValue()));
		assertThat("Charge rate not implemented", model.getChargeRatePercent(), is(nullValue()));
		assertThat("Rate time window not implemented", model.getChargeDischargeRateTimeWindow(),
				is(nullValue()));
		assertThat("Rate reversion time not implemented", model.getChargeDischargeRateReversionTime(),
				is(nullValue()));
		assertThat("Rate ramp time not implemented", model.getChargeDischargeRateRampTime(),
				is(nullValue()));
		assertThat("Charge source not implemented", model.getChargeSource(), is(nullValue()));
	}

	@Test
	public void syntheticValues() {
		// GIVEN
		InverterBasicStorageControlsModelAccessor model = ModelDataUtils
				.getModelDataInstanceWithRegisters(getClass(), TEST_DATA, BLOCK_ADDRESS, SYNTHETIC_BLOCK)
				.findTypedModel(InverterBasicStorageControlsModelAccessor.class);

		// THEN
		assertThat("Maximum charge rate", model.getActivePowerChargeRateMaximum(), is(equalTo(5000)));
		assertThat("Charge ramp rate", model.getChargeRampRate(), is(equalTo(10.0f)));
		assertThat("Discharge ramp rate", model.getDischargeRampRate(), is(equalTo(20.0f)));
		assertThat("Storage control modes", model.getStorageControlModes(), is(equalTo(
				EnumSet.of(InverterStorageControlMode.Charge, InverterStorageControlMode.Discharge))));
		assertThat("Maximum charge apparent power", model.getApparentPowerChargeRateMaximum(),
				is(equalTo(5500)));
		assertThat("Minimum reserve", model.getStateOfChargeReserveMinimum(), is(equalTo(15.0f)));
		assertThat("State of charge", model.getStateOfCharge(), is(equalTo(85.3f)));
		assertThat("Storage available", model.getStorageAvailable(), is(equalTo(123.4f)));
		assertThat("Battery voltage", model.getBatteryVoltage(), is(equalTo(51.2f)));
		assertThat("Charge status", model.getChargeStatus(), is(equalTo(BatteryChargeStatus.Charging)));
		assertThat("Discharge rate", model.getDischargeRatePercent(), is(equalTo(75.0f)));
		assertThat("Charge rate", model.getChargeRatePercent(), is(equalTo(-25.0f)));
		assertThat("Rate time window", model.getChargeDischargeRateTimeWindow(), is(equalTo(60)));
		assertThat("Rate reversion time", model.getChargeDischargeRateReversionTime(), is(equalTo(600)));
		assertThat("Rate ramp time", model.getChargeDischargeRateRampTime(), is(equalTo(30)));
		assertThat("Charge source", model.getChargeSource(), is(equalTo(InverterChargeSource.Grid)));
	}

	@Test
	public void writeValues() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnectionWithRegisters(
				getClass(), TEST_DATA, BLOCK_ADDRESS, SYNTHETIC_BLOCK);
		InverterBasicStorageControlsModelAccessor model = discoverModel(conn);

		// WHEN
		model.setActivePowerChargeRateMaximum(conn, 4000);
		model.setChargeRampRate(conn, 12.5f);
		model.setDischargeRampRate(conn, 7.25f);
		model.setStorageControlModes(conn, EnumSet.of(InverterStorageControlMode.Discharge));
		model.setApparentPowerChargeRateMaximum(conn, 4400);
		model.setStateOfChargeReserveMinimum(conn, 20.5f);
		model.setDischargeRatePercent(conn, 33.3f);
		model.setChargeRatePercent(conn, -12.5f);
		model.setChargeDischargeRateTimeWindow(conn, 120);
		model.setChargeDischargeRateReversionTime(conn, 3600);
		model.setChargeDischargeRateRampTime(conn, 45);
		model.setChargeSource(conn, InverterChargeSource.Pv);

		// THEN
		assertThat("Each point written to its own register", conn.getWrites(),
				is(equalTo(List.of(List.of(371, 1), List.of(372, 1), List.of(373, 1), List.of(374, 1),
						List.of(375, 1), List.of(376, 1), List.of(381, 1), List.of(382, 1),
						List.of(383, 1), List.of(384, 1), List.of(385, 1), List.of(386, 1)))));

		InverterBasicStorageControlsModelAccessor device = discoverModel(conn);
		assertThat("Maximum charge rate", device.getActivePowerChargeRateMaximum(), is(equalTo(4000)));
		assertThat("Charge ramp rate", device.getChargeRampRate(), is(equalTo(12.5f)));
		assertThat("Discharge ramp rate", device.getDischargeRampRate(), is(equalTo(7.25f)));
		assertThat("Storage control modes", device.getStorageControlModes(),
				is(equalTo(EnumSet.of(InverterStorageControlMode.Discharge))));
		assertThat("Maximum charge apparent power", device.getApparentPowerChargeRateMaximum(),
				is(equalTo(4400)));
		assertThat("Minimum reserve", device.getStateOfChargeReserveMinimum(), is(equalTo(20.5f)));
		assertThat("Discharge rate", device.getDischargeRatePercent(), is(equalTo(33.3f)));
		assertThat("Charge rate", device.getChargeRatePercent(), is(equalTo(-12.5f)));
		assertThat("Rate time window", device.getChargeDischargeRateTimeWindow(), is(equalTo(120)));
		assertThat("Rate reversion time", device.getChargeDischargeRateReversionTime(),
				is(equalTo(3600)));
		assertThat("Rate ramp time", device.getChargeDischargeRateRampTime(), is(equalTo(45)));
		assertThat("Charge source", device.getChargeSource(), is(equalTo(InverterChargeSource.Pv)));
		assertThat("Read-only state of charge unchanged", device.getStateOfCharge(), is(equalTo(85.3f)));
	}

	@Test
	public void writeValues_noModes() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnectionWithRegisters(
				getClass(), TEST_DATA, BLOCK_ADDRESS, SYNTHETIC_BLOCK);
		InverterBasicStorageControlsModelAccessor model = discoverModel(conn);

		// WHEN
		model.setStorageControlModes(conn, Set.of());

		// THEN
		assertThat("Storage control modes cleared", discoverModel(conn).getStorageControlModes(),
				is(equalTo(Set.of())));
	}

	@Test
	public void writeValue_scaleFactorNotImplemented() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		InverterBasicStorageControlsModelAccessor model = discoverModel(conn);

		// WHEN
		try {
			model.setChargeRampRate(conn, 10.0f);
			fail("Scaled value without an implemented scale factor should be rejected.");
		} catch ( IllegalStateException e ) {
			// expected
		}
		model.setActivePowerChargeRateMaximum(conn, 4000);

		// THEN
		assertThat("Only the point with an implemented scale factor written", conn.getWrites(),
				is(equalTo(List.of(List.of(371, 1)))));
		assertThat("Maximum charge rate", discoverModel(conn).getActivePowerChargeRateMaximum(),
				is(equalTo(4000)));
	}

}

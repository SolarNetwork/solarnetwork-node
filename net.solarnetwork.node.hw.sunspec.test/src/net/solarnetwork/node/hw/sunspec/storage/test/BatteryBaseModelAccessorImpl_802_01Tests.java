/* ==================================================================
 * BatteryBaseModelAccessorImpl_802_01Tests.java - 5/10/2026 8:21:46 pm
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

package net.solarnetwork.node.hw.sunspec.storage.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.time.LocalDate;
import java.util.BitSet;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.der.DerLocalRemoteControl;
import net.solarnetwork.node.hw.sunspec.storage.BatteryBaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.storage.BatteryBaseModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.storage.BatteryChargeStatus;
import net.solarnetwork.node.hw.sunspec.storage.BatteryEvent;
import net.solarnetwork.node.hw.sunspec.storage.BatteryInverterState;
import net.solarnetwork.node.hw.sunspec.storage.BatteryInverterStateRequest;
import net.solarnetwork.node.hw.sunspec.storage.BatteryOperation;
import net.solarnetwork.node.hw.sunspec.storage.BatteryState;
import net.solarnetwork.node.hw.sunspec.storage.BatteryType;
import net.solarnetwork.node.hw.sunspec.storage.StorageModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link BatteryBaseModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class BatteryBaseModelAccessorImpl_802_01Tests {

	private static final String TEST_DATA = "test-data-802-01.txt";

	/** The model block address. */
	private static final int BLOCK_ADDRESS = 72;

	private BatteryBaseModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(BatteryBaseModelAccessor.class);
	}

	private BatteryBaseModelAccessor getTestModel(int address, int... words) {
		return ModelDataUtils.getModelDataInstanceWithRegisters(getClass(), TEST_DATA, address, words)
				.findTypedModel(BatteryBaseModelAccessor.class);
	}

	private static BatteryBaseModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn).findTypedModel(BatteryBaseModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(BatteryBaseModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		BatteryBaseModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(70)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(BLOCK_ADDRESS)));
		assertThat("Model ID", model.getModelId(), is(equalTo(StorageModelId.BatteryBase)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(62)));
		assertThat("Model length", model.getModelLength(), is(equalTo(62)));
	}

	@Test
	public void ratings() {
		BatteryBaseModelAccessor model = getTestModel();
		assertThat("Charge capacity", model.getChargeCapacityRating(), is(equalTo(100.0f)));
		assertThat("Energy capacity", model.getEnergyCapacityRating(), is(equalTo(13500L)));
		assertThat("Maximum charge rate", model.getChargeRateMaximumRating(), is(equalTo(5000)));
		assertThat("Maximum discharge rate", model.getDischargeRateMaximumRating(), is(equalTo(7000)));
		assertThat("Self discharge rate", model.getSelfDischargeRate(), is(equalTo(0.5f)));
		assertThat("Maximum state of charge", model.getStateOfChargeMaximumRating(),
				is(equalTo(100.0f)));
		assertThat("Minimum state of charge", model.getStateOfChargeMinimumRating(), is(equalTo(5.0f)));
		assertThat("Maximum reserve", model.getStateOfChargeReserveMaximum(), is(equalTo(95.0f)));
		assertThat("Minimum reserve", model.getStateOfChargeReserveMinimum(), is(equalTo(10.0f)));
	}

	@Test
	public void status() {
		BatteryBaseModelAccessor model = getTestModel();
		assertThat("State of charge", model.getStateOfCharge(), is(equalTo(87.3f)));
		assertThat("Depth of discharge", model.getDepthOfDischarge(), is(equalTo(12.7f)));
		assertThat("State of health", model.getStateOfHealth(), is(equalTo(98.5f)));
		assertThat("Cycle count", model.getCycleCount(), is(equalTo(312L)));
		assertThat("Charge status", model.getChargeStatus(),
				is(equalTo(BatteryChargeStatus.Discharging)));
		assertThat("Local or remote control", model.getLocalRemoteControl(),
				is(equalTo(DerLocalRemoteControl.Remote)));
		assertThat("Battery heartbeat", model.getBatteryHeartbeat(), is(equalTo(4242)));
		assertThat("Controller heartbeat", model.getControllerHeartbeat(), is(equalTo(4240)));
		assertThat("Alarm reset in progress", model.isAlarmResetInProgress(), is(equalTo(false)));
		assertThat("Battery type", model.getBatteryType(), is(equalTo(BatteryType.LithiumIon)));
		assertThat("Battery state", model.getBatteryState(), is(equalTo(BatteryState.Connected)));
		assertThat("Vendor battery state not implemented", model.getVendorBatteryState(),
				is(nullValue()));
		assertThat("Warranty date, 10000 days after 1 January 2000", model.getWarrantyDate(),
				is(equalTo(LocalDate.of(2027, 5, 19))));
	}

	@Test
	public void events() {
		BatteryBaseModelAccessor model = getTestModel();
		assertThat("Events", model.getEvents(), is(equalTo(Set.of(BatteryEvent.OverTemperatureWarning,
				BatteryEvent.VoltageImbalanceWarning, BatteryEvent.Reserved1))));
		BitSet vendorEvents = new BitSet();
		vendorEvents.set(0);
		vendorEvents.set(2);
		vendorEvents.set(32);
		assertThat("Vendor events, with the second field offset by 32", model.getVendorEvents(),
				is(equalTo(vendorEvents)));
	}

	@Test
	public void events_mostSignificantBit() {
		// GIVEN
		// Evt1 at offset 24 and EvtVnd2 at offset 30, each with the most significant bit set
		BatteryBaseModelAccessor model = getTestModel(BLOCK_ADDRESS + 24, 0x8000, 0x0004, 0, 0, 0,
				0x0005, 0x8000, 0x0001);

		// THEN
		assertThat("Events not implemented", model.getEvents(), is(equalTo(Set.of())));
		BitSet vendorEvents = new BitSet();
		vendorEvents.set(0);
		vendorEvents.set(2);
		assertThat("Second vendor event field not implemented", model.getVendorEvents(),
				is(equalTo(vendorEvents)));
	}

	@Test
	public void warrantyDate_notImplemented() {
		// GIVEN
		BatteryBaseModelAccessor model = getTestModel(BLOCK_ADDRESS + 22, 0xFFFF, 0xFFFF);

		// THEN
		assertThat("Warranty date not implemented", model.getWarrantyDate(), is(nullValue()));
	}

	@Test
	public void measurements() {
		BatteryBaseModelAccessor model = getTestModel();
		assertThat("DC voltage", model.getDCVoltage(), is(equalTo(51.2f)));
		assertThat("Maximum voltage", model.getMaximumVoltage(), is(equalTo(57.6f)));
		assertThat("Minimum voltage", model.getMinimumVoltage(), is(equalTo(44.8f)));
		assertThat("Maximum cell voltage", model.getMaximumCellVoltage(), is(equalTo(3.35f)));
		assertThat("Maximum cell voltage string", model.getMaximumCellVoltageStringIndex(),
				is(equalTo(1)));
		assertThat("Maximum cell voltage module", model.getMaximumCellVoltageModuleIndex(),
				is(equalTo(4)));
		assertThat("Minimum cell voltage", model.getMinimumCellVoltage(), is(equalTo(3.31f)));
		assertThat("Minimum cell voltage string", model.getMinimumCellVoltageStringIndex(),
				is(equalTo(2)));
		assertThat("Minimum cell voltage module", model.getMinimumCellVoltageModuleIndex(),
				is(equalTo(7)));
		assertThat("Average cell voltage", model.getAverageCellVoltage(), is(equalTo(3.33f)));
		assertThat("DC current", model.getDCCurrent(), is(equalTo(-45.6f)));
		assertThat("Maximum charge current", model.getMaximumChargeCurrent(), is(equalTo(100.0f)));
		assertThat("Maximum discharge current", model.getMaximumDischargeCurrent(), is(equalTo(150.0f)));
		assertThat("DC power", model.getDCPower(), is(equalTo(-2335)));
	}

	@Test
	public void requestsAndCommands() {
		BatteryBaseModelAccessor model = getTestModel();
		assertThat("Inverter state request", model.getInverterStateRequest(),
				is(equalTo(BatteryInverterStateRequest.NoRequest)));
		assertThat("Power request", model.getPowerRequest(), is(equalTo(0)));
		assertThat("Operation", model.getOperation(), is(equalTo(BatteryOperation.Connect)));
		assertThat("Inverter state", model.getInverterState(),
				is(equalTo(BatteryInverterState.Started)));
	}

	@Test
	public void writeSettings() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		BatteryBaseModelAccessor model = discoverModel(conn);

		// WHEN
		model.setStateOfChargeReserveMaximum(conn, 90.5f);
		model.setStateOfChargeReserveMinimum(conn, 12.0f);
		model.setControllerHeartbeat(conn, 4241);
		model.resetAlarms(conn);
		model.setOperation(conn, BatteryOperation.Disconnect);
		model.setInverterState(conn, BatteryInverterState.Standby);

		// THEN
		BatteryBaseModelAccessor device = discoverModel(conn);
		assertThat("Maximum reserve", device.getStateOfChargeReserveMaximum(), is(equalTo(90.5f)));
		assertThat("Minimum reserve", device.getStateOfChargeReserveMinimum(), is(equalTo(12.0f)));
		assertThat("Controller heartbeat", device.getControllerHeartbeat(), is(equalTo(4241)));
		assertThat("Alarm reset in progress", device.isAlarmResetInProgress(), is(equalTo(true)));
		assertThat("Operation", device.getOperation(), is(equalTo(BatteryOperation.Disconnect)));
		assertThat("Inverter state", device.getInverterState(),
				is(equalTo(BatteryInverterState.Standby)));
		assertThat("Battery heartbeat unchanged", device.getBatteryHeartbeat(), is(equalTo(4242)));
	}

}

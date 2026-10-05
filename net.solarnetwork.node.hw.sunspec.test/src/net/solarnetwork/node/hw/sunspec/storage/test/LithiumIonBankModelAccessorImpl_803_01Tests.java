/* ==================================================================
 * LithiumIonBankModelAccessorImpl_803_01Tests.java - 5/10/2026 9:40:12 pm
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.storage.BatteryConnectionFailure;
import net.solarnetwork.node.hw.sunspec.storage.BatteryConnectionStatus;
import net.solarnetwork.node.hw.sunspec.storage.BatteryDisabledReason;
import net.solarnetwork.node.hw.sunspec.storage.BatteryEnableOperation;
import net.solarnetwork.node.hw.sunspec.storage.BatteryOperation;
import net.solarnetwork.node.hw.sunspec.storage.LithiumIonBankModelAccessor;
import net.solarnetwork.node.hw.sunspec.storage.LithiumIonBankModelAccessor.BatteryString;
import net.solarnetwork.node.hw.sunspec.storage.LithiumIonBankModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.storage.LithiumIonStringEvent;
import net.solarnetwork.node.hw.sunspec.storage.StorageModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link LithiumIonBankModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class LithiumIonBankModelAccessorImpl_803_01Tests {

	private static final String TEST_DATA = "test-data-803-01.txt";

	/** The model block address. */
	private static final int BLOCK_ADDRESS = 72;

	/**
	 * The string 2 address: 26 fixed registers, then 32 registers per string.
	 */
	private static final int STRING_2_ADDRESS = BLOCK_ADDRESS + 26 + 32;

	private LithiumIonBankModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(LithiumIonBankModelAccessor.class);
	}

	private static LithiumIonBankModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn)
				.findTypedModel(LithiumIonBankModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(LithiumIonBankModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		LithiumIonBankModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(70)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(BLOCK_ADDRESS)));
		assertThat("Model ID", model.getModelId(), is(equalTo(StorageModelId.LithiumIonBank)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(26)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(32)));
		assertThat("Model repeating instance count", model.getRepeatingBlockInstanceCount(),
				is(equalTo(2)));
		assertThat("Model length", model.getModelLength(), is(equalTo(90)));
	}

	@Test
	public void bank() {
		LithiumIonBankModelAccessor model = getTestModel();
		assertThat("String count", model.getStringCount(), is(equalTo(2)));
		assertThat("Connected string count", model.getConnectedStringCount(), is(equalTo(1)));
		assertThat("Maximum module temperature", model.getMaximumModuleTemperature(),
				is(equalTo(28.5f)));
		assertThat("Maximum module temperature string", model.getMaximumModuleTemperatureStringIndex(),
				is(equalTo(1)));
		assertThat("Maximum module temperature module", model.getMaximumModuleTemperatureModuleIndex(),
				is(equalTo(3)));
		assertThat("Minimum module temperature", model.getMinimumModuleTemperature(),
				is(equalTo(22.1f)));
		assertThat("Minimum module temperature string", model.getMinimumModuleTemperatureStringIndex(),
				is(equalTo(2)));
		assertThat("Minimum module temperature module", model.getMinimumModuleTemperatureModuleIndex(),
				is(equalTo(1)));
		assertThat("Average module temperature", model.getAverageModuleTemperature(),
				is(equalTo(25.0f)));
		assertThat("Maximum string voltage", model.getMaximumStringVoltage(), is(equalTo(52.4f)));
		assertThat("Maximum string voltage string", model.getMaximumStringVoltageStringIndex(),
				is(equalTo(1)));
		assertThat("Minimum string voltage", model.getMinimumStringVoltage(), is(equalTo(51.8f)));
		assertThat("Minimum string voltage string", model.getMinimumStringVoltageStringIndex(),
				is(equalTo(2)));
		assertThat("Average string voltage", model.getAverageStringVoltage(), is(equalTo(52.1f)));
		assertThat("Maximum string current", model.getMaximumStringCurrent(), is(equalTo(12.3f)));
		assertThat("Maximum string current string", model.getMaximumStringCurrentStringIndex(),
				is(equalTo(1)));
		assertThat("Minimum string current", model.getMinimumStringCurrent(), is(equalTo(-1.5f)));
		assertThat("Minimum string current string", model.getMinimumStringCurrentStringIndex(),
				is(equalTo(2)));
		assertThat("Average string current", model.getAverageStringCurrent(), is(equalTo(5.4f)));
		assertThat("Balancing cell count", model.getBalancingCellCount(), is(equalTo(3)));
	}

	@Test
	public void connectedString() {
		List<BatteryString> strings = getTestModel().getStrings();
		assertThat("Strings", strings, hasSize(2));

		BatteryString string = strings.get(0);
		assertThat("Index", string.getIndex(), is(equalTo(1)));
		assertThat("Module count", string.getModuleCount(), is(equalTo(8)));
		assertThat("Status", string.getStatus(), is(equalTo(
				Set.of(BatteryConnectionStatus.Enabled, BatteryConnectionStatus.ContactorClosed))));
		assertThat("Connection failure", string.getConnectionFailure(),
				is(equalTo(BatteryConnectionFailure.None)));
		assertThat("State of charge", string.getStateOfCharge(), is(equalTo(76.5f)));
		assertThat("State of health", string.getStateOfHealth(), is(equalTo(97.0f)));
		assertThat("DC current", string.getDCCurrent(), is(equalTo(12.3f)));
		assertThat("Maximum cell voltage", string.getMaximumCellVoltage(), is(equalTo(3.352f)));
		assertThat("Maximum cell voltage module", string.getMaximumCellVoltageModuleIndex(),
				is(equalTo(3)));
		assertThat("Minimum cell voltage", string.getMinimumCellVoltage(), is(equalTo(3.301f)));
		assertThat("Minimum cell voltage module", string.getMinimumCellVoltageModuleIndex(),
				is(equalTo(6)));
		assertThat("Average cell voltage", string.getAverageCellVoltage(), is(equalTo(3.33f)));
		assertThat("Maximum module temperature", string.getMaximumModuleTemperature(),
				is(equalTo(28.5f)));
		assertThat("Maximum module temperature module", string.getMaximumModuleTemperatureModuleIndex(),
				is(equalTo(3)));
		assertThat("Minimum module temperature", string.getMinimumModuleTemperature(),
				is(equalTo(24.0f)));
		assertThat("Minimum module temperature module", string.getMinimumModuleTemperatureModuleIndex(),
				is(equalTo(8)));
		assertThat("Average module temperature", string.getAverageModuleTemperature(),
				is(equalTo(26.2f)));
		assertThat("Disabled reason", string.getDisabledReason(),
				is(equalTo(BatteryDisabledReason.None)));
		assertThat("Closed contactors", string.getClosedContactors(), is(equalTo(Set.of(0, 2))));
		assertThat("Events, bit 24 reserved for strings", string.getEvents(),
				is(equalTo(Set.of(LithiumIonStringEvent.Reserved1))));
		BitSet vendorEvents = new BitSet();
		vendorEvents.set(1);
		assertThat("Vendor events", string.getVendorEvents(), is(equalTo(vendorEvents)));
		assertThat("No enable operation in progress", string.getEnableOperation(), is(nullValue()));
		assertThat("No connect operation in progress", string.getConnectOperation(), is(nullValue()));
	}

	@Test
	public void disabledString() {
		BatteryString string = getTestModel().getStrings().get(1);
		assertThat("Index", string.getIndex(), is(equalTo(2)));
		assertThat("Status", string.getStatus(), is(equalTo(Set.of())));
		assertThat("Connection failure", string.getConnectionFailure(),
				is(equalTo(BatteryConnectionFailure.NotEnabled)));
		assertThat("State of charge not implemented", string.getStateOfCharge(), is(nullValue()));
		assertThat("DC current", string.getDCCurrent(), is(equalTo(-1.5f)));
		assertThat("Disabled reason", string.getDisabledReason(),
				is(equalTo(BatteryDisabledReason.External)));
		assertThat("Closed contactors", string.getClosedContactors(), is(equalTo(Set.of())));
		assertThat("Events with the most significant bit set not implemented", string.getEvents(),
				is(equalTo(Set.of())));
		BitSet vendorEvents = new BitSet();
		vendorEvents.set(34);
		assertThat("Vendor events, with the second field offset by 32", string.getVendorEvents(),
				is(equalTo(vendorEvents)));
		assertThat("Enable operation not implemented", string.getEnableOperation(), is(nullValue()));
	}

	@Test
	public void writeStringOperations() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		BatteryString string = discoverModel(conn).getStrings().get(1);

		// WHEN
		string.setEnableOperation(conn, BatteryEnableOperation.Enable);
		string.setConnectOperation(conn, BatteryOperation.Connect);

		// THEN
		assertThat("Writes", conn.getWrites(), is(
				equalTo(List.of(List.of(STRING_2_ADDRESS + 28, 1), List.of(STRING_2_ADDRESS + 29, 1)))));
		BatteryString device = discoverModel(conn).getStrings().get(1);
		assertThat("Enable operation", device.getEnableOperation(),
				is(equalTo(BatteryEnableOperation.Enable)));
		assertThat("Connect operation", device.getConnectOperation(),
				is(equalTo(BatteryOperation.Connect)));
	}

}

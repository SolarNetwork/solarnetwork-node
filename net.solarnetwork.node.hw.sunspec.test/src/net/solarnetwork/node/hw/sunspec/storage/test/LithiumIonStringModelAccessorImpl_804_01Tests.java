/* ==================================================================
 * LithiumIonStringModelAccessorImpl_804_01Tests.java - 5/10/2026 9:40:12 pm
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
import net.solarnetwork.node.hw.sunspec.storage.BatteryEnableOperation;
import net.solarnetwork.node.hw.sunspec.storage.BatteryOperation;
import net.solarnetwork.node.hw.sunspec.storage.LithiumIonStringEvent;
import net.solarnetwork.node.hw.sunspec.storage.LithiumIonStringModelAccessor;
import net.solarnetwork.node.hw.sunspec.storage.LithiumIonStringModelAccessor.BatteryModule;
import net.solarnetwork.node.hw.sunspec.storage.LithiumIonStringModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.storage.StorageModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link LithiumIonStringModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class LithiumIonStringModelAccessorImpl_804_01Tests {

	private static final String TEST_DATA = "test-data-804-01.txt";

	private LithiumIonStringModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(LithiumIonStringModelAccessor.class);
	}

	private static LithiumIonStringModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn)
				.findTypedModel(LithiumIonStringModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(LithiumIonStringModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		LithiumIonStringModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(70)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(72)));
		assertThat("Model ID", model.getModelId(), is(equalTo(StorageModelId.LithiumIonString)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(46)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(16)));
		assertThat("Model length", model.getModelLength(), is(equalTo(94)));
	}

	@Test
	public void string() {
		LithiumIonStringModelAccessor model = getTestModel();
		assertThat("String index", model.getStringIndex(), is(equalTo(1)));
		assertThat("Module count", model.getModuleCount(), is(equalTo(3)));
		assertThat("Status", model.getStatus(), is(equalTo(
				Set.of(BatteryConnectionStatus.Enabled, BatteryConnectionStatus.ContactorClosed))));
		assertThat("Connection failure", model.getConnectionFailure(),
				is(equalTo(BatteryConnectionFailure.None)));
		assertThat("Balancing cell count", model.getBalancingCellCount(), is(equalTo(2)));
		assertThat("State of charge", model.getStateOfCharge(), is(equalTo(76.5f)));
		assertThat("Depth of discharge", model.getDepthOfDischarge(), is(equalTo(23.5f)));
		assertThat("Cycle count", model.getCycleCount(), is(equalTo(145L)));
		assertThat("State of health", model.getStateOfHealth(), is(equalTo(97.0f)));
		assertThat("DC current", model.getDCCurrent(), is(equalTo(12.3f)));
		assertThat("DC voltage", model.getDCVoltage(), is(equalTo(52.4f)));
		assertThat("Maximum cell voltage", model.getMaximumCellVoltage(), is(equalTo(3.352f)));
		assertThat("Maximum cell voltage module", model.getMaximumCellVoltageModuleIndex(),
				is(equalTo(2)));
		assertThat("Minimum cell voltage", model.getMinimumCellVoltage(), is(equalTo(3.301f)));
		assertThat("Minimum cell voltage module", model.getMinimumCellVoltageModuleIndex(),
				is(equalTo(3)));
		assertThat("Average cell voltage", model.getAverageCellVoltage(), is(equalTo(3.33f)));
		assertThat("Maximum module temperature", model.getMaximumModuleTemperature(),
				is(equalTo(28.5f)));
		assertThat("Maximum module temperature module", model.getMaximumModuleTemperatureModuleIndex(),
				is(equalTo(2)));
		assertThat("Minimum module temperature", model.getMinimumModuleTemperature(),
				is(equalTo(24.0f)));
		assertThat("Minimum module temperature module", model.getMinimumModuleTemperatureModuleIndex(),
				is(equalTo(1)));
		assertThat("Average module temperature", model.getAverageModuleTemperature(),
				is(equalTo(26.2f)));
		assertThat("Closed contactors", model.getClosedContactors(), is(equalTo(Set.of(0))));
		assertThat("Events", model.getEvents(),
				is(equalTo(Set.of(LithiumIonStringEvent.OverVoltageWarning))));
		assertThat("Vendor events", model.getVendorEvents(), is(equalTo(new BitSet())));
		assertThat("Enable operation in progress", model.getEnableOperation(),
				is(equalTo(BatteryEnableOperation.Enable)));
		assertThat("No connect operation in progress", model.getConnectOperation(), is(nullValue()));
	}

	@Test
	public void modules() {
		List<BatteryModule> modules = getTestModel().getModules();
		assertThat("Modules", modules, hasSize(3));

		BatteryModule module = modules.get(0);
		assertThat("Index", module.getIndex(), is(equalTo(1)));
		assertThat("Cell count", module.getCellCount(), is(equalTo(16)));
		assertThat("State of charge", module.getStateOfCharge(), is(equalTo(77.0f)));
		assertThat("State of health", module.getStateOfHealth(), is(equalTo(98.0f)));
		assertThat("Maximum cell voltage", module.getMaximumCellVoltage(), is(equalTo(3.345f)));
		assertThat("Maximum cell voltage cell", module.getMaximumCellVoltageCellIndex(), is(equalTo(5)));
		assertThat("Minimum cell voltage", module.getMinimumCellVoltage(), is(equalTo(3.31f)));
		assertThat("Minimum cell voltage cell", module.getMinimumCellVoltageCellIndex(),
				is(equalTo(12)));
		assertThat("Average cell voltage", module.getAverageCellVoltage(), is(equalTo(3.33f)));
		assertThat("Maximum cell temperature", module.getMaximumCellTemperature(), is(equalTo(27.0f)));
		assertThat("Maximum cell temperature cell", module.getMaximumCellTemperatureCellIndex(),
				is(equalTo(1)));
		assertThat("Minimum cell temperature", module.getMinimumCellTemperature(), is(equalTo(24.0f)));
		assertThat("Minimum cell temperature cell", module.getMinimumCellTemperatureCellIndex(),
				is(equalTo(16)));
		assertThat("Average cell temperature", module.getAverageCellTemperature(), is(equalTo(25.5f)));

		BatteryModule module3 = modules.get(2);
		assertThat("Module 3 index", module3.getIndex(), is(equalTo(3)));
		assertThat("Module 3 state of charge not implemented", module3.getStateOfCharge(),
				is(nullValue()));
		assertThat("Module 3 state of health not implemented", module3.getStateOfHealth(),
				is(nullValue()));
		assertThat("Module 3 maximum cell voltage", module3.getMaximumCellVoltage(), is(equalTo(3.34f)));
	}

	@Test
	public void writeStringOperations() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		LithiumIonStringModelAccessor model = discoverModel(conn);

		// WHEN
		model.setEnableOperation(conn, BatteryEnableOperation.Disable);
		model.setConnectOperation(conn, BatteryOperation.Disconnect);

		// THEN
		assertThat("Writes", conn.getWrites(),
				is(equalTo(List.of(List.of(72 + 34, 1), List.of(72 + 35, 1)))));
		LithiumIonStringModelAccessor device = discoverModel(conn);
		assertThat("Enable operation", device.getEnableOperation(),
				is(equalTo(BatteryEnableOperation.Disable)));
		assertThat("Connect operation", device.getConnectOperation(),
				is(equalTo(BatteryOperation.Disconnect)));
	}

}

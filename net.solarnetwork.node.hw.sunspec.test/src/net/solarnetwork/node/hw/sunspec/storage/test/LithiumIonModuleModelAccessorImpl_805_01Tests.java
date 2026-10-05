/* ==================================================================
 * LithiumIonModuleModelAccessorImpl_805_01Tests.java - 5/10/2026 9:40:12 pm
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
import java.util.List;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.storage.LithiumIonCellStatus;
import net.solarnetwork.node.hw.sunspec.storage.LithiumIonModuleModelAccessor;
import net.solarnetwork.node.hw.sunspec.storage.LithiumIonModuleModelAccessor.BatteryCell;
import net.solarnetwork.node.hw.sunspec.storage.LithiumIonModuleModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.storage.StorageModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link LithiumIonModuleModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class LithiumIonModuleModelAccessorImpl_805_01Tests {

	private static final String TEST_DATA = "test-data-805-01.txt";

	private LithiumIonModuleModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(LithiumIonModuleModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(LithiumIonModuleModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		LithiumIonModuleModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(70)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(72)));
		assertThat("Model ID", model.getModelId(), is(equalTo(StorageModelId.LithiumIonModule)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(42)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(4)));
		assertThat("Model length", model.getModelLength(), is(equalTo(58)));
	}

	@Test
	public void module() {
		LithiumIonModuleModelAccessor model = getTestModel();
		assertThat("String index", model.getStringIndex(), is(equalTo(1)));
		assertThat("Module index", model.getModuleIndex(), is(equalTo(2)));
		assertThat("Cell count", model.getCellCount(), is(equalTo(4)));
		assertThat("State of charge", model.getStateOfCharge(), is(equalTo(76.5f)));
		assertThat("Depth of discharge", model.getDepthOfDischarge(), is(equalTo(23.5f)));
		assertThat("State of health", model.getStateOfHealth(), is(equalTo(97.0f)));
		assertThat("Cycle count", model.getCycleCount(), is(equalTo(145L)));
		assertThat("DC voltage", model.getDCVoltage(), is(equalTo(13.32f)));
		assertThat("Maximum cell voltage", model.getMaximumCellVoltage(), is(equalTo(3.335f)));
		assertThat("Maximum cell voltage cell", model.getMaximumCellVoltageCellIndex(), is(equalTo(3)));
		assertThat("Minimum cell voltage", model.getMinimumCellVoltage(), is(equalTo(3.32f)));
		assertThat("Minimum cell voltage cell", model.getMinimumCellVoltageCellIndex(), is(equalTo(1)));
		assertThat("Average cell voltage", model.getAverageCellVoltage(), is(equalTo(3.33f)));
		assertThat("Maximum cell temperature", model.getMaximumCellTemperature(), is(equalTo(26.2f)));
		assertThat("Maximum cell temperature cell", model.getMaximumCellTemperatureCellIndex(),
				is(equalTo(2)));
		assertThat("Minimum cell temperature", model.getMinimumCellTemperature(), is(equalTo(24.8f)));
		assertThat("Minimum cell temperature cell", model.getMinimumCellTemperatureCellIndex(),
				is(equalTo(4)));
		assertThat("Average cell temperature", model.getAverageCellTemperature(), is(equalTo(25.5f)));
		assertThat("Balancing cell count", model.getBalancingCellCount(), is(equalTo(1)));
		assertThat("Serial number", model.getSerialNumber(), is(equalTo("LIM-0002-ABC")));
	}

	@Test
	public void cells() {
		List<BatteryCell> cells = getTestModel().getCells();
		assertThat("Cells, from the model length", cells, hasSize(4));

		BatteryCell cell = cells.get(0);
		assertThat("Index", cell.getIndex(), is(equalTo(1)));
		assertThat("Voltage", cell.getVoltage(), is(equalTo(3.32f)));
		assertThat("Temperature", cell.getTemperature(), is(equalTo(25.0f)));
		assertThat("Status", cell.getStatus(), is(equalTo(Set.of())));

		assertThat("Cell 3 balancing", cells.get(2).getStatus(),
				is(equalTo(Set.of(LithiumIonCellStatus.Balancing))));
		assertThat("Cell 4 status with the most significant bit set not implemented",
				cells.get(3).getStatus(), is(equalTo(Set.of())));
		assertThat("Cell 4 voltage", cells.get(3).getVoltage(), is(equalTo(3.337f)));
	}

}

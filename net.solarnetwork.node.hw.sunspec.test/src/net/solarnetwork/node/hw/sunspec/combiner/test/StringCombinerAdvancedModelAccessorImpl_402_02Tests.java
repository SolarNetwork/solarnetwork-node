/* ==================================================================
 * StringCombinerAdvancedModelAccessorImpl_402_02Tests.java - 5/10/2026 7:44:39 am
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

package net.solarnetwork.node.hw.sunspec.combiner.test;

import static net.solarnetwork.node.hw.sunspec.combiner.test.StringCombinerTestUtils.assertAdvancedDcInput;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.GenericModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerAdvancedModelAccessor;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerAdvancedModelAccessor.AdvancedDcInput;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerAdvancedModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerModelEvent;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link StringCombinerAdvancedModelAccessorImpl} class,
 * using synthetic model 402 data with the 14 register input layout.
 *
 * @author matt
 * @version 1.0
 */
public class StringCombinerAdvancedModelAccessorImpl_402_02Tests {

	private StringCombinerAdvancedModelAccessor getTestModel() {
		ModelData data = ModelDataUtils.getModelDataInstance(getClass(), "test-data-402-02.txt");
		return data.findTypedModel(StringCombinerAdvancedModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), instanceOf(StringCombinerAdvancedModelAccessorImpl.class));
	}

	@Test
	public void block() {
		StringCombinerAdvancedModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(70)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(72)));
		assertThat("Model ID", model.getModelId(),
				is(equalTo(StringCombinerModelId.AdvancedStringCombiner)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(20)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(14)));
		assertThat("Model length", model.getModelLength(), is(equalTo(62)));
		assertThat("Model repeating instance count", model.getRepeatingBlockInstanceCount(),
				is(equalTo(3)));
	}

	@Test
	public void values() {
		StringCombinerAdvancedModelAccessor model = getTestModel();
		assertThat("Current", model.getDCCurrent(), is(equalTo(24.68f)));
		assertThat("Charge", model.getDCChargeDelivered(), is(equalTo(1000L)));
		assertThat("Voltage", model.getDCVoltage(), is(equalTo(601.2f)));
		assertThat("Temperature", model.getTemperature(), is(equalTo(31.0f)));
		assertThat("Power", model.getDCPower(), is(equalTo(14840)));
		assertThat("Energy 0xFFFFFFFF not implemented, a uint32 for model 402", model.getDCEnergy(),
				is(nullValue()));
		assertThat("Performance ratio", model.getDCPerformanceRatio(), is(equalTo(0.92f)));
	}

	@Test
	public void events() {
		StringCombinerAdvancedModelAccessor model = getTestModel();
		assertThat("Events", model.getEvents(),
				is(equalTo(Set.<ModelEvent> of(StringCombinerModelEvent.LowVoltage,
						StringCombinerModelEvent.GroundFault))));
		assertThat("Vendor events", model.getVendorEvents(),
				is(equalTo(Set.<ModelEvent> of(new GenericModelEvent(0), new GenericModelEvent(2)))));
	}

	@Test
	public void inputs() {
		List<AdvancedDcInput> inputs = getTestModel().getAdvancedDcInputs();
		assertThat("Inputs count", inputs, hasSize(3));

		// input power uses DCWh_SF and input energy is not scaled, per the model 402 definition
		assertAdvancedDcInput("Input 1", inputs.get(0), 1, 8.23f, 333L, 601.0f, 5000, 1234567L, 0.95f,
				12, Set.of(StringCombinerModelEvent.LowEfficiency), Set.of(new GenericModelEvent(16)));
		assertAdvancedDcInput("Input 2", inputs.get(1), 2, 8.22f, 0L, 601.4f, -100, null, null, 12,
				Set.of(), Set.of());
		assertAdvancedDcInput("Input 3", inputs.get(2), 3, null, null, null, null, 0L, 0.0f, null,
				Set.of(StringCombinerModelEvent.Disconnected), Set.of());
	}

}

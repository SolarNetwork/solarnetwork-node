/* ==================================================================
 * StringCombinerAdvancedModelAccessorImpl_404_01Tests.java - 5/10/2026 7:44:39 am
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
 * using synthetic model 404 data.
 *
 * @author matt
 * @version 1.0
 */
public class StringCombinerAdvancedModelAccessorImpl_404_01Tests {

	private StringCombinerAdvancedModelAccessor getTestModel() {
		ModelData data = ModelDataUtils.getModelDataInstance(getClass(), "test-data-404-01.txt");
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
				is(equalTo(StringCombinerModelId.AdvancedStringCombiner2)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(25)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(14)));
		assertThat("Model length", model.getModelLength(), is(equalTo(53)));
		assertThat("Model repeating instance count", model.getRepeatingBlockInstanceCount(),
				is(equalTo(2)));
	}

	@Test
	public void values() {
		StringCombinerAdvancedModelAccessor model = getTestModel();
		assertThat("Current", model.getDCCurrent(), is(equalTo(16.6f)));
		assertThat("Charge 0 not accumulated, an acc32 for model 404", model.getDCChargeDelivered(),
				is(nullValue()));
		assertThat("Voltage", model.getDCVoltage(), is(equalTo(602.0f)));
		assertThat("Temperature", model.getTemperature(), is(equalTo(-5.0f)));
		assertThat("Power", model.getDCPower(), is(equalTo(9960)));
		assertThat("Energy", model.getDCEnergy(), is(equalTo(987650L)));
		assertThat("Performance ratio 0x8000 not implemented, an int16 for model 404",
				model.getDCPerformanceRatio(), is(nullValue()));
	}

	@Test
	public void events() {
		StringCombinerAdvancedModelAccessor model = getTestModel();
		assertThat("Events", model.getEvents(),
				is(equalTo(Set.<ModelEvent> of(StringCombinerModelEvent.CombinerCabinetOpen))));
		assertThat("Vendor events", model.getVendorEvents(), is(equalTo(Set.of())));
	}

	@Test
	public void inputs() {
		List<AdvancedDcInput> inputs = getTestModel().getAdvancedDcInputs();
		assertThat("Inputs count", inputs, hasSize(2));

		// inputs use the input scale factors, and charge and energy are acc32 types
		assertAdvancedDcInput("Input 1", inputs.get(0), 1, 8.31f, 0xFFFFFFFFL, 602.1f, 4980, 493800L,
				0.97f, 14, Set.of(StringCombinerModelEvent.LowVoltage),
				Set.of(new GenericModelEvent(1)));
		assertAdvancedDcInput("Input 2", inputs.get(1), 2, null, null, null, -12, null, null, 14,
				Set.of(), Set.of());
	}

}

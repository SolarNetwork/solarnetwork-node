/* ==================================================================
 * StringCombinerModelAccessorImpl_403_01Tests.java - 5/10/2026 7:44:39 am
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

import static net.solarnetwork.node.hw.sunspec.combiner.test.StringCombinerTestUtils.assertDcInput;
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
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerModelAccessor;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerModelAccessor.DcInput;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerModelEvent;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link StringCombinerModelAccessorImpl} class, using
 * synthetic model 403 data.
 *
 * @author matt
 * @version 1.0
 */
public class StringCombinerModelAccessorImpl_403_01Tests {

	private StringCombinerModelAccessor getTestModel() {
		ModelData data = ModelDataUtils.getModelDataInstance(getClass(), "test-data-403-01.txt");
		return data.findTypedModel(StringCombinerModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), instanceOf(StringCombinerModelAccessorImpl.class));
	}

	@Test
	public void block() {
		StringCombinerModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(70)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(72)));
		assertThat("Model ID", model.getModelId(),
				is(equalTo(StringCombinerModelId.BasicStringCombiner2)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(16)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(8)));
		assertThat("Model length", model.getModelLength(), is(equalTo(32)));
		assertThat("Model repeating instance count", model.getRepeatingBlockInstanceCount(),
				is(equalTo(2)));
	}

	@Test
	public void values() {
		StringCombinerModelAccessor model = getTestModel();
		assertThat("Current", model.getDCCurrent(), is(equalTo(16.3f)));
		assertThat("Charge 0xFFFFFFFF is a value, an acc32 for model 403", model.getDCChargeDelivered(),
				is(equalTo(0xFFFFFFFFL)));
		assertThat("Voltage 0x8000 not implemented, an int16 for model 403", model.getDCVoltage(),
				is(nullValue()));
		assertThat("Temperature", model.getTemperature(), is(equalTo(28.0f)));
	}

	@Test
	public void events() {
		StringCombinerModelAccessor model = getTestModel();
		assertThat("Events", model.getEvents(),
				is(equalTo(Set.<ModelEvent> of(StringCombinerModelEvent.ReversedPolarity))));
		assertThat("Vendor events", model.getVendorEvents(), is(equalTo(Set.of())));
	}

	@Test
	public void inputs() {
		List<DcInput> inputs = getTestModel().getDcInputs();
		assertThat("Inputs count", inputs, hasSize(2));

		// inputs use the input scale factors, and charge is an acc32 type
		assertDcInput("Input 1", inputs.get(0), 1, 8.15f, 43210L,
				Set.of(StringCombinerModelEvent.FuseFault), Set.of(new GenericModelEvent(2)));
		assertDcInput("Input 2", inputs.get(1), 2, 8.12f, null, Set.of(), Set.of());
	}

}

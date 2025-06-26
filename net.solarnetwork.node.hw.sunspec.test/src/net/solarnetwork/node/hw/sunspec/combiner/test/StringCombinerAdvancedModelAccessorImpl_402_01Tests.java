/* ==================================================================
 * StringCombinerAdvancedModelAccessorImpl_402_01Tests.java - 10/09/2019 3:32:24 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerAdvancedModelAccessor;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerAdvancedModelAccessor.AdvancedDcInput;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerAdvancedModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerModelId;
import net.solarnetwork.node.hw.sunspec.inverter.IntegerInverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link StringCombinerModelAccessorImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class StringCombinerAdvancedModelAccessorImpl_402_01Tests {

	private static final Logger log = LoggerFactory
			.getLogger(StringCombinerAdvancedModelAccessorImpl_402_01Tests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-402-01.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void commonModelProperties() {
		CommonModelAccessor data = getTestDataInstance();
		assertThat("Manufacturer", data.getManufacturer(), equalTo("Solren"));
		assertThat("Model name", data.getModelName(), equalTo("PVI85"));
		assertThat("Options", data.getOptions(), equalTo("208VAC"));
		assertThat("Version", data.getVersion(), equalTo("C20130730"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("130602-14"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(1));
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		StringCombinerAdvancedModelAccessor accessor = data
				.findTypedModel(StringCombinerAdvancedModelAccessor.class);
		assertThat(accessor, instanceOf(StringCombinerAdvancedModelAccessorImpl.class));
	}

	@Test
	public void getTypedModel() {
		ModelData data = getTestDataInstance();
		InverterModelAccessor accessor = data.getTypedModel();
		assertThat(accessor, instanceOf(IntegerInverterModelAccessor.class));
	}

	@Test
	public void block() {
		StringCombinerAdvancedModelAccessor model = getTestDataInstance()
				.findTypedModel(StringCombinerAdvancedModelAccessor.class);
		assertThat("Model base address", model.getBaseAddress(), equalTo(122));
		assertThat("Model block address", model.getBlockAddress(), equalTo(124));
		assertThat("Model ID", model.getModelId(),
				equalTo(StringCombinerModelId.AdvancedStringCombiner));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(20));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(13));
		assertThat("Model length", model.getModelLength(), equalTo(124));
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(8));
	}

	@Test
	public void voltage() {
		StringCombinerAdvancedModelAccessor model = getTestDataInstance()
				.findTypedModel(StringCombinerAdvancedModelAccessor.class);
		assertThat("Voltage", model.getDCVoltage(), equalTo(338.5f));
	}

	@Test
	public void inputs() {
		StringCombinerAdvancedModelAccessor model = getTestDataInstance()
				.findTypedModel(StringCombinerAdvancedModelAccessor.class);
		List<AdvancedDcInput> inputs = model.getAdvancedDcInputs();
		assertThat("Inputs count", inputs, hasSize(8));
		// TODO: validate data
	}

}

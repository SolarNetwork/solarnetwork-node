/* ==================================================================
 * BomTemperatureModelAccessorImpl_303_01Tests.java - 5/07/2023 10:43:25 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.environmental.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.environmental.BomTemperatureModelAccessor;
import net.solarnetwork.node.hw.sunspec.environmental.BomTemperatureModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.environmental.EnvironmentalModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link BomTemperatureModelAccessorImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BomTemperatureModelAccessorImpl_303_01Tests {

	private static final Logger log = LoggerFactory
			.getLogger(BomTemperatureModelAccessorImpl_303_01Tests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-307-01.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		BomTemperatureModelAccessor accessor = data.findTypedModel(BomTemperatureModelAccessor.class);
		assertThat(accessor, instanceOf(BomTemperatureModelAccessorImpl.class));
	}

	@Test
	public void block() {
		BomTemperatureModelAccessor model = getTestDataInstance()
				.findTypedModel(BomTemperatureModelAccessor.class);
		assertThat("Model base address", model.getBaseAddress(), equalTo(90));
		assertThat("Model block address", model.getBlockAddress(), equalTo(92));
		assertThat("Model ID", model.getModelId(),
				equalTo(EnvironmentalModelId.BackOfModuleTemperature));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(0));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(1));
		assertThat("Model length", model.getModelLength(), equalTo(3));
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(3));
	}

	@Test
	public void data() {
		BomTemperatureModelAccessor model = getTestDataInstance()
				.findTypedModel(BomTemperatureModelAccessor.class);
		List<Float> temps = model.getBackOfModuleTemperatures();
		assertThat("3 temps returned", temps, hasSize(3));
		for ( int i = 0; i < 3; i++ ) {
			assertThat(String.format("Temp %d", i + 1), temps.get(i), equalTo(23.4f + (0.1f * i)));
		}
	}

}

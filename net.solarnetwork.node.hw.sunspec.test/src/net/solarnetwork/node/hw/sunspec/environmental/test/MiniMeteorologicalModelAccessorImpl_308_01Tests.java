/* ==================================================================
 * MiniMeteorologicalModelAccessorImpl_308_01Tests.java - 10/07/2023 7:26:23 am
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
import static org.hamcrest.Matchers.instanceOf;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.environmental.EnvironmentalModelId;
import net.solarnetwork.node.hw.sunspec.environmental.MiniMeteorologicalModelAccessor;
import net.solarnetwork.node.hw.sunspec.environmental.MiniMeteorologicalModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link MiniMeteorologicalModelAccessorImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MiniMeteorologicalModelAccessorImpl_308_01Tests {

	private static final Logger log = LoggerFactory
			.getLogger(IrradianceModelAccessorImpl_302_01Tests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-308-01.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		MiniMeteorologicalModelAccessor accessor = data
				.findTypedModel(MiniMeteorologicalModelAccessor.class);
		assertThat(accessor, instanceOf(MiniMeteorologicalModelAccessorImpl.class));
	}

	@Test
	public void block() {
		MiniMeteorologicalModelAccessor model = getTestDataInstance()
				.findTypedModel(MiniMeteorologicalModelAccessor.class);
		assertThat("Model base address", model.getBaseAddress(), equalTo(70));
		assertThat("Model block address", model.getBlockAddress(), equalTo(72));
		assertThat("Model ID", model.getModelId(), equalTo(EnvironmentalModelId.MiniMeteorolgical));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(4));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(0));
		assertThat("Model length", model.getModelLength(), equalTo(4));
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(0));
	}

	@Test
	public void data() {
		MiniMeteorologicalModelAccessor model = getTestDataInstance()
				.findTypedModel(MiniMeteorologicalModelAccessor.class);
		assertThat("GHI", model.getGlobalHorizontalIrradiance(), equalTo(12345));
		assertThat("BOM temp", model.getBackOfModuleTemperature(), equalTo(123.4f));
		assertThat("Ambient temp", model.getAmbientTemperature(), equalTo(-2.3f));
		assertThat("Wind speed", model.getWindSpeed(), equalTo(23));
	}

}

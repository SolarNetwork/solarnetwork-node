/* ==================================================================
 * IrradianceModelAccessorImpl_302_01Tests.java - 5/07/2023 8:38:52 am
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
import net.solarnetwork.node.hw.sunspec.environmental.IrradianceModelAccessor;
import net.solarnetwork.node.hw.sunspec.environmental.IrradianceModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link IrradianceModelAccessorImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class IrradianceModelAccessorImpl_302_01Tests {

	private static final Logger log = LoggerFactory
			.getLogger(IrradianceModelAccessorImpl_302_01Tests.class);

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
		IrradianceModelAccessor accessor = data.findTypedModel(IrradianceModelAccessor.class);
		assertThat(accessor, instanceOf(IrradianceModelAccessorImpl.class));
	}

	@Test
	public void block() {
		IrradianceModelAccessor model = getTestDataInstance()
				.findTypedModel(IrradianceModelAccessor.class);
		assertThat("Model base address", model.getBaseAddress(), equalTo(83));
		assertThat("Model block address", model.getBlockAddress(), equalTo(85));
		assertThat("Model ID", model.getModelId(), equalTo(EnvironmentalModelId.Irradiance));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(5));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(0));
		assertThat("Model length", model.getModelLength(), equalTo(5));
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(0));
	}

	@Test
	public void data() {
		IrradianceModelAccessor model = getTestDataInstance()
				.findTypedModel(IrradianceModelAccessor.class);
		assertThat("GHI", model.getGlobalHorizontalIrradiance(), equalTo(257));
		assertThat("POAI", model.getPlaneOfArrayIrradiance(), equalTo(258));
		assertThat("DFI", model.getDiffuseIrradiance(), equalTo(259));
		assertThat("DNI", model.getDirectNormalIrradiance(), equalTo(260));
		assertThat("OTI", model.getOtherIrradiance(), equalTo(261));
	}

}

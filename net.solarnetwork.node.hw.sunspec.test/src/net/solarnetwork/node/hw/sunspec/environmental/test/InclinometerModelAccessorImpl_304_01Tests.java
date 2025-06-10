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
import static org.hamcrest.Matchers.is;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.environmental.EnvironmentalModelId;
import net.solarnetwork.node.hw.sunspec.environmental.Incline;
import net.solarnetwork.node.hw.sunspec.environmental.InclinometerModelAccessor;
import net.solarnetwork.node.hw.sunspec.environmental.InclinometerModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link InclinometerModelAccessorImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class InclinometerModelAccessorImpl_304_01Tests {

	private static final Logger log = LoggerFactory
			.getLogger(InclinometerModelAccessorImpl_304_01Tests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-304-01.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		InclinometerModelAccessor accessor = data.findTypedModel(InclinometerModelAccessor.class);
		assertThat(accessor, instanceOf(InclinometerModelAccessorImpl.class));
	}

	@Test
	public void block() {
		InclinometerModelAccessor model = getTestDataInstance()
				.findTypedModel(InclinometerModelAccessor.class);
		assertThat("Model base address", model.getBaseAddress(), equalTo(70));
		assertThat("Model block address", model.getBlockAddress(), equalTo(72));
		assertThat("Model ID", model.getModelId(), equalTo(EnvironmentalModelId.Inclinometer));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(0));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(6));
		assertThat("Model length", model.getModelLength(), equalTo(12));
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(2));
	}

	@Test
	public void data() {
		InclinometerModelAccessor model = getTestDataInstance()
				.findTypedModel(InclinometerModelAccessor.class);
		List<Incline> inclines = model.getInclines();
		assertThat("2 inclines returned", inclines, hasSize(2));
		Incline inc = inclines.get(0);
		assertThat("Incline 1 x", inc.getInclineX(), is(equalTo(245.82f)));
		assertThat("Incline 1 y", inc.getInclineY(), is(equalTo(10.82f)));
		assertThat("Incline 1 z", inc.getInclineZ(), is(equalTo(735.98f)));
		inc = inclines.get(1);
		assertThat("Incline 2 x", inc.getInclineX(), is(equalTo(12.0f)));
		assertThat("Incline 2 y", inc.getInclineY(), is(equalTo(11.82f)));
		assertThat("Incline 2 z", inc.getInclineZ(), is(equalTo(3.02f)));
	}

}

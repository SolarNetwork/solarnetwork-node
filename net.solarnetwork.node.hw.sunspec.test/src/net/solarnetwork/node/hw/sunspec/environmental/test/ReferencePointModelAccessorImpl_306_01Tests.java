/* ==================================================================
 * ReferencePointModelAccessorImpl_306_01Tests.java - 9/07/2023 4:53:29 pm
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
import net.solarnetwork.node.hw.sunspec.environmental.ReferencePoint;
import net.solarnetwork.node.hw.sunspec.environmental.ReferencePointModelAccessor;
import net.solarnetwork.node.hw.sunspec.environmental.ReferencePointModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link ReferencePointModelAccessorImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ReferencePointModelAccessorImpl_306_01Tests {

	private static final Logger log = LoggerFactory
			.getLogger(InclinometerModelAccessorImpl_304_01Tests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-306-01.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		ReferencePointModelAccessor accessor = data.findTypedModel(ReferencePointModelAccessor.class);
		assertThat(accessor, instanceOf(ReferencePointModelAccessorImpl.class));
	}

	@Test
	public void block() {
		ReferencePointModelAccessor model = getTestDataInstance()
				.findTypedModel(ReferencePointModelAccessor.class);
		assertThat("Model base address", model.getBaseAddress(), equalTo(70));
		assertThat("Model block address", model.getBlockAddress(), equalTo(72));
		assertThat("Model ID", model.getModelId(), equalTo(EnvironmentalModelId.ReferencePoint));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(0));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(7));
		assertThat("Model length", model.getModelLength(), equalTo(14));
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(2));
	}

	@Test
	public void data() {
		ReferencePointModelAccessor model = getTestDataInstance()
				.findTypedModel(ReferencePointModelAccessor.class);
		List<ReferencePoint> points = model.getReferencePoints();
		assertThat("2 reference points returned", points, hasSize(2));
		ReferencePoint p = points.get(0);
		assertThat("ReferencePoint 1 irradiance", p.getIrradiance(), is(equalTo(12345)));
		assertThat("ReferencePoint 1 current", p.getCurrent(), is(equalTo(1.23f)));
		assertThat("ReferencePoint 1 voltage", p.getVoltage(), is(equalTo(2.34f)));
		assertThat("ReferencePoint 1 temperature", p.getTemperature(), is(equalTo(34.5f)));
		p = points.get(1);
		assertThat("ReferencePoint 2 irradiance", p.getIrradiance(), is(equalTo(23456)));
		assertThat("ReferencePoint 2 current", p.getCurrent(), is(equalTo(-2.34f)));
		assertThat("ReferencePoint 2 voltage", p.getVoltage(), is(equalTo(-3.45f)));
		assertThat("ReferencePoint 2 temperature", p.getTemperature(), is(equalTo(-45.6f)));
	}

}

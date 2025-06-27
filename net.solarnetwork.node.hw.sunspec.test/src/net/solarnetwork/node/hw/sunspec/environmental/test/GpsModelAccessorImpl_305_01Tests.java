/* ==================================================================
 * GpsModelAccessorImpl_305_01Tests.java - 9/07/2023 7:27:06 am
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
import static org.hamcrest.Matchers.is;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.environmental.EnvironmentalModelId;
import net.solarnetwork.node.hw.sunspec.environmental.GpsModelAccessor;
import net.solarnetwork.node.hw.sunspec.environmental.GpsModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for {@link GpsModelAccessorImpl}.
 * 
 * @author matt
 * @version 1.0
 */
public class GpsModelAccessorImpl_305_01Tests {

	private static final Logger log = LoggerFactory
			.getLogger(IrradianceModelAccessorImpl_302_01Tests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-305-01.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		GpsModelAccessor accessor = data.findTypedModel(GpsModelAccessor.class);
		assertThat(accessor, instanceOf(GpsModelAccessorImpl.class));
	}

	@Test
	public void block() {
		GpsModelAccessor model = getTestDataInstance().findTypedModel(GpsModelAccessor.class);
		assertThat("Model base address", model.getBaseAddress(), equalTo(70));
		assertThat("Model block address", model.getBlockAddress(), equalTo(72));
		assertThat("Model ID", model.getModelId(), equalTo(EnvironmentalModelId.GPS));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(36));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(0));
		assertThat("Model length", model.getModelLength(), equalTo(36));
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(0));
	}

	@Test
	public void data() {
		GpsModelAccessor model = getTestDataInstance().findTypedModel(GpsModelAccessor.class);
		assertThat("GPS timestamp", model.getGpsTimestamp(), is(equalTo(
				DateTimeFormatter.ISO_INSTANT.parse("2023-07-09T19:28:34.123Z", Instant::from))));
		assertThat("Location", model.getLocationName(), is(equalTo("Home sweet home")));
		assertThat("Latitude", model.getLatitude(), is(equalTo(new BigDecimal("-37.1133611"))));
		assertThat("Latitude", model.getLongitude(), is(equalTo(new BigDecimal("175.8884328"))));
	}

}

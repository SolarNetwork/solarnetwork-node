/* ==================================================================
 * IntegerInverterModelAccessor_103_04Tests.java - 8/10/2018 7:06:15 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.inverter.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.GenericModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.inverter.IntegerInverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelId;
import net.solarnetwork.node.hw.sunspec.meter.test.IntegerMeterModelAccessorTests;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link IntegerInverterModelAccessor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class IntegerInverterModelAccessor_103_05Tests {

	private static final Logger log = LoggerFactory.getLogger(IntegerMeterModelAccessorTests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-103-05.txt", true);
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void commonModelProperties() {
		CommonModelAccessor data = getTestDataInstance();
		assertThat("Manufacturer", data.getManufacturer(), equalTo("SMA"));
		assertThat("Model name", data.getModelName(), equalTo("Solar Inverter"));
		assertThat("Options", data.getOptions(), equalTo("9338"));
		assertThat("Version", data.getVersion(), equalTo("3.11.02.R"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("3009060251"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(65535));
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		InverterModelAccessor meterAccessor = data.findTypedModel(InverterModelAccessor.class);
		assertThat(meterAccessor, instanceOf(IntegerInverterModelAccessor.class));
	}

	@Test
	public void getTypedModel() {
		ModelData data = getTestDataInstance();
		ModelAccessor accessor = data.getTypedModel();
		assertThat(accessor, instanceOf(GenericModelAccessor.class));
		assertThat(accessor.getModelId().getId(), equalTo(11));
	}

	@Test
	public void block() {
		InverterModelAccessor model = getTestDataInstance().findTypedModel(InverterModelAccessor.class);
		assertThat("Model base address", model.getBaseAddress(), equalTo(40185));
		assertThat("Model block address", model.getBlockAddress(), equalTo(40187));
		assertThat("Model ID", model.getModelId(), equalTo(InverterModelId.ThreePhaseInverterInteger));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(50));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(0));
		assertThat("Model length", model.getModelLength(), equalTo(50));
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(0));
	}

}

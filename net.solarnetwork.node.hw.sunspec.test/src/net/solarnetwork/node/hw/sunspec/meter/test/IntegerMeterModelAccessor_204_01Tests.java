/* ==================================================================
 * IntegerMeterModelAccessor_204_01Tests.java - 28/02/2019 4:12:24 pm
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

package net.solarnetwork.node.hw.sunspec.meter.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import java.util.Set;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.meter.IntegerMeterModelAccessor;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelAccessor;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link IntegerMeterModelAccessor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class IntegerMeterModelAccessor_204_01Tests {

	private static final Logger log = LoggerFactory
			.getLogger(IntegerMeterModelAccessor_203_01Tests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-204-01.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void commonModelProperties() {
		CommonModelAccessor data = getTestDataInstance();
		assertThat("Manufacturer", data.getManufacturer(), equalTo("ACCUENERGY"));
		assertThat("Model name", data.getModelName(), equalTo("Acuvim II"));
		assertThat("Options", data.getOptions(), equalTo("Acuvim II"));
		assertThat("Version", data.getVersion(), equalTo("H:2.32 S:3.66"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("AH17122035"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(100));
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		MeterModelAccessor meterAccessor = data.findTypedModel(MeterModelAccessor.class);
		assertThat(meterAccessor, instanceOf(IntegerMeterModelAccessor.class));
	}

	@Test
	public void block() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		assertThat("Model base address", model.getBaseAddress(), equalTo(69));
		assertThat("Model block address", model.getBlockAddress(), equalTo(71));
		assertThat("Model ID", model.getModelId(),
				equalTo(MeterModelId.DeltaConnectThreePhaseMeterInteger));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(105));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(0));
		assertThat("Model length", model.getModelLength(), equalTo(81)); // SHOULD BE 105!
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(0));
	}

	@Test
	public void events() {
		MeterModelAccessor model = getTestDataInstance().getTypedModel();
		Set<ModelEvent> events = model.getEvents();
		assertThat(events, hasSize(0));
	}

}

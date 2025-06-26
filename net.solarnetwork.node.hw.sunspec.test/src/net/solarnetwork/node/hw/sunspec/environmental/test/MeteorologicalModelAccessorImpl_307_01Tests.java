/* ==================================================================
 * MeteorologicalModelAccessorImpl_307_01Tests.java - 10/07/2023 9:40:35 am
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
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.environmental.EnvironmentalModelId;
import net.solarnetwork.node.hw.sunspec.environmental.MeteorologicalModelAccessor;
import net.solarnetwork.node.hw.sunspec.environmental.MeteorologicalModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.environmental.PrecipitationType;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link MeteorologicalModelAccessorImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MeteorologicalModelAccessorImpl_307_01Tests {

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
	public void commonModelProperties() {
		CommonModelAccessor data = getTestDataInstance();
		assertThat("Manufacturer", data.getManufacturer(), equalTo("Rainwise_Inc"));
		assertThat("Model name", data.getModelName(), equalTo("PVmet 500"));
		assertThat("Options", data.getOptions(), equalTo("0"));
		assertThat("Version", data.getVersion(), equalTo("1.2"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("123456"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(60));
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		MeteorologicalModelAccessor accessor = data.findTypedModel(MeteorologicalModelAccessor.class);
		assertThat(accessor, instanceOf(MeteorologicalModelAccessorImpl.class));
	}

	@Test
	public void block() {
		MeteorologicalModelAccessor model = getTestDataInstance()
				.findTypedModel(MeteorologicalModelAccessor.class);
		assertThat("Model base address", model.getBaseAddress(), equalTo(70));
		assertThat("Model block address", model.getBlockAddress(), equalTo(72));
		assertThat("Model ID", model.getModelId(), equalTo(EnvironmentalModelId.BaseMeteorolgical));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(11));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(0));
		assertThat("Model length", model.getModelLength(), equalTo(11));
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(0));
	}

	@Test
	public void data() {
		MeteorologicalModelAccessor model = getTestDataInstance()
				.findTypedModel(MeteorologicalModelAccessor.class);
		assertThat("Ambient temperature", model.getAmbientTemperature(), equalTo(328.5f));
		assertThat("Relative humidity", model.getRelativeHumidity(), equalTo(1097));
		assertThat("Atmospheric pressure", model.getAtmosphericPressure(), equalTo(110600));
		assertThat("Wind speed", model.getWindSpeed(), equalTo(1082));
		assertThat("Wind direciton", model.getWindDirection(), equalTo(-1));
		assertThat("Rain", model.getRainAccumulation(), equalTo(2070));
		assertThat("Snow", model.getSnowAccumulation(), equalTo(2090));
		assertThat("Precipitation type", model.getPrecipitationType(),
				equalTo(PrecipitationType.PatchyFog));
		assertThat("Electric field", model.getElectricField(), equalTo(1172));
		assertThat("Surface wetness", model.getSurfaceWetness(), equalTo(1182000));
		assertThat("Soil moisture", model.getSoilMoisture(), equalTo(1176));
	}

}

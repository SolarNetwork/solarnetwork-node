/* ==================================================================
 * InverterMpptExtensionModelAccessor_113_01Tests.java - 12/10/2019 7:23:08 am
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

package net.solarnetwork.node.hw.sunspec.inverter.test;

import static net.solarnetwork.node.hw.sunspec.inverter.InverterOperatingState.Mppt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.OperatingState;
import net.solarnetwork.node.hw.sunspec.inverter.InverterMpptExtensionModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterMpptExtensionModelAccessor.DcModule;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for {@link InverterMpptExtensionModelAccessor}.
 * 
 * @author matt
 * @version 1.0
 */
public class InverterMpptExtensionModelAccessor_113_01Tests {

	private static final Logger log = LoggerFactory
			.getLogger(InverterMpptExtensionModelAccessor_113_01Tests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-113-01.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void modelAccessor() {
		InverterMpptExtensionModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterMpptExtensionModelAccessor.class);
		assertThat("InverterMpptExtensionModelAccessor available", model, notNullValue());
	}

	@Test
	public void timestampPeriod() {
		InverterMpptExtensionModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterMpptExtensionModelAccessor.class);
		assertThat("Timestamp period", model.getTimestampPeriod(), nullValue());
	}

	@Test
	public void events() {
		InverterMpptExtensionModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterMpptExtensionModelAccessor.class);
		assertThat("Events", model.getEvents(), hasSize(0));
	}

	private void assertDcModule(String suffix, DcModule module, Integer id, String name, Long timestamp,
			Float current, Long energyDelivered, Integer power, Float voltage, Float temperature,
			OperatingState state, Set<ModelEvent> events) {
		assertThat("Module ID " + suffix, module.getInputId(), equalTo(id));
		assertThat("Module input name " + suffix, module.getInputName(), equalTo(name));
		assertThat("Module data timestamp " + suffix, module.getDataTimestamp(), equalTo(timestamp));
		assertThat("Module current " + suffix, module.getDCCurrent(), equalTo(current));
		assertThat("Module energy delivered " + suffix, module.getDCEnergyDelivered(),
				equalTo(energyDelivered));
		assertThat("Module power " + suffix, module.getDCPower(), equalTo(power));
		assertThat("Module voltage " + suffix, module.getDCVoltage(), equalTo(voltage));
		assertThat("Module temperature " + suffix, module.getTemperature(), equalTo(temperature));
		assertThat("Module operating state " + suffix, module.getOperatingState(), equalTo(state));
		assertThat("Module events " + suffix, module.getEvents(), equalTo(events));
	}

	@Test
	public void dcModules() {
		InverterMpptExtensionModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterMpptExtensionModelAccessor.class);
		List<DcModule> modules = model.getDcModules();
		assertThat("DC modules", modules, hasSize(2));

		Set<ModelEvent> noEvents = Collections.emptySet();
		assertDcModule("1", modules.get(0), 1, "String 1", 623608619L, 0.15f, 11937020L, 65, 439.7f,
				null, Mppt, noEvents);
		assertDcModule("2", modules.get(1), 2, "Not supported", 4294967295L, null, null, null, null,
				null, null, noEvents);
	}
}

/* ==================================================================
 * StringCombinerTestUtils.java - 5/10/2026 7:44:39 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.combiner.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.util.Set;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerAdvancedModelAccessor.AdvancedDcInput;

/**
 * Helper methods for string combiner tests.
 *
 * @author matt
 * @version 1.0
 */
public final class StringCombinerTestUtils {

	private StringCombinerTestUtils() {
		// not available
	}

	/**
	 * Assert the properties of an advanced DC input.
	 *
	 * @param prefix
	 *        the assertion message prefix
	 * @param input
	 *        the input to verify
	 * @param id
	 *        the expected input ID
	 * @param current
	 *        the expected current
	 * @param charge
	 *        the expected charge delivered
	 * @param voltage
	 *        the expected voltage
	 * @param power
	 *        the expected power
	 * @param energy
	 *        the expected energy
	 * @param performanceRatio
	 *        the expected performance ratio
	 * @param moduleCount
	 *        the expected module count
	 * @param events
	 *        the expected events
	 * @param vendorEvents
	 *        the expected vendor events
	 */
	public static void assertAdvancedDcInput(String prefix, AdvancedDcInput input, Integer id,
			Float current, Long charge, Float voltage, Integer power, Long energy,
			Float performanceRatio, Integer moduleCount, Set<ModelEvent> events,
			Set<ModelEvent> vendorEvents) {
		assertThat(prefix + " ID", input.getInputId(), is(equalTo(id)));
		assertThat(prefix + " current", input.getDCCurrent(), is(equalTo(current)));
		assertThat(prefix + " charge", input.getDCChargeDelivered(), is(equalTo(charge)));
		assertThat(prefix + " voltage", input.getDCVoltage(), is(equalTo(voltage)));
		assertThat(prefix + " power", input.getDCPower(), is(equalTo(power)));
		assertThat(prefix + " energy", input.getDCEnergy(), is(equalTo(energy)));
		assertThat(prefix + " performance ratio", input.getDCPerformanceRatio(),
				is(equalTo(performanceRatio)));
		assertThat(prefix + " module count", input.getModuleCount(), is(equalTo(moduleCount)));
		assertThat(prefix + " events", input.getEvents(), is(equalTo(events)));
		assertThat(prefix + " vendor events", input.getVendorEvents(), is(equalTo(vendorEvents)));
	}

}

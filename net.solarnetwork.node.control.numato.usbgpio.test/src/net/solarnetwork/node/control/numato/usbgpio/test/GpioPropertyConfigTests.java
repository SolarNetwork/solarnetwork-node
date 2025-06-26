/* ==================================================================
 * GpioPropertyConfigTests.java - 25/09/2021 3:17:44 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.numato.usbgpio.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.util.BitSet;
import org.junit.Test;
import net.solarnetwork.node.control.numato.usbgpio.GpioDirection;
import net.solarnetwork.node.control.numato.usbgpio.GpioPropertyConfig;

/**
 * Test cases for the {@link GpioPropertyConfig} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GpioPropertyConfigTests {

	@Test
	public void ioDirectionBitSet_null() {
		// WHEN
		BitSet result = GpioPropertyConfig.ioDirectionBitSet(null);

		// THEN
		assertThat("null returned from null input", result, is(nullValue()));
	}

	@Test
	public void ioDirectionBitSet_empty() {
		// WHEN
		BitSet result = GpioPropertyConfig.ioDirectionBitSet(new GpioPropertyConfig[0]);

		// THEN
		assertThat("null returned from empty input", result, is(nullValue()));
	}

	@Test
	public void ioDirectionBitSet_single_output() {
		// GIVEN
		GpioPropertyConfig cfg = GpioPropertyConfig.of("test", 1);
		cfg.setGpioDirection(GpioDirection.Output);

		// WHEN
		BitSet result = GpioPropertyConfig.ioDirectionBitSet(new GpioPropertyConfig[] { cfg });

		// THEN
		assertThat("BitSet returned from valid input", result, is(notNullValue()));
		assertThat("BitSet has no inputs", result.cardinality(), is(0));
	}

}

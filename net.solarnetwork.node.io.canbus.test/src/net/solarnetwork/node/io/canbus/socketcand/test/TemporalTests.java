/* ==================================================================
 * TemporalTests.java - 20/09/2019 1:20:05 pm
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

package net.solarnetwork.node.io.canbus.socketcand.test;

import static org.hamcrest.MatcherAssert.assertThat;
import java.math.BigDecimal;
import org.hamcrest.Matchers;
import org.junit.Test;
import net.solarnetwork.node.io.canbus.Temporal;

/**
 * Test cases for the {@link Temporal} class.
 * 
 * @author matt
 * @version 1.0
 */
public class TemporalTests {

	private static class TestTemporal implements Temporal {

		private final int seconds;
		private final int microseconds;

		private TestTemporal(int seconds, int microseconds) {
			super();
			this.seconds = seconds;
			this.microseconds = microseconds;
		}

		@Override
		public int getSeconds() {
			return seconds;
		}

		@Override
		public int getMicroseconds() {
			return microseconds;
		}

	}

	@Test
	public void fractionalSeconds_basic() {
		assertThat("Fractional seconds", new TestTemporal(1, 500000).getFractionalSeconds(),
				Matchers.equalTo(new BigDecimal("1.500000")));
	}

	@Test
	public void fractionalSeconds_small() {
		assertThat("Fractional seconds has leading microsecond zeros",
				new TestTemporal(1, 50).getFractionalSeconds(),
				Matchers.equalTo(new BigDecimal("1.000050")));
	}

	@Test
	public void fractionalSeconds_spill() {
		assertThat("Fractional seconds has excess microsecond spill over",
				new TestTemporal(1, 1500000).getFractionalSeconds(),
				Matchers.equalTo(new BigDecimal("2.500000")));
	}
}

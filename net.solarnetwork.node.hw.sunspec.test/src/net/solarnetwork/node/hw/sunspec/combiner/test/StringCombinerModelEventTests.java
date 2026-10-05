/* ==================================================================
 * StringCombinerModelEventTests.java - 6/10/2026 12:18:40 pm
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
import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.combiner.StringCombinerModelEvent;

/**
 * Test cases for the {@link StringCombinerModelEvent} class.
 *
 * @author matt
 * @version 1.0
 */
public class StringCombinerModelEventTests {

	@Test
	public void forBitmask() {
		assertThat("Events from bits", StringCombinerModelEvent.forBitmask(0x40001L), is(equalTo(
				EnumSet.of(StringCombinerModelEvent.LowVoltage, StringCombinerModelEvent.ArcDetected))));
	}

	@Test
	public void forBitmask_highestEvent() {
		assertThat("Highest defined event", StringCombinerModelEvent.forBitmask(0x40000L),
				is(equalTo(EnumSet.of(StringCombinerModelEvent.ArcDetected))));
	}

	@Test
	public void forBitmask_zero() {
		assertThat("No events", StringCombinerModelEvent.forBitmask(0L), is(equalTo(Set.of())));
	}

	@Test
	public void forBitmask_notImplemented() {
		assertThat("Not implemented", StringCombinerModelEvent.forBitmask(0xFFFFFFFFL),
				is(equalTo(Set.of())));
	}

	@Test
	public void forBitmask_mostSignificantBit() {
		assertThat("Most significant bit set means not implemented",
				StringCombinerModelEvent.forBitmask(0x80000000L | 0x40001L), is(equalTo(Set.of())));
	}

}

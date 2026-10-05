/* ==================================================================
 * InverterMpptExtensionModelEventTests.java - 6/10/2026 12:17:22 pm
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

package net.solarnetwork.node.hw.sunspec.inverter.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.inverter.InverterMpptExtensionModelEvent;

/**
 * Test cases for the {@link InverterMpptExtensionModelEvent} class.
 *
 * @author matt
 * @version 1.0
 */
public class InverterMpptExtensionModelEventTests {

	@Test
	public void forBitmask() {
		assertThat("Events from bits", InverterMpptExtensionModelEvent.forBitmask(0x8001L),
				is(equalTo(EnumSet.of(InverterMpptExtensionModelEvent.GroundFault,
						InverterMpptExtensionModelEvent.ArcDetection))));
	}

	@Test
	public void forBitmask_highestEvent() {
		assertThat("Highest defined event", InverterMpptExtensionModelEvent.forBitmask(0x400000L),
				is(equalTo(EnumSet.of(InverterMpptExtensionModelEvent.DcOverCurrent))));
	}

	@Test
	public void forBitmask_undefinedBits() {
		assertThat("Undefined bits ignored", InverterMpptExtensionModelEvent.forBitmask(0x5L),
				is(equalTo(EnumSet.of(InverterMpptExtensionModelEvent.GroundFault))));
	}

	@Test
	public void forBitmask_zero() {
		assertThat("No events", InverterMpptExtensionModelEvent.forBitmask(0L), is(equalTo(Set.of())));
	}

	@Test
	public void forBitmask_notImplemented() {
		assertThat("Not implemented", InverterMpptExtensionModelEvent.forBitmask(0xFFFFFFFFL),
				is(equalTo(Set.of())));
	}

	@Test
	public void forBitmask_mostSignificantBit() {
		assertThat("Most significant bit set means not implemented",
				InverterMpptExtensionModelEvent.forBitmask(0x80000000L | 0x8001L),
				is(equalTo(Set.of())));
	}

}

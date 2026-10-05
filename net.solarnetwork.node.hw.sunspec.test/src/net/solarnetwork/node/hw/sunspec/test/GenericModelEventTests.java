/* ==================================================================
 * GenericModelEventTests.java - 5/10/2026 10:31:20 pm
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

package net.solarnetwork.node.hw.sunspec.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.GenericModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelEvent;

/**
 * Test cases for the {@link GenericModelEvent} class.
 *
 * @author matt
 * @version 1.0
 */
public class GenericModelEventTests {

	@Test
	public void forBitmask() {
		assertThat("Events for the set bits", GenericModelEvent.forBitmask(0x40000005L),
				is(equalTo(Set.<ModelEvent> of(new GenericModelEvent(0), new GenericModelEvent(2),
						new GenericModelEvent(30)))));
	}

	@Test
	public void forBitmask_none() {
		assertThat("No events", GenericModelEvent.forBitmask(0L), is(equalTo(Set.of())));
	}

	@Test
	public void forBitmask_notImplemented() {
		assertThat("Not implemented value has no events", GenericModelEvent.forBitmask(0xFFFFFFFFL),
				is(equalTo(Set.of())));
	}

	@Test
	public void forBitmask_mostSignificantBit() {
		assertThat("Bitmask with the most significant bit set is not implemented",
				GenericModelEvent.forBitmask(0x80000001L), is(equalTo(Set.of())));
	}

}

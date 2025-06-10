/* ==================================================================
 * DurationRangeTests.java - 9/08/2019 2:57:42 pm
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

package net.solarnetwork.node.control.esi.domain.test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import java.time.Duration;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.node.control.esi.domain.DurationRange;

/**
 * Test cases for the {@link DurationRange} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DurationRangeTests {

	@Test
	public void asMap() {
		// given
		Duration min = Duration.ofSeconds(123L, MILLISECONDS.toNanos(456L));
		Duration max = Duration.ofSeconds(789L, MILLISECONDS.toNanos(123L));
		DurationRange r = new DurationRange(min, max);

		// when
		Map<String, Object> m = r.asMap();

		// then
		assertThat("Map size", m.keySet(), hasSize(2));
		assertThat("Map value", m,
				allOf(hasEntry("minMillis", min.toMillis()), hasEntry("maxMillis", max.toMillis())));
	}

	@Test
	public void minMillisAccessorGet() {
		// given
		Duration d = Duration.ofSeconds(123L, MILLISECONDS.toNanos(456L));
		DurationRange r = new DurationRange(d, null);

		// when
		long result = r.getMinMillis();

		// then
		assertThat("Min millis", result, equalTo(d.toMillis()));
	}

	@Test
	public void minMillisAccessorSet() {
		// given
		DurationRange pc = new DurationRange();

		// when
		pc.setMinMillis(123456L);

		// then
		assertThat("Min", pc.getMin(), equalTo(Duration.ofSeconds(123L, MILLISECONDS.toNanos(456L))));
	}

	@Test
	public void maxMillisAccessorGet() {
		// given
		Duration d = Duration.ofSeconds(123L, MILLISECONDS.toNanos(456L));
		DurationRange r = new DurationRange(null, d);

		// when
		long result = r.getMaxMillis();

		// then
		assertThat("Max millis", result, equalTo(d.toMillis()));
	}

	@Test
	public void maxMillisAccessorSet() {
		// given
		DurationRange pc = new DurationRange();

		// when
		pc.setMaxMillis(123456L);

		// then
		assertThat("Max", pc.getMax(), equalTo(Duration.ofSeconds(123L, MILLISECONDS.toNanos(456L))));
	}

}

/* ==================================================================
 * MetricKeyTests.java - 16/02/2026 5:39:38 pm
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

package net.solarnetwork.node.metrics.domain.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import org.junit.Test;
import net.solarnetwork.node.metrics.domain.MetricKey;

/**
 * Test cases for the {@link MetricKey} class.
 *
 * @author matt
 * @version 1.0
 */
public class MetricKeyTests {

	@Test
	public void compare_eq() {
		// GIVEN
		final MetricKey l = new MetricKey(Instant.now(), "a", "a");
		final MetricKey r = new MetricKey(l.getTimestamp(), l.getType(), l.getName());

		// THEN
		then(l.compareTo(r)).isEqualTo(0);
	}

	@Test
	public void compare_ts_lt() {
		// GIVEN
		final MetricKey l = new MetricKey(Instant.now(), "a", "a");
		final MetricKey r = new MetricKey(l.getTimestamp().plusSeconds(1), l.getType(), l.getName());

		// THEN
		then(l.compareTo(r)).isLessThan(0);
	}

	@Test
	public void compare_ts_gt() {
		// GIVEN
		final MetricKey l = new MetricKey(Instant.now(), "a", "a");
		final MetricKey r = new MetricKey(l.getTimestamp().minusSeconds(1), l.getType(), l.getName());

		// THEN
		then(l.compareTo(r)).isGreaterThan(0);
	}

	@Test
	public void compare_type_lt() {
		// GIVEN
		final MetricKey l = new MetricKey(Instant.now(), "b", "a");
		final MetricKey r = new MetricKey(l.getTimestamp(), "c", l.getName());

		// THEN
		then(l.compareTo(r)).isLessThan(0);
	}

	@Test
	public void compare_type_gt() {
		// GIVEN
		final MetricKey l = new MetricKey(Instant.now(), "b", "a");
		final MetricKey r = new MetricKey(l.getTimestamp(), "a", l.getName());

		// THEN
		then(l.compareTo(r)).isGreaterThan(0);
	}

	@Test
	public void compare_name_lt() {
		// GIVEN
		final MetricKey l = new MetricKey(Instant.now(), "a", "b");
		final MetricKey r = new MetricKey(l.getTimestamp(), l.getType(), "c");

		// THEN
		then(l.compareTo(r)).isLessThan(0);
	}

	@Test
	public void compare_name_gt() {
		// GIVEN
		final MetricKey l = new MetricKey(Instant.now(), "a", "b");
		final MetricKey r = new MetricKey(l.getTimestamp(), l.getType(), "a");

		// THEN
		then(l.compareTo(r)).isGreaterThan(0);
	}

}

/* ==================================================================
 * ParameterizedMetricAggregateTests.java - 15/07/2024 8:20:00 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.util.function.Function;
import org.junit.Test;
import net.solarnetwork.node.metrics.domain.MetricAggregate;
import net.solarnetwork.node.metrics.domain.ParameterizedMetricAggregate;

/**
 * Test cases for the {@link ParameterizedMetricAggregate} class.
 *
 * @author matt
 * @version 1.0
 */
public class ParameterizedMetricAggregateTests {

	@Test
	public void keyProvider() {
		// GIVEN
		Function<MetricAggregate, String> fn = (agg) -> {
			return agg.getClass().getSimpleName();
		};
		ParameterizedMetricAggregate agg = new ParameterizedMetricAggregate("t", new Object[] { 1 }, fn);

		// WHEN
		String key = agg.key();

		// THEN
		assertThat("Key generated via provider", key, is(equalTo(fn.apply(agg))));
	}

	@Test
	public void quantileKey_25() {
		// WHEN
		String key = ParameterizedMetricAggregate.METRIC_TYPE_QUANTILE_25.key();

		// THEN
		assertThat("Key generated", key, is(equalTo("q:25")));
	}

	@Test
	public void quantileKey_75() {
		// WHEN
		String key = ParameterizedMetricAggregate.METRIC_TYPE_QUANTILE_75.key();

		// THEN
		assertThat("Key generated", key, is(equalTo("q:75")));
	}

}

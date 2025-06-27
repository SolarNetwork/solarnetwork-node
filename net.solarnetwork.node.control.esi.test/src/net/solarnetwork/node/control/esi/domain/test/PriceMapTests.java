/* ==================================================================
 * PriceMapTests.java - 10/08/2019 11:02:37 am
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Currency;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.node.control.esi.domain.DurationRange;
import net.solarnetwork.node.control.esi.domain.PowerComponents;
import net.solarnetwork.node.control.esi.domain.PriceComponents;
import net.solarnetwork.node.control.esi.domain.PriceMap;

/**
 * Test cases for the {@link PriceMap} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PriceMapTests {

	private static final Currency USD = Currency.getInstance("USD");

	@SuppressWarnings("unchecked")
	@Test
	public void asMap() {
		// given
		PriceMap pm = new PriceMap(new PowerComponents(1L, 2L), Duration.ofMillis(3000L),
				DurationRange.ofSeconds(123L, 789L), new PriceComponents(USD, new BigDecimal("4.56")));

		// when
		Map<String, Object> m = pm.asMap();

		// then
		assertThat("Map keys", m.keySet(), containsInAnyOrder("powerComponents", "durationMillis",
				"responseTime", "priceComponents"));

		Map<String, Object> subMap = (Map<String, Object>) m.get("powerComponents");
		assertThat("Power components size", subMap.keySet(), hasSize(2));
		assertThat("Power components", subMap,
				allOf(hasEntry("realPower", pm.powerComponents().getRealPower()),
						hasEntry("reactivePower", pm.powerComponents().getReactivePower())));

		assertThat("Duration millis", m, hasEntry("durationMillis", pm.getDuration().toMillis()));

		subMap = (Map<String, Object>) m.get("responseTime");
		assertThat("Response time size", subMap.keySet(), hasSize(2));
		assertThat("Response time", subMap,
				allOf(hasEntry("minMillis", pm.responseTime().min().toMillis()),
						hasEntry("maxMillis", pm.responseTime().max().toMillis())));

		subMap = (Map<String, Object>) m.get("priceComponents");
		assertThat("Price components size", subMap.keySet(), hasSize(2));
		assertThat("Price components", subMap,
				allOf(hasEntry("currencyCode", USD.getCurrencyCode()),
						hasEntry("apparentEnergyPriceValue",
								pm.priceComponents().apparentEnergyPrice().toPlainString())));
	}

	@Test
	public void durationMillisAccessorGet() {
		// given
		Duration d = Duration.ofSeconds(123L, MILLISECONDS.toNanos(456L));
		PriceMap pm = new PriceMap(null, d, null, null);

		// when
		long result = pm.getDurationMillis();

		// then
		assertThat("Duration millis", result, equalTo(d.toMillis()));
	}

	@Test
	public void durationMillisAccessorSet() {
		// given
		PriceMap pm = new PriceMap();

		// when
		pm.setDurationMillis(123456L);

		// then
		assertThat("Min", pm.getDuration(),
				equalTo(Duration.ofSeconds(123L, MILLISECONDS.toNanos(456L))));
	}

}

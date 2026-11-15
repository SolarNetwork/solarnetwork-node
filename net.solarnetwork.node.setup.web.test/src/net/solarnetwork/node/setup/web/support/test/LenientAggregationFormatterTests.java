/* ==================================================================
 * LenientAggregationFormatterTests.java - 13 Nov 2026 4:18:02 pm
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

package net.solarnetwork.node.setup.web.support.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.util.Locale;
import org.junit.Test;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.node.setup.web.support.LenientAggregationFormatter;

/**
 * Test cases for the {@link LenientAggregationFormatter} class.
 *
 * @author matt
 * @version 1.0
 */
public class LenientAggregationFormatterTests {

	private final LenientAggregationFormatter formatter = LenientAggregationFormatter.INSTANCE;

	@Test
	public void parse_name() {
		then(formatter.parse("Hour", Locale.ENGLISH)).as("Name parsed").isEqualTo(Aggregation.Hour);
	}

	@Test
	public void parse_name_anyCase() {
		then(formatter.parse("month", Locale.ENGLISH)).as("Name parsed in any case")
				.isEqualTo(Aggregation.Month);
	}

	@Test
	public void parse_key() {
		then(formatter.parse(Aggregation.Day.getKey(), Locale.ENGLISH)).as("Key parsed")
				.isEqualTo(Aggregation.Day);
	}

	@Test
	public void parse_unknown() {
		then(formatter.parse("Fortnightly", Locale.ENGLISH)).as("Unknown value parsed as null")
				.isNull();
	}

	@Test
	public void print() {
		then(formatter.print(Aggregation.Hour, Locale.ENGLISH)).as("Name printed").isEqualTo("Hour");
	}

}

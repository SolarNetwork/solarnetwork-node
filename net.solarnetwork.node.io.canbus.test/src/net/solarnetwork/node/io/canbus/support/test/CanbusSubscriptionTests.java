/* ==================================================================
 * CanbusSubscriptionTests.java - 23/09/2019 10:23:57 am
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

package net.solarnetwork.node.io.canbus.support.test;

import static net.solarnetwork.node.io.canbus.CanbusConnection.DATA_FILTER_NONE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.time.Duration;
import org.junit.Test;
import net.solarnetwork.node.io.canbus.support.CanbusSubscription;

/**
 * Test cases for the {@link CanbusSubscription} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CanbusSubscriptionTests {

	@Test
	public void hasLimit_null() {
		// GIVEN
		CanbusSubscription sub = new CanbusSubscription(1, false, null, DATA_FILTER_NONE, null);

		// WHEN
		boolean b = sub.hasLimit();

		// THEN
		assertThat("Null limit", b, equalTo(false));
	}

	@Test
	public void hasLimit_zero() {
		// GIVEN
		CanbusSubscription sub = new CanbusSubscription(1, false, Duration.ofSeconds(0),
				DATA_FILTER_NONE, null);

		// WHEN
		boolean b = sub.hasLimit();

		// THEN
		assertThat("Zero limit", b, equalTo(false));
	}

	@Test
	public void hasLimit_positive() {
		// GIVEN
		CanbusSubscription sub = new CanbusSubscription(1, false, Duration.ofSeconds(1),
				DATA_FILTER_NONE, null);

		// WHEN
		boolean b = sub.hasLimit();

		// THEN
		assertThat("Positive limit", b, equalTo(true));
	}

	@Test
	public void limitValues_subsecond() {
		// GIVEN
		CanbusSubscription sub = new CanbusSubscription(1, false, Duration.ofSeconds(0, 100000),
				DATA_FILTER_NONE, null);

		// WHEN
		int s = sub.getLimitSeconds();
		int m = sub.getLimitMicroseconds();

		// THEN
		assertThat("Limit seconds", s, equalTo(0));
		assertThat("Limit microseconds", m, equalTo(100));
	}

	@Test
	public void limitValues_roundSecnods() {
		// GIVEN
		CanbusSubscription sub = new CanbusSubscription(1, false, Duration.ofSeconds(123),
				DATA_FILTER_NONE, null);

		// WHEN
		int s = sub.getLimitSeconds();
		int m = sub.getLimitMicroseconds();

		// THEN
		assertThat("Limit seconds", s, equalTo(123));
		assertThat("Limit microseconds", m, equalTo(0));
	}

	@Test
	public void limitValues_fractionalSecnods() {
		// GIVEN
		CanbusSubscription sub = new CanbusSubscription(1, false, Duration.ofSeconds(332, 123123000),
				DATA_FILTER_NONE, null);

		// WHEN
		int s = sub.getLimitSeconds();
		int m = sub.getLimitMicroseconds();

		// THEN
		assertThat("Limit seconds", s, equalTo(332));
		assertThat("Limit microseconds", m, equalTo(123123));
	}

	@Test
	public void hasFilter_none() {
		// GIVEN
		CanbusSubscription sub = new CanbusSubscription(1, false, null, DATA_FILTER_NONE, null);

		// WHEN
		boolean b = sub.hasFilter();

		// THEN
		assertThat("None filter", b, equalTo(false));
	}

	@Test
	public void hasFilter_some() {
		// GIVEN
		CanbusSubscription sub = new CanbusSubscription(1, false, null, 0xFF00000000000000L, null);

		// WHEN
		boolean b = sub.hasFilter();

		// THEN
		assertThat("None filter", b, equalTo(true));
	}

	@Test
	public void stringValue_noLimit_noFilter() {
		// GIVEN
		CanbusSubscription sub = new CanbusSubscription(1234, false, Duration.ofSeconds(0),
				DATA_FILTER_NONE, null);

		// WHEN
		String s = sub.toString();

		// THEN
		assertThat("String without limit or filter", s, equalTo("CanbusSubscription{0x4D2}"));
	}

	@Test
	public void stringValue_noLimit_withFilter() {
		// GIVEN
		CanbusSubscription sub = new CanbusSubscription(1234, false, Duration.ofSeconds(0),
				0xFF00000000000000L, null);

		// WHEN
		String s = sub.toString();

		// THEN
		assertThat("String without limit shows with filter", s,
				equalTo("CanbusSubscription{0x4D2,filter=0xFF00000000000000}"));
	}

	@Test
	public void stringValue_withLimit_withFilter() {
		// GIVEN
		CanbusSubscription sub = new CanbusSubscription(1234, false, Duration.ofSeconds(123, 100000),
				0x0000FF00000000FFL, null);

		// WHEN
		String s = sub.toString();

		// THEN
		assertThat("String with limit shows and filter", s,
				equalTo("CanbusSubscription{0x4D2,limit=123.000100,filter=0x0000FF00000000FF}"));
	}
}

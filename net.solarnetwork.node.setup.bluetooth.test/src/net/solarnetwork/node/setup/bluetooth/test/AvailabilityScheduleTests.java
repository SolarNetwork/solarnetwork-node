/* ==================================================================
 * AvailabilityScheduleTests.java - 14/10/2026 9:00:00 AM
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

package net.solarnetwork.node.setup.bluetooth.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import org.junit.Test;
import net.solarnetwork.node.setup.bluetooth.AvailabilitySchedule;

/**
 * Test cases for the {@link AvailabilitySchedule} class.
 *
 * @author elijah
 * @version 1.0
 */
public class AvailabilityScheduleTests {

	// 2026-10-06 is a Tuesday; 2026-10-10 a Saturday; 2026-10-05 a Monday
	private static LocalDateTime tue(int hour, int minute) {
		return LocalDateTime.of(2026, 10, 6, hour, minute);
	}

	private static LocalDateTime sat(int hour, int minute) {
		return LocalDateTime.of(2026, 10, 10, hour, minute);
	}

	private static void assertInvalid(String schedule, String expectedMessagePart) {
		try {
			AvailabilitySchedule.parse(schedule);
			fail("Expected [" + schedule + "] to be rejected");
		} catch ( IllegalArgumentException e ) {
			assertThat("message for [" + schedule + "]", e.getMessage(),
					containsString(expectedMessagePart));
		}
	}

	@Test
	public void parse_blank_isEmpty() {
		assertThat(AvailabilitySchedule.parse(null).isEmpty(), is(true));
		assertThat(AvailabilitySchedule.parse("").isEmpty(), is(true));
		assertThat(AvailabilitySchedule.parse("  \n\t ").isEmpty(), is(true));
		assertThat(AvailabilitySchedule.parse("# only a comment\n").isEmpty(), is(true));
		assertThat(AvailabilitySchedule.parse("").applies(tue(12, 0)), is(false));
	}

	@Test
	public void parse_commentsAndBlankLines() {
		AvailabilitySchedule s = AvailabilitySchedule
				.parse("# business hours\n\nMon-Fri 07:00-18:00   # weekdays\n\n");
		assertThat(s.getWindows(), hasSize(1));
		assertThat(s.applies(tue(12, 0)), is(true));
	}

	@Test
	public void parse_semicolonAndNewlineEquivalent() {
		AvailabilitySchedule a = AvailabilitySchedule.parse("Mon-Fri 07:00-18:00; Sat,Sun 08:00-12:00");
		AvailabilitySchedule b = AvailabilitySchedule.parse("Mon-Fri 07:00-18:00\nSat,Sun 08:00-12:00");
		assertThat(a.toString(), equalTo(b.toString()));
		assertThat(a.applies(sat(9, 0)), is(true));
		assertThat(a.applies(sat(13, 0)), is(false));
		assertThat(b.applies(sat(9, 0)), is(true));
	}

	@Test
	public void parse_weekdayWindow_edges() {
		AvailabilitySchedule s = AvailabilitySchedule.parse("Mon-Fri 07:00-18:00");
		assertThat("before start", s.applies(tue(6, 59)), is(false));
		assertThat("start inclusive", s.applies(tue(7, 0)), is(true));
		assertThat("inside", s.applies(tue(17, 59)), is(true));
		assertThat("end exclusive", s.applies(tue(18, 0)), is(false));
		assertThat("weekend", s.applies(sat(10, 0)), is(false));
		assertThat("Monday before start", s.applies(LocalDateTime.of(2026, 10, 5, 6, 59)), is(false));
	}

	@Test
	public void parse_multiRange_innerEndExclusive() {
		AvailabilitySchedule s = AvailabilitySchedule.parse("Mon-Fri 06:00-09:00,16:00-20:00");
		assertThat(s.applies(tue(8, 59)), is(true));
		assertThat("inner end is exclusive too", s.applies(tue(9, 0)), is(false));
		assertThat(s.applies(tue(9, 30)), is(false));
		assertThat(s.applies(tue(16, 0)), is(true));
		assertThat(s.applies(tue(19, 59)), is(true));
		assertThat(s.applies(tue(20, 0)), is(false));
	}

	@Test
	public void parse_adjacentRanges_notMerged() {
		AvailabilitySchedule s = AvailabilitySchedule.parse("06:00-09:00,09:01-12:00");
		assertThat(s.applies(tue(8, 59)), is(true));
		assertThat("gap minute stays off", s.applies(tue(9, 0)), is(false));
		assertThat(s.applies(tue(9, 1)), is(true));
	}

	@Test
	public void parse_numericDaysAndWholeHours() {
		AvailabilitySchedule numeric = AvailabilitySchedule.parse("1-5 7-18");
		AvailabilitySchedule named = AvailabilitySchedule.parse("Mon-Fri 07:00-18:00");
		assertThat(numeric.toString(), equalTo(named.toString()));
		assertThat(numeric.applies(tue(7, 0)), is(true));
		assertThat(numeric.applies(tue(18, 0)), is(false));
		assertThat(numeric.applies(sat(12, 0)), is(false));

		AvailabilitySchedule allSunday = AvailabilitySchedule.parse("Sun 0-24");
		LocalDateTime sunday = LocalDateTime.of(2026, 10, 11, 23, 59);
		assertThat(allSunday.applies(sunday), is(true));
		assertThat(allSunday.applies(LocalDateTime.of(2026, 10, 11, 0, 0)), is(true));
		assertThat(allSunday.applies(sat(23, 59)), is(false));

		AvailabilitySchedule endOfDay = AvailabilitySchedule.parse("22:00-24:00");
		assertThat(endOfDay.applies(tue(23, 59)), is(true));
		assertThat(endOfDay.applies(tue(21, 59)), is(false));
	}

	@Test
	public void parse_wildcardAndLoneTimes() {
		assertThat(AvailabilitySchedule.parse("* 07:00-18:00").applies(sat(12, 0)), is(true));
		assertThat(AvailabilitySchedule.parse("07:00-18:00").applies(sat(12, 0)), is(true));
		assertThat(AvailabilitySchedule.parse("07:00-18:00").applies(sat(19, 0)), is(false));
		AvailabilitySchedule weekend = AvailabilitySchedule.parse("Sat-Sun *");
		assertThat(weekend.applies(sat(0, 0)), is(true));
		assertThat(weekend.applies(sat(23, 59)), is(true));
		assertThat(weekend.applies(tue(12, 0)), is(false));
		AvailabilitySchedule always = AvailabilitySchedule.parse("*");
		assertThat(always.isEmpty(), is(false));
		assertThat(always.applies(tue(12, 0)), is(true));
	}

	@Test
	public void parse_threeColumns_daysWeekdaysTimes() {
		AvailabilitySchedule s = AvailabilitySchedule.parse("1-15 Mon-Fri 07:00-18:00");
		assertThat("6th of the month", s.applies(tue(12, 0)), is(true));
		assertThat("20th of the month (a Tuesday)", s.applies(LocalDateTime.of(2026, 10, 20, 12, 0)),
				is(false));
		assertThat("10th but Saturday", s.applies(sat(12, 0)), is(false));
	}

	@Test
	public void parse_fourColumns_months() {
		AvailabilitySchedule summer = AvailabilitySchedule.parse("Jun-Aug * Mon-Fri 07:00-18:00");
		assertThat("October", summer.applies(tue(12, 0)), is(false));
		// 2026-07-07 is a Tuesday
		assertThat("July weekday", summer.applies(LocalDateTime.of(2026, 7, 7, 12, 0)), is(true));
		// 2026-07-11 is a Saturday
		assertThat("July weekend", summer.applies(LocalDateTime.of(2026, 7, 11, 12, 0)), is(false));

		AvailabilitySchedule xmas = AvailabilitySchedule.parse("Dec 24-26 * *");
		assertThat(xmas.applies(LocalDateTime.of(2026, 12, 24, 0, 0)), is(true));
		assertThat(xmas.applies(LocalDateTime.of(2026, 12, 26, 23, 59)), is(true));
		assertThat(xmas.applies(LocalDateTime.of(2026, 12, 27, 0, 0)), is(false));
		assertThat(xmas.applies(LocalDateTime.of(2026, 11, 25, 12, 0)), is(false));

		AvailabilitySchedule numeric = AvailabilitySchedule.parse("12 24-26 * *");
		assertThat(numeric.toString(), equalTo(xmas.toString()));

		AvailabilitySchedule leap = AvailabilitySchedule.parse("Feb 29 * *");
		assertThat(leap.applies(LocalDateTime.of(2028, 2, 29, 12, 0)), is(true));
		assertThat(leap.applies(LocalDateTime.of(2027, 2, 28, 12, 0)), is(false));
	}

	@Test
	public void parse_monthNamesFull() {
		AvailabilitySchedule s = AvailabilitySchedule.parse("January-March * Monday-Friday 7-18");
		assertThat(s.applies(LocalDateTime.of(2026, 2, 3, 12, 0)), is(true)); // Tuesday
		assertThat(s.applies(tue(12, 0)), is(false));
	}

	@Test
	public void parse_reversedRanges_rejected() {
		assertInvalid("Mon-Fri 22:00-06:00", "Window 1");
		assertInvalid("Mon-Fri 22:00-06:00", "ascending");
		assertInvalid("Fri-Mon 7-18", "descending");
		assertInvalid("Dec-Jan * * *", "descending");
		assertInvalid("* 20-10 * *", "descending");
		assertInvalid("Mon-Fri 07:00-07:00", "at least one minute");
	}

	@Test
	public void parse_invalidTokens_rejected() {
		assertInvalid("Mon-Fry 7-18", "Window 1");
		assertInvalid("Mon-Fri 25:00-26:00", "Window 1");
		assertInvalid("Mon-Fri 24:30-25:00", "Window 1");
		assertInvalid("* 32 * *", "Window 1");
		assertInvalid("Jan-Fri 7-18", "Window 1");
		assertInvalid("a b c d e", "more than 4 columns");
		assertInvalid("Mon-Fri 07:00-18:00\nSat 08:00-07:00", "Window 2");
	}

	@Test
	public void parse_localeFallback() {
		Locale original = Locale.getDefault();
		try {
			Locale.setDefault(Locale.GERMANY);
			AvailabilitySchedule english = AvailabilitySchedule.parse("Mon-Fri 07:00-18:00");
			assertThat("English always parses", english.applies(tue(12, 0)), is(true));
			AvailabilitySchedule german = AvailabilitySchedule.parse("Montag-Freitag 07:00-18:00");
			assertThat("default locale names parse", german.applies(tue(12, 0)), is(true));
			assertThat(german.toString(), equalTo(english.toString()));
		} finally {
			Locale.setDefault(original);
		}
	}

	@Test
	public void parse_commentContainingSemicolon() {
		AvailabilitySchedule s = AvailabilitySchedule
				.parse("Mon-Fri 07:00-18:00 # on-site hours; call first\nSat 08:00-12:00");
		assertThat(s.getWindows(), hasSize(2));
		assertThat(s.applies(tue(12, 0)), is(true));
		assertThat(s.applies(sat(9, 0)), is(true));
	}

	@Test
	public void parse_extraDelimiters_rejected() {
		assertInvalid("Mon-Wed-Fri 7-18", "Window 1");
		assertInvalid("Mon-Fri-Wed 7-18", "Window 1");
		assertInvalid("Mon-Fri 7-18-20", "Window 1");
		assertInvalid("* 1-5-20 * *", "Window 1");
		assertInvalid("Jan-Mar-Dec * * *", "Window 1");
	}

	@Test
	public void parse_narrowNames_rejected() {
		assertInvalid("S 7-18", "three letters");
		assertInvalid("T-S 7-18", "three letters");
		assertInvalid("J * * *", "three letters");
		assertInvalid("Mo-Fr 7-18", "three letters");
	}

	@Test
	public void parse_loneTime_rejectedWithHint() {
		assertInvalid("Mon-Fri 12", "such as 12-12:59");
		assertInvalid("Sat 09:00", "such as 09:00-09:00");
		// other columns do accept single values
		assertThat(AvailabilitySchedule.parse("Sat 9-10").applies(sat(9, 30)), is(true));
		assertThat(
				AvailabilitySchedule.parse("Dec 25 * *").applies(LocalDateTime.of(2026, 12, 25, 1, 0)),
				is(true));
	}

	@Test
	public void nextBoundary_skipsDstGap() {
		// NZ clocks go from 02:00 NZST to 03:00 NZDT on Sunday 2026-09-27, so
		// 02:00 that day never happens and the window first applies the next Sunday
		AvailabilitySchedule s = AvailabilitySchedule.parse("Sun 02:00-03:00");
		LocalDateTime from = LocalDateTime.of(2026, 9, 27, 1, 0);
		assertThat("zone-unaware returns the gap time", s.nextBoundary(from),
				equalTo(LocalDateTime.of(2026, 9, 27, 2, 0)));
		assertThat("zone-aware skips the gap", s.nextBoundary(from, ZoneId.of("Pacific/Auckland")),
				equalTo(LocalDateTime.of(2026, 10, 4, 2, 0)));
	}

	@Test
	public void nextBoundary() {
		AvailabilitySchedule s = AvailabilitySchedule.parse("Mon-Fri 07:00-18:00");
		assertThat("inside: end of window", s.nextBoundary(tue(12, 0)), equalTo(tue(18, 0)));
		assertThat("outside: next start", s.nextBoundary(tue(18, 0)),
				equalTo(LocalDateTime.of(2026, 10, 7, 7, 0)));
		assertThat("before start same day", s.nextBoundary(tue(6, 30)), equalTo(tue(7, 0)));
		assertThat("weekend wraps to Monday", s.nextBoundary(sat(12, 0)),
				equalTo(LocalDateTime.of(2026, 10, 12, 7, 0)));
		assertThat("empty schedule", AvailabilitySchedule.parse("").nextBoundary(tue(12, 0)),
				is(nullValue()));
		assertThat("always applies", AvailabilitySchedule.parse("*").nextBoundary(tue(12, 0)),
				is(nullValue()));
	}

	@Test
	public void nextBoundary_fullDays() {
		AvailabilitySchedule s = AvailabilitySchedule.parse("Dec 24-26 * *");
		assertThat(s.nextBoundary(LocalDateTime.of(2026, 12, 20, 12, 0)),
				equalTo(LocalDateTime.of(2026, 12, 24, 0, 0)));
		assertThat(s.nextBoundary(LocalDateTime.of(2026, 12, 25, 12, 0)),
				equalTo(LocalDateTime.of(2026, 12, 27, 0, 0)));
	}

	@Test
	public void toString_canonical() {
		AvailabilitySchedule s = AvailabilitySchedule
				.parse("mon-fri 7-18; Jun-Aug 1-15 * 06:00-09:00,16:00-20:00; Dec 24-26 * *");
		assertThat(s.toString(), is(notNullValue()));
		assertThat(s.toString(), equalTo(
				"* * Mon-Fri 07:00-18:00; Jun-Aug 1-15 * 06:00-09:00,16:00-20:00; Dec 24-26 * *"));
		assertThat("round trip", AvailabilitySchedule.parse(s.toString()).toString(),
				equalTo(s.toString()));
	}

}

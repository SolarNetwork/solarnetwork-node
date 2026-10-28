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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.node.setup.bluetooth.AvailabilitySchedule;

/**
 * Test cases for the {@link AvailabilitySchedule} class.
 *
 * @author elijah
 * @version 1.0
 */
public class AvailabilityScheduleTests {

	private static final String HEADER = AvailabilitySchedule.EXAMPLE_HEADER + "\n";

	// 2026-10-06 is a Tuesday; 2026-10-10 a Saturday; 2026-10-05 a Monday
	private static LocalDateTime tue(int hour, int minute) {
		return LocalDateTime.of(2026, 10, 6, hour, minute);
	}

	private static LocalDateTime sat(int hour, int minute) {
		return LocalDateTime.of(2026, 10, 10, hour, minute);
	}

	private static AvailabilitySchedule parse(String rows) {
		return AvailabilitySchedule.parse(HEADER + rows);
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
		assertThat("header only", AvailabilitySchedule.parse(HEADER).isEmpty(), is(true));
		assertThat(AvailabilitySchedule.parse("").applies(tue(12, 0)), is(false));
	}

	@Test
	public void parse_commentsAndBlankLines() {
		AvailabilitySchedule s = AvailabilitySchedule
				.parse("# business hours\n" + HEADER + "\n# weekdays\n,,Mon-Fri,07:00-18:00,1\n\n");
		assertThat(s.getRules(), hasSize(1));
		assertThat(s.applies(tue(12, 0)), is(true));
	}

	@Test
	public void parse_weekdayWindow_edges() {
		AvailabilitySchedule s = parse(",,Mon-Fri,07:00-18:00,1");
		assertThat("before start", s.applies(tue(6, 59)), is(false));
		assertThat("start inclusive", s.applies(tue(7, 0)), is(true));
		assertThat("inside", s.applies(tue(17, 59)), is(true));
		assertThat("end exclusive", s.applies(tue(18, 0)), is(false));
		assertThat("weekend", s.applies(sat(10, 0)), is(false));
		assertThat("Monday before start", s.applies(LocalDateTime.of(2026, 10, 5, 6, 59)), is(false));
	}

	@Test
	public void parse_multiRange_innerEndExclusive() {
		AvailabilitySchedule s = parse(",,Mon-Fri,\"06:00-09:00,16:00-20:00\",1");
		assertThat(s.applies(tue(8, 59)), is(true));
		assertThat("inner end is exclusive too", s.applies(tue(9, 0)), is(false));
		assertThat(s.applies(tue(9, 30)), is(false));
		assertThat(s.applies(tue(16, 0)), is(true));
		assertThat(s.applies(tue(19, 59)), is(true));
		assertThat(s.applies(tue(20, 0)), is(false));
	}

	@Test
	public void parse_oneMinuteGap_separateRules() {
		AvailabilitySchedule s = parse(",,,06:00-09:00,1\n,,,09:01-12:00,1");
		assertThat(s.applies(tue(8, 59)), is(true));
		assertThat("gap minute stays off", s.applies(tue(9, 0)), is(false));
		assertThat(s.applies(tue(9, 1)), is(true));
		assertThat("next boundary is the gap", s.nextBoundary(tue(8, 0)), equalTo(tue(9, 0)));
		assertThat("gap ends a minute later", s.nextBoundary(tue(9, 0)), equalTo(tue(9, 1)));
	}

	@Test
	public void parse_multipleRules() {
		AvailabilitySchedule s = parse(",,Mon-Fri,07:00-18:00,1\n,,\"Sat,Sun\",08:00-12:00,1");
		assertThat(s.getRules(), hasSize(2));
		assertThat(s.applies(tue(12, 0)), is(true));
		assertThat(s.applies(sat(9, 0)), is(true));
		assertThat(s.applies(sat(13, 0)), is(false));
	}

	@Test
	public void parse_numericDaysAndWholeHours() {
		AvailabilitySchedule numeric = parse(",,1-5,7-18,1");
		AvailabilitySchedule named = parse(",,Mon-Fri,07:00-18:00,1");
		assertThat(numeric.toString(), equalTo(named.toString()));
		assertThat(numeric.applies(tue(7, 0)), is(true));
		assertThat(numeric.applies(tue(18, 0)), is(false));
		assertThat(numeric.applies(sat(12, 0)), is(false));

		AvailabilitySchedule allSunday = parse(",,Sun,0-24,1");
		assertThat(allSunday.applies(LocalDateTime.of(2026, 10, 11, 23, 59)), is(true));
		assertThat(allSunday.applies(LocalDateTime.of(2026, 10, 11, 0, 0)), is(true));
		assertThat(allSunday.applies(sat(23, 59)), is(false));

		AvailabilitySchedule endOfDay = parse(",,,22:00-24:00,1");
		assertThat(endOfDay.applies(tue(23, 59)), is(true));
		assertThat(endOfDay.applies(tue(21, 59)), is(false));

		AvailabilitySchedule oneHour = parse(",,Sat,9,1");
		assertThat("whole hour is that hour", oneHour.applies(sat(9, 30)), is(true));
		assertThat(oneHour.applies(sat(10, 0)), is(false));
	}

	@Test
	public void parse_emptyCellsMatchAll() {
		assertThat(parse(",,,07:00-18:00,1").applies(sat(12, 0)), is(true));
		assertThat(parse(",,,07:00-18:00,1").applies(sat(19, 0)), is(false));
		AvailabilitySchedule weekend = parse(",,Sat-Sun,,1");
		assertThat(weekend.applies(sat(0, 0)), is(true));
		assertThat(weekend.applies(sat(23, 59)), is(true));
		assertThat(weekend.applies(tue(12, 0)), is(false));
		AvailabilitySchedule always = parse(",,,,1");
		assertThat(always.isEmpty(), is(false));
		assertThat(always.applies(tue(12, 0)), is(true));
	}

	@Test
	public void parse_daysAndMonths() {
		AvailabilitySchedule s = parse(",1-15,Mon-Fri,07:00-18:00,1");
		assertThat("6th of the month", s.applies(tue(12, 0)), is(true));
		assertThat("20th of the month (a Tuesday)", s.applies(LocalDateTime.of(2026, 10, 20, 12, 0)),
				is(false));
		assertThat("10th but Saturday", s.applies(sat(12, 0)), is(false));

		AvailabilitySchedule summer = parse("Jun-Aug,,Mon-Fri,07:00-18:00,1");
		assertThat("October", summer.applies(tue(12, 0)), is(false));
		// 2026-07-07 is a Tuesday
		assertThat("July weekday", summer.applies(LocalDateTime.of(2026, 7, 7, 12, 0)), is(true));
		// 2026-07-11 is a Saturday
		assertThat("July weekend", summer.applies(LocalDateTime.of(2026, 7, 11, 12, 0)), is(false));

		AvailabilitySchedule xmas = parse("Dec,24-26,,,1");
		assertThat(xmas.applies(LocalDateTime.of(2026, 12, 24, 0, 0)), is(true));
		assertThat(xmas.applies(LocalDateTime.of(2026, 12, 26, 23, 59)), is(true));
		assertThat(xmas.applies(LocalDateTime.of(2026, 12, 27, 0, 0)), is(false));
		assertThat(xmas.applies(LocalDateTime.of(2026, 11, 25, 12, 0)), is(false));
		assertThat(parse("12,24-26,,,1").toString(), equalTo(xmas.toString()));

		AvailabilitySchedule leap = parse("Feb,29,,,1");
		assertThat(leap.applies(LocalDateTime.of(2028, 2, 29, 12, 0)), is(true));
		assertThat(leap.applies(LocalDateTime.of(2027, 2, 28, 12, 0)), is(false));

		AvailabilitySchedule full = parse("January-March,,Monday-Friday,7-18,1");
		assertThat(full.applies(LocalDateTime.of(2026, 2, 3, 12, 0)), is(true)); // Tuesday
		assertThat(full.applies(tue(12, 0)), is(false));
	}

	@Test
	public void parse_blackoutRow() {
		AvailabilitySchedule s = parse("Dec,25,,,0\n,,Mon-Fri,07:00-18:00,1");
		// 2026-12-24 is a Thursday, 2026-12-25 a Friday
		assertThat(s.applies(LocalDateTime.of(2026, 12, 24, 12, 0)), is(true));
		assertThat("blackout first match wins", s.applies(LocalDateTime.of(2026, 12, 25, 12, 0)),
				is(false));
		assertThat("next start skips the blackout",
				s.nextBoundary(LocalDateTime.of(2026, 12, 24, 20, 0)),
				equalTo(LocalDateTime.of(2026, 12, 28, 7, 0)));

		AvailabilitySchedule later = parse(",,Mon-Fri,07:00-18:00,1\nDec,25,,,0");
		assertThat("a later blackout never matches",
				later.applies(LocalDateTime.of(2026, 12, 25, 12, 0)), is(true));
	}

	@Test
	public void parse_reversedRanges_rejected() {
		assertInvalid(HEADER + ",,Mon-Fri,22:00-06:00,1", "CSV row 2");
		assertInvalid(HEADER + ",,Mon-Fri,22:00-06:00,1", "Time range [22:00-06:00] is descending");
		assertInvalid(HEADER + ",,Fri-Mon,7-18,1", "Range [Fri-Mon] is descending");
		assertInvalid(HEADER + "Dec-Jan,,,,1", "Range [Dec-Jan] is descending");
		assertInvalid(HEADER + ",20-10,,,1", "Range [20-10] is descending");
		assertInvalid(HEADER + ",,Mon-Fri,07:00-07:00,1", "is empty");
		assertInvalid(HEADER + ",,Sat,09:00,1", "Time range [09:00] is empty");
		assertInvalid(HEADER + ",,Mon-Fri,23:00-24:30,1", "ends after 24:00");
		assertInvalid(HEADER + ",,,\"06:00-09:00,09:01-12:00\",1",
				"Time ranges [06:00-09:00] and [09:01-12:00] are one minute apart");
	}

	@Test
	public void parse_invalidTokens_rejected() {
		assertInvalid(HEADER + ",,Mon-Fry,7-18,1", "CSV row 2");
		assertInvalid(HEADER + ",,Mon-Fri,25:00-26:00,1", "CSV row 2");
		assertInvalid(HEADER + ",32,,,1", "CSV row 2");
		assertInvalid(HEADER + "Jan-Fri,,,7-18,1", "CSV row 2");
		assertInvalid(HEADER + ",,*,7-18,1", "CSV row 2");
		assertInvalid(HEADER + ",,Mon-Fri,07:00-18:00,1\n,,Sat,08:00-07:00,1", "CSV row 3");
		assertInvalid(HEADER + ",,Mon-Fri,07:00-18:00", "CSV row 2");
	}

	@Test
	public void parse_missingHeader_rejected() {
		assertInvalid(",,Mon-Fri,07:00-18:00,1", "header row");
		assertInvalid("# hours\n,,Mon-Fri,07:00-18:00,1\n,,Sat,08:00-12:00,1", "header row");
		assertInvalid("Mon-Fri 07:00-18:00", "Not enough columns in CSV header");
	}

	@Test
	public void parse_availabilityValue_rejected() {
		assertInvalid(HEADER + ",,Mon-Fri,07:00-18:00,2", "availability value [2] must be 0 or 1");
		assertInvalid(HEADER + ",,Mon-Fri,07:00-18:00,", "must have exactly one availability value");
		assertInvalid(HEADER + ",,Mon-Fri,07:00-18:00,yes", "rate value");
		assertInvalid("Month,Day,Weekday,Time,Available,Other\n,,Mon-Fri,07:00-18:00,1,1",
				"must have exactly one availability value");
		assertThat(parse(",,Mon-Fri,07:00-18:00,1.0").applies(tue(12, 0)), is(true));
	}

	@Test
	public void nextBoundary_skipsDstGap() {
		// NZ clocks go from 02:00 NZST to 03:00 NZDT on Sunday 2026-09-27, so
		// 02:00 that day never happens and the window first applies the next Sunday
		AvailabilitySchedule s = parse(",,Sun,02:00-03:00,1");
		LocalDateTime from = LocalDateTime.of(2026, 9, 27, 1, 0);
		assertThat("zone-unaware returns the gap time", s.nextBoundary(from),
				equalTo(LocalDateTime.of(2026, 9, 27, 2, 0)));
		assertThat("zone-aware skips the gap", s.nextBoundary(from, ZoneId.of("Pacific/Auckland")),
				equalTo(LocalDateTime.of(2026, 10, 4, 2, 0)));
	}

	@Test
	public void nextBoundary() {
		AvailabilitySchedule s = parse(",,Mon-Fri,07:00-18:00,1");
		assertThat("inside: end of window", s.nextBoundary(tue(12, 0)), equalTo(tue(18, 0)));
		assertThat("outside: next start", s.nextBoundary(tue(18, 0)),
				equalTo(LocalDateTime.of(2026, 10, 7, 7, 0)));
		assertThat("before start same day", s.nextBoundary(tue(6, 30)), equalTo(tue(7, 0)));
		assertThat("weekend wraps to Monday", s.nextBoundary(sat(12, 0)),
				equalTo(LocalDateTime.of(2026, 10, 12, 7, 0)));
		assertThat("empty schedule", AvailabilitySchedule.parse("").nextBoundary(tue(12, 0)),
				is(nullValue()));
		assertThat("always applies", parse(",,,,1").nextBoundary(tue(12, 0)), is(nullValue()));
	}

	@Test
	public void nextBoundary_multiRange() {
		AvailabilitySchedule s = parse(",,Mon-Fri,\"06:00-09:00,16:00-20:00\",1");
		assertThat("inner end", s.nextBoundary(tue(8, 0)), equalTo(tue(9, 0)));
		assertThat("next inner start", s.nextBoundary(tue(9, 0)), equalTo(tue(16, 0)));
	}

	@Test
	public void nextBoundary_fullDays() {
		AvailabilitySchedule s = parse("Dec,24-26,,,1");
		assertThat(s.nextBoundary(LocalDateTime.of(2026, 12, 20, 12, 0)),
				equalTo(LocalDateTime.of(2026, 12, 24, 0, 0)));
		assertThat(s.nextBoundary(LocalDateTime.of(2026, 12, 25, 12, 0)),
				equalTo(LocalDateTime.of(2026, 12, 27, 0, 0)));
	}

	@Test
	public void toString_canonical() {
		AvailabilitySchedule s = parse(
				",,mon-fri,07:00-18:00,1\nJun-Aug,1-15,,\"06:00-09:00,16:00-20:00\",1\nDec,24-26,,,1");
		assertThat(s.toString(),
				equalTo(",,Mon-Fri,7-18,1; Jun-Aug,1-15,,\"6-9,16-20\",1; Dec,24-26,,,1"));
		assertThat("round trip", AvailabilitySchedule.parse(s.toCsv()).toString(),
				equalTo(s.toString()));
	}

	private static final List<Object> HEADER_ROW = List.of("Month", "Day", "Weekday", "Time",
			"Available");

	@Test
	public void fromMetadata_string() {
		AvailabilitySchedule s = AvailabilitySchedule.fromMetadata(HEADER + ",,Mon-Fri,07:00-18:00,1");
		assertThat(s.applies(tue(12, 0)), is(true));
		assertThat(s.applies(sat(12, 0)), is(false));
	}

	@Test
	public void fromMetadata_rows() {
		AvailabilitySchedule s = AvailabilitySchedule
				.fromMetadata(List.of(HEADER_ROW, Arrays.asList(null, null, "Sat,Sun", "08:00-12:00", 1),
						Arrays.asList("", "", "Mon-Fri", "07:00-09:00", "1")));
		assertThat("comma cell needs no quoting", s.applies(sat(9, 0)), is(true));
		assertThat("numeric cell", s.applies(sat(13, 0)), is(false));
		assertThat(s.getRules(), hasSize(2));
		assertThat("same as the string form", s.toString(), equalTo(AvailabilitySchedule
				.parse(HEADER + ",,\"Sat,Sun\",08:00-12:00,1\n,,Mon-Fri,07:00-09:00,1").toString()));
	}

	@Test
	public void fromMetadata_rows_validatedLikeString() {
		try {
			AvailabilitySchedule.fromMetadata(
					List.of(HEADER_ROW, Arrays.asList(null, null, null, "22:00-06:00", 1)));
			fail("descending range should be rejected");
		} catch ( IllegalArgumentException e ) {
			assertThat(e.getMessage(), containsString("is descending"));
		}
		try {
			AvailabilitySchedule.fromMetadata(List.of(Arrays.asList(null, null, "Mon-Fri", "7-18", 1)));
			fail("missing header should be rejected");
		} catch ( IllegalArgumentException e ) {
			assertThat(e.getMessage(), containsString("header row"));
		}
	}

	@Test
	public void fromMetadata_empty() {
		assertThat(AvailabilitySchedule.fromMetadata("").isEmpty(), is(true));
		assertThat(AvailabilitySchedule.fromMetadata(List.of()).isEmpty(), is(true));
		assertThat(AvailabilitySchedule.fromMetadata(List.of(HEADER_ROW)).isEmpty(), is(true));
	}

	@Test
	public void fromMetadata_unsupported() {
		for ( Object value : new Object[] { List.of(AvailabilitySchedule.EXAMPLE_HEADER, ",,,,1"),
				List.of(HEADER_ROW, List.of(Map.of("a", 1), "", "", "", "1")), Map.of("a", 1), 42 } ) {
			try {
				AvailabilitySchedule.fromMetadata(value);
				fail("Expected [" + value + "] to be rejected");
			} catch ( IllegalArgumentException e ) {
				// expected
			}
		}
	}

}

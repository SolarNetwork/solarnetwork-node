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

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
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
		// @formatter:off
		thenIllegalArgumentException()
			.as("Message for [%s]", schedule)
			.isThrownBy(() -> AvailabilitySchedule.parse(schedule))
			.withMessageContaining(expectedMessagePart)
			;
		// @formatter:on
	}

	@Test
	public void parse_blank_isEmpty() {
		// @formatter:off
		then(AvailabilitySchedule.parse(null))
			.as("Null is empty")
			.returns(true, from(AvailabilitySchedule::isEmpty))
			;
		then(AvailabilitySchedule.parse(""))
			.as("Empty string is empty")
			.returns(true, from(AvailabilitySchedule::isEmpty))
			.as("Empty schedule never applies")
			.returns(false, from(s -> s.applies(tue(12, 0))))
			;
		then(AvailabilitySchedule.parse("  \n\t "))
			.as("Whitespace is empty")
			.returns(true, from(AvailabilitySchedule::isEmpty))
			;
		then(AvailabilitySchedule.parse("# only a comment\n"))
			.as("Comment only is empty")
			.returns(true, from(AvailabilitySchedule::isEmpty))
			;
		then(AvailabilitySchedule.parse(HEADER))
			.as("Header only")
			.returns(true, from(AvailabilitySchedule::isEmpty))
			;
		// @formatter:on
	}

	@Test
	public void parse_commentsAndBlankLines() {
		// WHEN
		AvailabilitySchedule s = AvailabilitySchedule
				.parse("# business hours\n" + HEADER + "\n# weekdays\n,,Mon-Fri,07:00-18:00,1\n\n");

		// THEN
		// @formatter:off
		then(s.getRules())
			.as("One rule parsed")
			.hasSize(1)
			;
		then(s)
			.as("Weekday window applies")
			.returns(true, from(x -> x.applies(tue(12, 0))))
			;
		// @formatter:on
	}

	@Test
	public void parse_weekdayWindow_edges() {
		// WHEN
		AvailabilitySchedule s = parse(",,Mon-Fri,07:00-18:00,1");

		// THEN
		// @formatter:off
		then(s)
			.as("Before start")
			.returns(false, from(x -> x.applies(tue(6, 59))))
			.as("Start inclusive")
			.returns(true, from(x -> x.applies(tue(7, 0))))
			.as("Inside")
			.returns(true, from(x -> x.applies(tue(17, 59))))
			.as("End exclusive")
			.returns(false, from(x -> x.applies(tue(18, 0))))
			.as("Weekend")
			.returns(false, from(x -> x.applies(sat(10, 0))))
			.as("Monday before start")
			.returns(false, from(x -> x.applies(LocalDateTime.of(2026, 10, 5, 6, 59))))
			;
		// @formatter:on
	}

	@Test
	public void parse_multiRange_innerEndExclusive() {
		// WHEN
		AvailabilitySchedule s = parse(",,Mon-Fri,\"06:00-09:00,16:00-20:00\",1");

		// THEN
		// @formatter:off
		then(s)
			.as("Inside first range")
			.returns(true, from(x -> x.applies(tue(8, 59))))
			.as("Inner end is exclusive too")
			.returns(false, from(x -> x.applies(tue(9, 0))))
			.as("Between ranges")
			.returns(false, from(x -> x.applies(tue(9, 30))))
			.as("Second range start inclusive")
			.returns(true, from(x -> x.applies(tue(16, 0))))
			.as("Inside second range")
			.returns(true, from(x -> x.applies(tue(19, 59))))
			.as("Second range end exclusive")
			.returns(false, from(x -> x.applies(tue(20, 0))))
			;
		// @formatter:on
	}

	@Test
	public void parse_oneMinuteGap_separateRules() {
		// WHEN
		AvailabilitySchedule s = parse(",,,06:00-09:00,1\n,,,09:01-12:00,1");

		// THEN
		// @formatter:off
		then(s)
			.as("Inside first rule")
			.returns(true, from(x -> x.applies(tue(8, 59))))
			.as("Gap minute stays off")
			.returns(false, from(x -> x.applies(tue(9, 0))))
			.as("Second rule start inclusive")
			.returns(true, from(x -> x.applies(tue(9, 1))))
			.as("Next boundary is the gap")
			.returns(tue(9, 0), from(x -> x.nextBoundary(tue(8, 0))))
			.as("Gap ends a minute later")
			.returns(tue(9, 1), from(x -> x.nextBoundary(tue(9, 0))))
			;
		// @formatter:on
	}

	@Test
	public void parse_multipleRules() {
		// WHEN
		AvailabilitySchedule s = parse(",,Mon-Fri,07:00-18:00,1\n,,\"Sat,Sun\",08:00-12:00,1");

		// THEN
		// @formatter:off
		then(s.getRules())
			.as("Two rules parsed")
			.hasSize(2)
			;
		then(s)
			.as("Weekday window applies")
			.returns(true, from(x -> x.applies(tue(12, 0))))
			.as("Weekend window applies")
			.returns(true, from(x -> x.applies(sat(9, 0))))
			.as("Outside weekend window")
			.returns(false, from(x -> x.applies(sat(13, 0))))
			;
		// @formatter:on
	}

	@Test
	public void parse_numericDaysAndWholeHours() {
		// WHEN
		AvailabilitySchedule numeric = parse(",,1-5,7-18,1");
		AvailabilitySchedule named = parse(",,Mon-Fri,07:00-18:00,1");
		AvailabilitySchedule allSunday = parse(",,Sun,0-24,1");
		AvailabilitySchedule endOfDay = parse(",,,22:00-24:00,1");
		AvailabilitySchedule oneHour = parse(",,Sat,9,1");

		// THEN
		// @formatter:off
		then(numeric)
			.as("Numeric same as named")
			.hasToString(named.toString())
			.as("Numeric start inclusive")
			.returns(true, from(x -> x.applies(tue(7, 0))))
			.as("Numeric end exclusive")
			.returns(false, from(x -> x.applies(tue(18, 0))))
			.as("Numeric weekend")
			.returns(false, from(x -> x.applies(sat(12, 0))))
			;
		then(allSunday)
			.as("End of Sunday")
			.returns(true, from(x -> x.applies(LocalDateTime.of(2026, 10, 11, 23, 59))))
			.as("Start of Sunday")
			.returns(true, from(x -> x.applies(LocalDateTime.of(2026, 10, 11, 0, 0))))
			.as("Saturday")
			.returns(false, from(x -> x.applies(sat(23, 59))))
			;
		then(endOfDay)
			.as("Last minute of day")
			.returns(true, from(x -> x.applies(tue(23, 59))))
			.as("Before start")
			.returns(false, from(x -> x.applies(tue(21, 59))))
			;
		then(oneHour)
			.as("Whole hour is that hour")
			.returns(true, from(x -> x.applies(sat(9, 30))))
			.as("Whole hour end exclusive")
			.returns(false, from(x -> x.applies(sat(10, 0))))
			;
		// @formatter:on
	}

	@Test
	public void parse_emptyCellsMatchAll() {
		// WHEN
		AvailabilitySchedule anyDay = parse(",,,07:00-18:00,1");
		AvailabilitySchedule weekend = parse(",,Sat-Sun,,1");
		AvailabilitySchedule always = parse(",,,,1");

		// THEN
		// @formatter:off
		then(anyDay)
			.as("Any day inside window")
			.returns(true, from(x -> x.applies(sat(12, 0))))
			.as("Any day outside window")
			.returns(false, from(x -> x.applies(sat(19, 0))))
			;
		then(weekend)
			.as("Start of weekend day")
			.returns(true, from(x -> x.applies(sat(0, 0))))
			.as("End of weekend day")
			.returns(true, from(x -> x.applies(sat(23, 59))))
			.as("Weekday")
			.returns(false, from(x -> x.applies(tue(12, 0))))
			;
		then(always)
			.as("Not empty")
			.returns(false, from(AvailabilitySchedule::isEmpty))
			.as("Always applies")
			.returns(true, from(x -> x.applies(tue(12, 0))))
			;
		// @formatter:on
	}

	@Test
	public void parse_daysAndMonths() {
		// WHEN
		AvailabilitySchedule s = parse(",1-15,Mon-Fri,07:00-18:00,1");
		AvailabilitySchedule summer = parse("Jun-Aug,,Mon-Fri,07:00-18:00,1");
		AvailabilitySchedule xmas = parse("Dec,24-26,,,1");
		AvailabilitySchedule leap = parse("Feb,29,,,1");
		AvailabilitySchedule full = parse("January-March,,Monday-Friday,7-18,1");

		// THEN
		// @formatter:off
		then(s)
			.as("6th of the month")
			.returns(true, from(x -> x.applies(tue(12, 0))))
			.as("20th of the month (a Tuesday)")
			.returns(false, from(x -> x.applies(LocalDateTime.of(2026, 10, 20, 12, 0))))
			.as("10th but Saturday")
			.returns(false, from(x -> x.applies(sat(12, 0))))
			;
		then(summer)
			.as("October")
			.returns(false, from(x -> x.applies(tue(12, 0))))
			// 2026-07-07 is a Tuesday
			.as("July weekday")
			.returns(true, from(x -> x.applies(LocalDateTime.of(2026, 7, 7, 12, 0))))
			// 2026-07-11 is a Saturday
			.as("July weekend")
			.returns(false, from(x -> x.applies(LocalDateTime.of(2026, 7, 11, 12, 0))))
			;
		then(xmas)
			.as("Start of first day")
			.returns(true, from(x -> x.applies(LocalDateTime.of(2026, 12, 24, 0, 0))))
			.as("End of last day")
			.returns(true, from(x -> x.applies(LocalDateTime.of(2026, 12, 26, 23, 59))))
			.as("Day after")
			.returns(false, from(x -> x.applies(LocalDateTime.of(2026, 12, 27, 0, 0))))
			.as("Other month")
			.returns(false, from(x -> x.applies(LocalDateTime.of(2026, 11, 25, 12, 0))))
			;
		then(parse("12,24-26,,,1"))
			.as("Numeric month same as named")
			.hasToString(xmas.toString())
			;
		then(leap)
			.as("Leap day")
			.returns(true, from(x -> x.applies(LocalDateTime.of(2028, 2, 29, 12, 0))))
			.as("Non-leap year end of February")
			.returns(false, from(x -> x.applies(LocalDateTime.of(2027, 2, 28, 12, 0))))
			;
		then(full)
			.as("Full names: February Tuesday")
			.returns(true, from(x -> x.applies(LocalDateTime.of(2026, 2, 3, 12, 0))))
			.as("Full names: October")
			.returns(false, from(x -> x.applies(tue(12, 0))))
			;
		// @formatter:on
	}

	@Test
	public void parse_blackoutRow() {
		// WHEN
		AvailabilitySchedule s = parse("Dec,25,,,0\n,,Mon-Fri,07:00-18:00,1");
		AvailabilitySchedule later = parse(",,Mon-Fri,07:00-18:00,1\nDec,25,,,0");

		// THEN
		// 2026-12-24 is a Thursday, 2026-12-25 a Friday
		// @formatter:off
		then(s)
			.as("Day before blackout")
			.returns(true, from(x -> x.applies(LocalDateTime.of(2026, 12, 24, 12, 0))))
			.as("Blackout first match wins")
			.returns(false, from(x -> x.applies(LocalDateTime.of(2026, 12, 25, 12, 0))))
			.as("Next start skips the blackout")
			.returns(LocalDateTime.of(2026, 12, 28, 7, 0),
					from(x -> x.nextBoundary(LocalDateTime.of(2026, 12, 24, 20, 0))))
			;
		then(later)
			.as("A later blackout never matches")
			.returns(true, from(x -> x.applies(LocalDateTime.of(2026, 12, 25, 12, 0))))
			;
		// @formatter:on
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

		// @formatter:off
		then(parse(",,Mon-Fri,07:00-18:00,1.0"))
			.as("Decimal one is available")
			.returns(true, from(x -> x.applies(tue(12, 0))))
			;
		// @formatter:on
	}

	@Test
	public void nextBoundary_skipsDstGap() {
		// GIVEN
		// NZ clocks go from 02:00 NZST to 03:00 NZDT on Sunday 2026-09-27, so
		// 02:00 that day never happens and the window first applies the next Sunday
		AvailabilitySchedule s = parse(",,Sun,02:00-03:00,1");
		LocalDateTime start = LocalDateTime.of(2026, 9, 27, 1, 0);

		// THEN
		// @formatter:off
		then(s)
			.as("Zone-unaware returns the gap time")
			.returns(LocalDateTime.of(2026, 9, 27, 2, 0), from(x -> x.nextBoundary(start)))
			.as("Zone-aware skips the gap")
			.returns(LocalDateTime.of(2026, 10, 4, 2, 0),
					from(x -> x.nextBoundary(start, ZoneId.of("Pacific/Auckland"))))
			;
		// @formatter:on
	}

	@Test
	public void nextBoundary() {
		// GIVEN
		AvailabilitySchedule s = parse(",,Mon-Fri,07:00-18:00,1");

		// THEN
		// @formatter:off
		then(s)
			.as("Inside: end of window")
			.returns(tue(18, 0), from(x -> x.nextBoundary(tue(12, 0))))
			.as("Outside: next start")
			.returns(LocalDateTime.of(2026, 10, 7, 7, 0), from(x -> x.nextBoundary(tue(18, 0))))
			.as("Before start same day")
			.returns(tue(7, 0), from(x -> x.nextBoundary(tue(6, 30))))
			.as("Weekend wraps to Monday")
			.returns(LocalDateTime.of(2026, 10, 12, 7, 0), from(x -> x.nextBoundary(sat(12, 0))))
			;
		then(AvailabilitySchedule.parse("").nextBoundary(tue(12, 0)))
			.as("Empty schedule")
			.isNull()
			;
		then(parse(",,,,1").nextBoundary(tue(12, 0)))
			.as("Always applies")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void nextBoundary_multiRange() {
		// GIVEN
		AvailabilitySchedule s = parse(",,Mon-Fri,\"06:00-09:00,16:00-20:00\",1");

		// THEN
		// @formatter:off
		then(s)
			.as("Inner end")
			.returns(tue(9, 0), from(x -> x.nextBoundary(tue(8, 0))))
			.as("Next inner start")
			.returns(tue(16, 0), from(x -> x.nextBoundary(tue(9, 0))))
			;
		// @formatter:on
	}

	@Test
	public void nextBoundary_fullDays() {
		// GIVEN
		AvailabilitySchedule s = parse("Dec,24-26,,,1");

		// THEN
		// @formatter:off
		then(s)
			.as("Before: start of first day")
			.returns(LocalDateTime.of(2026, 12, 24, 0, 0),
					from(x -> x.nextBoundary(LocalDateTime.of(2026, 12, 20, 12, 0))))
			.as("Inside: start of day after last day")
			.returns(LocalDateTime.of(2026, 12, 27, 0, 0),
					from(x -> x.nextBoundary(LocalDateTime.of(2026, 12, 25, 12, 0))))
			;
		// @formatter:on
	}

	@Test
	public void toString_canonical() {
		// WHEN
		AvailabilitySchedule s = parse(
				",,mon-fri,07:00-18:00,1\nJun-Aug,1-15,,\"06:00-09:00,16:00-20:00\",1\nDec,24-26,,,1");

		// THEN
		// @formatter:off
		then(s)
			.as("Canonical form")
			.hasToString(",,Mon-Fri,7-18,1; Jun-Aug,1-15,,\"6-9,16-20\",1; Dec,24-26,,,1")
			;
		then(AvailabilitySchedule.parse(s.toCsv()))
			.as("Round trip")
			.hasToString(s.toString())
			;
		// @formatter:on
	}

	private static final List<Object> HEADER_ROW = List.of("Month", "Day", "Weekday", "Time",
			"Available");

	@Test
	public void fromMetadata_string() {
		// WHEN
		AvailabilitySchedule s = AvailabilitySchedule.fromMetadata(HEADER + ",,Mon-Fri,07:00-18:00,1");

		// THEN
		// @formatter:off
		then(s)
			.as("Weekday window applies")
			.returns(true, from(x -> x.applies(tue(12, 0))))
			.as("Weekend")
			.returns(false, from(x -> x.applies(sat(12, 0))))
			;
		// @formatter:on
	}

	@Test
	public void fromMetadata_rows() {
		// WHEN
		AvailabilitySchedule s = AvailabilitySchedule
				.fromMetadata(List.of(HEADER_ROW, Arrays.asList(null, null, "Sat,Sun", "08:00-12:00", 1),
						Arrays.asList("", "", "Mon-Fri", "07:00-09:00", "1")));

		// THEN
		// @formatter:off
		then(s)
			.as("Comma cell needs no quoting")
			.returns(true, from(x -> x.applies(sat(9, 0))))
			.as("Numeric cell")
			.returns(false, from(x -> x.applies(sat(13, 0))))
			.as("Same as the string form")
			.hasToString(AvailabilitySchedule
					.parse(HEADER + ",,\"Sat,Sun\",08:00-12:00,1\n,,Mon-Fri,07:00-09:00,1").toString())
			;
		then(s.getRules())
			.as("Two rules parsed")
			.hasSize(2)
			;
		// @formatter:on
	}

	@Test
	public void fromMetadata_rows_validatedLikeString() {
		// @formatter:off
		thenIllegalArgumentException()
			.as("Descending range should be rejected")
			.isThrownBy(() -> AvailabilitySchedule.fromMetadata(
					List.of(HEADER_ROW, Arrays.asList(null, null, null, "22:00-06:00", 1))))
			.withMessageContaining("is descending")
			;
		thenIllegalArgumentException()
			.as("Missing header should be rejected")
			.isThrownBy(() -> AvailabilitySchedule.fromMetadata(
					List.of(Arrays.asList(null, null, "Mon-Fri", "7-18", 1))))
			.withMessageContaining("header row")
			;
		// @formatter:on
	}

	@Test
	public void fromMetadata_empty() {
		// @formatter:off
		then(AvailabilitySchedule.fromMetadata(""))
			.as("Empty string is empty")
			.returns(true, from(AvailabilitySchedule::isEmpty))
			;
		then(AvailabilitySchedule.fromMetadata(List.of()))
			.as("Empty list is empty")
			.returns(true, from(AvailabilitySchedule::isEmpty))
			;
		then(AvailabilitySchedule.fromMetadata(List.of(HEADER_ROW)))
			.as("Header row only is empty")
			.returns(true, from(AvailabilitySchedule::isEmpty))
			;
		// @formatter:on
	}

	@Test
	public void fromMetadata_unsupported() {
		for ( Object value : new Object[] { List.of(AvailabilitySchedule.EXAMPLE_HEADER, ",,,,1"),
				List.of(HEADER_ROW, List.of(Map.of("a", 1), "", "", "", "1")), Map.of("a", 1), 42 } ) {
			// @formatter:off
			thenIllegalArgumentException()
				.as("Expected [%s] to be rejected", value)
				.isThrownBy(() -> AvailabilitySchedule.fromMetadata(value))
				;
			// @formatter:on
		}
	}

}

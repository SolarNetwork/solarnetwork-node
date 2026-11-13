/* ==================================================================
 * TemporalFormatterTests.java - 13 Nov 2026 4:14:30 pm
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

import static net.solarnetwork.node.setup.web.support.TemporalFormatter.SOLARNET_INSTANT_FORMATTER;
import static net.solarnetwork.node.setup.web.support.TemporalFormatter.SOLARNET_LOCAL_DATE_TIME_FORMATTER;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.junit.Test;
import net.solarnetwork.node.setup.web.support.TemporalFormatter;

/**
 * Test cases for the {@link TemporalFormatter} class.
 *
 * @author matt
 * @version 1.0
 */
public class TemporalFormatterTests {

	private static Instant instant(String text) {
		return SOLARNET_INSTANT_FORMATTER.parse(text, Locale.ENGLISH);
	}

	private static LocalDateTime localDateTime(String text) {
		return SOLARNET_LOCAL_DATE_TIME_FORMATTER.parse(text, Locale.ENGLISH);
	}

	@Test
	public void instant_date() {
		then(instant("2026-11-13")).as("Date only is midnight UTC")
				.isEqualTo(Instant.parse("2026-11-13T00:00:00Z"));
	}

	@Test
	public void instant_dateTime() {
		then(instant("2026-11-13T12:34")).as("Date and time without zone is UTC")
				.isEqualTo(Instant.parse("2026-11-13T12:34:00Z"));
	}

	@Test
	public void instant_dateTime_millis() {
		then(instant("2026-11-13T12:34:56.789")).as("Milliseconds parsed")
				.isEqualTo(Instant.parse("2026-11-13T12:34:56.789Z"));
	}

	@Test
	public void instant_dateTime_space() {
		then(instant("2026-11-13 12:34:56")).as("Space date and time delimiter parsed")
				.isEqualTo(Instant.parse("2026-11-13T12:34:56Z"));
	}

	@Test
	public void instant_rfc3339_utc() {
		then(instant("2026-11-13T12:34:56Z")).as("RFC 3339 UTC date parsed, as Grafana sends")
				.isEqualTo(Instant.parse("2026-11-13T12:34:56Z"));
	}

	@Test
	public void instant_rfc3339_offset() {
		then(instant("2026-11-14T01:34:56+13:00")).as("RFC 3339 date with offset parsed")
				.isEqualTo(Instant.parse("2026-11-13T12:34:56Z"));
	}

	@Test
	public void instant_invalid() {
		thenThrownBy(() -> instant("not a date")).as("Invalid date throws parse exception")
				.isInstanceOf(DateTimeParseException.class);
	}

	@Test
	public void instant_print() {
		then(SOLARNET_INSTANT_FORMATTER.print(Instant.parse("2026-11-13T12:34:56.789Z"),
				Locale.ENGLISH)).as("Printed with first formatter").isEqualTo("2026-11-13T12:34:56.789");
	}

	@Test
	public void localDateTime_dateTime() {
		then(localDateTime("2026-11-13T12:34")).as("Local date and time parsed")
				.isEqualTo(LocalDateTime.of(2026, 11, 13, 12, 34));
	}

	@Test
	public void localDateTime_space() {
		then(localDateTime("2026-11-13 12:34:56.789")).as("Space date and time delimiter parsed")
				.isEqualTo(LocalDateTime.of(2026, 11, 13, 12, 34, 56, 789_000_000));
	}

	@Test
	public void localDateTime_date() {
		then(localDateTime("2026-11-13")).as("Date only is midnight")
				.isEqualTo(LocalDateTime.of(2026, 11, 13, 0, 0));
	}

	@Test
	public void noFormatters() {
		thenThrownBy(() -> new TemporalFormatter<>(Instant::from, new DateTimeFormatter[0]))
				.as("At least one formatter required").isInstanceOf(IllegalArgumentException.class);
	}

}

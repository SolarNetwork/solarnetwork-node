/* ==================================================================
 * AvailabilitySchedule.java - 14/10/2026 9:00:00 AM
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

package net.solarnetwork.node.setup.bluetooth;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.tariff.ChronoFieldsTariff;
import net.solarnetwork.domain.tariff.CsvTemporalRangeTariffParser;
import net.solarnetwork.domain.tariff.SimpleTariffRate;
import net.solarnetwork.domain.tariff.SimpleTemporalTariffSchedule;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.domain.tariff.Tariff.Rate;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.domain.tariff.TariffUtils;
import net.solarnetwork.domain.tariff.TemporalRangeSetsTariff;
import net.solarnetwork.domain.tariff.TemporalTariffEvaluator;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeContainer;

/**
 * A schedule of availability windows, in the tariff schedule CSV format.
 *
 * <pre>
 * Month,Day,Weekday,Time,Available
 * ,,Mon-Fri,07:00-18:00,1
 * Dec,24-26,,,1
 * </pre>
 *
 * <p>
 * The first row is <b>always</b> the header row, even if it looks like a rule.
 * A schedule without a header therefore loses its first rule, which becomes the
 * header. The rule table in the status shows the rules actually parsed, so
 * compare it with the rules you expect.
 * </p>
 *
 * <p>
 * The first non-empty rate of each rule, normally the {@code Available} column,
 * decides the rule and must be {@code 0} or {@code 1}: if {@code Available} is
 * empty a later rate column decides. The first rule that matches a time decides
 * it. Time ranges exclude their end. Descending ranges such as
 * {@code 22:00-06:00} are rejected rather than reordered.
 * </p>
 *
 * @author elijah
 * @version 1.0
 */
public final class AvailabilitySchedule {

	/** An example header row. */
	public static final String EXAMPLE_HEADER = "Month,Day,Weekday,Time,Available";

	/** The evaluator used to match rules to dates. */
	public static final TemporalTariffEvaluator EVALUATOR = SimpleTemporalTariffSchedule.DEFAULT_EVALUATOR;

	/** The number of minutes in a day. */
	private static final int MINUTES_PER_DAY = 24 * 60;

	/** The maximum number of days to scan for the next boundary. */
	private static final int MAX_BOUNDARY_SCAN_DAYS = 366;

	/** The rate description of the deciding value, for {@link #toCsv()}. */
	private static final String AVAILABLE = "Available";

	private static final AvailabilitySchedule EMPTY = new AvailabilitySchedule(
			new SimpleTemporalTariffSchedule(List.of(), EVALUATOR), List.of(), List.of());

	private final TariffSchedule schedule;
	private final List<ChronoFieldsTariff> rules;
	private final List<ChronoFieldsTariff> csvRules;
	private final int[] edges;

	private AvailabilitySchedule(TariffSchedule schedule, List<ChronoFieldsTariff> rules,
			List<ChronoFieldsTariff> csvRules) {
		super();
		this.schedule = schedule;
		this.rules = Collections.unmodifiableList(rules);
		this.csvRules = csvRules;

		// every minute of the day a rule's time range starts or ends; the start
		// and end of each day are edges too, in case the date columns change
		TreeSet<Integer> e = new TreeSet<>();
		e.add(0);
		e.add(MINUTES_PER_DAY);
		for ( ChronoFieldsTariff rule : rules ) {
			IntRangeContainer times = rule.rangeForChronoField(ChronoField.MINUTE_OF_DAY);
			if ( times == null ) {
				continue;
			}
			for ( IntRange r : times.ranges() ) {
				e.add(r.getMin());
				e.add(r.getMax());
			}
		}
		this.edges = e.stream().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * Get an empty schedule.
	 *
	 * @return the empty schedule
	 */
	public static AvailabilitySchedule empty() {
		return EMPTY;
	}

	/**
	 * Parse a schedule from a metadata value.
	 *
	 * <p>
	 * The value can be the schedule CSV as a string, or the rows as a
	 * {@code List} of {@code List} (like a JSON array of arrays) or a
	 * {@code String[][]}. The first row is always the header row: see the class
	 * description.
	 * </p>
	 *
	 * <p>
	 * In the row forms, values are trimmed, rows that are not arrays or have
	 * fewer than 5 values are ignored, as are values in columns past the header
	 * row's last column, and there are no comment rows. In the CSV form a row
	 * with fewer than 5 values is rejected.
	 * </p>
	 *
	 * @param value
	 *        the metadata value
	 * @return the schedule, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if the value is not a supported type, its header row has fewer
	 *         than 5 values, or the schedule cannot be parsed
	 * @see #parse(Object)
	 */
	public static AvailabilitySchedule fromMetadata(@Nullable Object value) {
		if ( value == null ) {
			throw new IllegalArgumentException(unsupportedMessage("null"));
		}
		return parse(value);
	}

	private static String unsupportedMessage(String type) {
		return "Unsupported schedule metadata " + type
				+ ": use a CSV string, or an array of rows starting with a header row"
				+ " of at least 5 values like " + EXAMPLE_HEADER;
	}

	/**
	 * Parse a schedule.
	 *
	 * <p>
	 * The first row is always the header row: see the class description.
	 * </p>
	 *
	 * @param text
	 *        the schedule CSV text; {@literal null} or blank produces an empty
	 *        schedule
	 * @return the schedule, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if the schedule cannot be parsed
	 * @see TariffUtils#parseCsvTemporalRangeSchedule(Locale, boolean, boolean,
	 *      boolean, TemporalTariffEvaluator, Object)
	 */
	public static AvailabilitySchedule parse(@Nullable Object data) {
		if ( data == null || (data instanceof String s && s.isBlank())) {
			return EMPTY;
		}
		final TariffSchedule sched;
		try {
			sched = TariffUtils.parseCsvTemporalRangeSchedule(Locale.ENGLISH, false, true, true,
					EVALUATOR, data);
		} catch ( IOException e ) {
			// not expected from in-memory data
			throw new IllegalArgumentException(e.getMessage(), e);
		} catch ( NumberFormatException e ) {
			// the row form does not wrap a bad number like the CSV parser does
			throw new IllegalArgumentException("Invalid schedule availability value: " + e.getMessage(),
					e);
		} catch ( DateTimeException e ) {
			// the row form does not wrap a bad range like the CSV parser does
			throw new IllegalArgumentException("Invalid schedule: " + e.getMessage(), e);
		}
		if ( sched == null ) {
			throw new IllegalArgumentException(unsupportedMessage(data.getClass().getSimpleName()));
		}
		final List<ChronoFieldsTariff> rules = new ArrayList<>(sched.rules().size());
		final List<ChronoFieldsTariff> csvRules = new ArrayList<>(sched.rules().size());
		int row = 0;
		for ( Tariff t : sched.rules() ) {
			row++;
			if ( !(t instanceof TemporalRangeSetsTariff rule) ) {
				// not expected from TariffUtils
				throw new IllegalArgumentException(
						String.format("Schedule rule %d is not a supported rule type", row));
			}
			final Rate rate = rule.rate();
			if ( rate == null ) {
				throw new IllegalArgumentException(
						String.format("Schedule rule %d has no availability value: use 0 or 1", row));
			}
			final BigDecimal value = rate.getAmount();
			if ( value.signum() != 0 && value.compareTo(BigDecimal.ONE) != 0 ) {
				throw new IllegalArgumentException(
						String.format("Schedule rule %d availability value [%s] must be 0 or 1", row,
								value.toPlainString()));
			}
			rules.add(rule);
			// only the deciding value, so the CSV parses back to the same schedule
			csvRules.add(new TemporalRangeSetsTariff(rule.getMonthRanges(), rule.getDayOfMonthRanges(),
					rule.getDayOfWeekRanges(), rule.getMinuteOfDayRanges(),
					List.of(new SimpleTariffRate(AVAILABLE, value))));
		}
		return (rules.isEmpty() ? EMPTY : new AvailabilitySchedule(sched, rules, csvRules));
	}

	/**
	 * Get the tariff schedule.
	 *
	 * <p>
	 * The schedule resolves only the first matching rule. Use
	 * {@link #EVALUATOR} to evaluate its rules.
	 * </p>
	 *
	 * @return the schedule, never {@literal null}
	 */
	public TariffSchedule getSchedule() {
		return schedule;
	}

	/**
	 * Get the schedule rules.
	 *
	 * @return the rules, in schedule order, never {@literal null}
	 */
	public List<ChronoFieldsTariff> getRules() {
		return rules;
	}

	/**
	 * Test if this schedule has no rules.
	 *
	 * @return {@literal true} if there are no rules
	 */
	public boolean isEmpty() {
		return rules.isEmpty();
	}

	/**
	 * Test if the schedule makes the radio available at a date and time.
	 *
	 * @param dateTime
	 *        the date and time
	 * @return {@literal true} if the first rule that matches has a first
	 *         availability value of {@code 1}
	 */
	public boolean applies(LocalDateTime dateTime) {
		if ( rules.isEmpty() ) {
			return false;
		}
		// parsing guarantees each rule's first rate is 0 or 1
		Tariff t = schedule.resolveTariff(dateTime);
		BigDecimal value = (t != null ? t.amount() : null);
		return (value != null && value.signum() > 0);
	}

	/**
	 * Find the next time the schedule changes between applying and not
	 * applying.
	 *
	 * <p>
	 * If the schedule applies at {@code from} the result is the end of the
	 * current availability; otherwise it is the start of the next availability.
	 * At most about a year is searched.
	 * </p>
	 *
	 * @param from
	 *        the date and time to search from
	 * @return the next boundary, or {@literal null} if there is none within a
	 *         year
	 */
	public @Nullable LocalDateTime nextBoundary(LocalDateTime from) {
		return nextBoundary(from, null);
	}

	/**
	 * Find the next time the schedule changes between applying and not
	 * applying, in a time zone.
	 *
	 * <p>
	 * Wall-clock times that do not exist in the zone (inside a daylight saving
	 * gap) are never returned, because the schedule can never apply at them.
	 * </p>
	 *
	 * @param from
	 *        the date and time to search from
	 * @param zone
	 *        the zone the schedule is evaluated in, or {@literal null} to
	 *        ignore daylight saving gaps
	 * @return the next boundary, or {@literal null} if there is none within a
	 *         year
	 * @see #nextBoundary(LocalDateTime)
	 */
	public @Nullable LocalDateTime nextBoundary(LocalDateTime from, @Nullable ZoneId zone) {
		if ( rules.isEmpty() ) {
			return null;
		}
		final boolean current = applies(from);
		LocalDate date = from.toLocalDate();
		int startMinute = from.toLocalTime().toSecondOfDay() / 60 + 1;
		for ( int day = 0; day < MAX_BOUNDARY_SCAN_DAYS; day++ ) {
			for ( int edge : edges ) {
				if ( edge < startMinute ) {
					continue;
				}
				LocalDateTime candidate = (edge == MINUTES_PER_DAY ? date.plusDays(1).atStartOfDay()
						: date.atTime(LocalTime.ofSecondOfDay(edge * 60L)));
				if ( zone != null && !candidate.atZone(zone).toLocalDateTime().equals(candidate) ) {
					// inside a daylight saving gap: this wall-clock time never happens
					continue;
				}
				if ( applies(candidate) != current ) {
					return candidate;
				}
			}
			date = date.plusDays(1);
			startMinute = 0;
		}
		return null;
	}

	/**
	 * Get the schedule as CSV, with a header row.
	 *
	 * <p>
	 * Each rule is written with only its deciding value, under a single
	 * {@code Available} column, so the CSV parses back to the same schedule.
	 * </p>
	 *
	 * @return the CSV, or an empty string if the schedule is empty
	 */
	public String toCsv() {
		if ( rules.isEmpty() ) {
			return "";
		}
		StringWriter out = new StringWriter();
		try {
			new CsvTemporalRangeTariffParser(Locale.ENGLISH).formatCsv(csvRules, out);
		} catch ( IOException e ) {
			// not expected from a StringWriter
			throw new IllegalStateException(e);
		}
		return out.toString();
	}

	/**
	 * Get the schedule rows on one line, without the header, separated by
	 * {@code "; "}.
	 *
	 * @return the rows
	 */
	@Override
	public String toString() {
		String[] lines = toCsv().split("\\R");
		StringBuilder buf = new StringBuilder();
		for ( int i = 1; i < lines.length; i++ ) {
			if ( lines[i].isEmpty() ) {
				continue;
			}
			if ( buf.length() > 0 ) {
				buf.append("; ");
			}
			buf.append(lines[i]);
		}
		return buf.toString();
	}

}

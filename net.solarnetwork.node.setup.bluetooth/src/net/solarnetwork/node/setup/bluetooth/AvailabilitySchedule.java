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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.util.DateUtils;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;

/**
 * A schedule of availability windows.
 *
 * <p>
 * A schedule is a list of windows, one per line (or separated by {@code ;}).
 * Blank lines are ignored and {@code #} starts a comment that runs to the end
 * of the line. Each window has up to four whitespace-separated columns, in this
 * order:
 * </p>
 *
 * <ol>
 * <li>month ranges, e.g. {@code Jan-Mar} or {@code 1-3}</li>
 * <li>day-of-month ranges, e.g. {@code 1-15}</li>
 * <li>weekday ranges, e.g. {@code Mon-Fri} or {@code 1-5} (1 = Monday)</li>
 * <li>time-of-day ranges, e.g. {@code 07:00-18:00} or {@code 7-18}</li>
 * </ol>
 *
 * <p>
 * Leading columns may be omitted, so {@code Mon-Fri 07:00-18:00} is a weekday
 * and time window and {@code 07:00-18:00} alone applies every day. A column of
 * {@code *} matches every value. Each column is a comma-separated set of
 * inclusive ranges, except that time ranges are inclusive-exclusive: the window
 * {@code 07:00-18:00} applies from 07:00:00 up to but not including 18:00:00,
 * and {@code 24:00} means the end of the day.
 * </p>
 *
 * <p>
 * Ranges must be ascending. A descending range such as {@code 22:00-06:00} is
 * rejected rather than silently reordered, so an overnight window must be
 * written as two windows. Names are parsed in English first, then in the
 * default locale.
 * </p>
 *
 * @author elijah
 * @version 1.0
 */
public final class AvailabilitySchedule {

	/** The line delimiter pattern. */
	public static final Pattern LINE_DELIMITER = Pattern.compile("[\\r\\n]+");

	/** The window delimiter pattern within a line. */
	public static final Pattern WINDOW_DELIMITER = Pattern.compile("\\s*;\\s*");

	/** The comment character; it runs to the end of the line. */
	public static final char COMMENT = '#';

	/** The column delimiter pattern. */
	public static final Pattern COLUMN_DELIMITER = Pattern.compile("\\s+");

	/** The range set delimiter pattern. */
	public static final Pattern SET_DELIMITER = Pattern.compile("\\s*,\\s*");

	/** The wildcard column value. */
	public static final String WILDCARD = "*";

	/** The number of minutes in a day. */
	private static final int MINUTES_PER_DAY = 24 * 60;

	/** The maximum number of days to scan for the next boundary. */
	private static final int MAX_BOUNDARY_SCAN_DAYS = 366;

	private static final AvailabilitySchedule EMPTY = new AvailabilitySchedule(Collections.emptyList());

	private final List<Window> windows;

	private AvailabilitySchedule(List<Window> windows) {
		super();
		this.windows = Collections.unmodifiableList(windows);
	}

	/**
	 * An availability window.
	 *
	 * <p>
	 * A {@literal null} column matches every value.
	 * </p>
	 *
	 * @param months
	 *        the month-of-year set, or {@literal null} for any
	 * @param days
	 *        the day-of-month set, or {@literal null} for any
	 * @param weekdays
	 *        the day-of-week set, or {@literal null} for any
	 * @param minutes
	 *        the minute-of-day ranges, each inclusive-exclusive, or
	 *        {@literal null} for any
	 */
	public record Window(@Nullable IntRangeSet months, @Nullable IntRangeSet days,
			@Nullable IntRangeSet weekdays, @Nullable List<IntRange> minutes) {

		/**
		 * Test if this window applies to a date.
		 *
		 * @param date
		 *        the date
		 * @return {@literal true} if the month, day and weekday columns all
		 *         match
		 */
		public boolean appliesToDate(LocalDate date) {
			return (months == null || months.contains(date.getMonthValue()))
					&& (days == null || days.contains(date.getDayOfMonth()))
					&& (weekdays == null || weekdays.contains(date.getDayOfWeek().getValue()));
		}

		/**
		 * Test if this window applies to a minute of the day.
		 *
		 * @param minuteOfDay
		 *        the minute of the day
		 * @return {@literal true} if any time range contains the minute, with
		 *         an exclusive end
		 */
		public boolean appliesToMinute(int minuteOfDay) {
			if ( minutes == null ) {
				return true;
			}
			for ( IntRange r : minutes ) {
				if ( r.getMin() <= minuteOfDay && minuteOfDay < r.getMax() ) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Test if this window applies at a date and time.
		 *
		 * @param dateTime
		 *        the date and time
		 * @return {@literal true} if the window applies
		 */
		public boolean applies(LocalDateTime dateTime) {
			return appliesToDate(dateTime.toLocalDate())
					&& appliesToMinute(dateTime.toLocalTime().toSecondOfDay() / 60);
		}

		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append(formatSet(ChronoField.MONTH_OF_YEAR, months)).append(' ');
			buf.append(formatSet(ChronoField.DAY_OF_MONTH, days)).append(' ');
			buf.append(formatSet(ChronoField.DAY_OF_WEEK, weekdays)).append(' ');
			if ( minutes == null ) {
				buf.append(WILDCARD);
			} else {
				boolean first = true;
				for ( IntRange r : minutes ) {
					if ( !first ) {
						buf.append(',');
					}
					first = false;
					buf.append(formatMinute(r.getMin())).append('-').append(formatMinute(r.getMax()));
				}
			}
			return buf.toString();
		}

		private static String formatSet(ChronoField field, @Nullable IntRangeSet set) {
			if ( set == null ) {
				return WILDCARD;
			}
			TextStyle style = (field == ChronoField.DAY_OF_MONTH ? TextStyle.NARROW : TextStyle.SHORT);
			if ( field == ChronoField.DAY_OF_MONTH ) {
				// no text names for days; format as numbers
				StringBuilder buf = new StringBuilder();
				for ( IntRange r : set.ranges() ) {
					if ( buf.length() > 0 ) {
						buf.append(',');
					}
					buf.append(r.getMin());
					if ( r.getMax() != r.getMin() ) {
						buf.append('-').append(r.getMax());
					}
				}
				return buf.toString();
			}
			String s = DateUtils.formatRange(field, set, Locale.ENGLISH, style);
			return (s != null ? s : WILDCARD);
		}

		private static String formatMinute(int minuteOfDay) {
			int h = minuteOfDay / 60;
			int m = minuteOfDay - h * 60;
			return String.format("%02d:%02d", h, m);
		}
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
	 * Parse a schedule.
	 *
	 * @param text
	 *        the schedule text; {@literal null} or blank produces an empty
	 *        schedule
	 * @return the schedule, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if any window cannot be parsed; the message names the window
	 */
	public static AvailabilitySchedule parse(@Nullable String text) {
		if ( text == null || text.isBlank() ) {
			return EMPTY;
		}
		List<Window> windows = new ArrayList<>(4);
		int index = 0;
		for ( String physicalLine : LINE_DELIMITER.split(text) ) {
			// strip the comment first, so a ';' inside a comment is not a separator
			int hash = physicalLine.indexOf(COMMENT);
			if ( hash >= 0 ) {
				physicalLine = physicalLine.substring(0, hash);
			}
			for ( String line : WINDOW_DELIMITER.split(physicalLine) ) {
				line = line.trim();
				if ( line.isEmpty() ) {
					continue;
				}
				index++;
				try {
					windows.add(parseWindow(line));
				} catch ( DateTimeException | IllegalArgumentException e ) {
					throw new IllegalArgumentException(
							String.format("Window %d [%s] is invalid: %s", index, line, e.getMessage()),
							e);
				}
			}
		}
		return (windows.isEmpty() ? EMPTY : new AvailabilitySchedule(windows));
	}

	private static Window parseWindow(String line) {
		String[] cols = COLUMN_DELIMITER.split(line);
		if ( cols.length > 4 ) {
			throw new IllegalArgumentException("more than 4 columns");
		}
		// right-align: missing leading columns are wildcards
		String[] c = new String[] { WILDCARD, WILDCARD, WILDCARD, WILDCARD };
		System.arraycopy(cols, 0, c, 4 - cols.length, cols.length);
		IntRangeSet months = parseSet(ChronoField.MONTH_OF_YEAR, c[0], "month");
		IntRangeSet days = parseSet(ChronoField.DAY_OF_MONTH, c[1], "day");
		IntRangeSet weekdays = parseSet(ChronoField.DAY_OF_WEEK, c[2], "weekday");
		List<IntRange> minutes = parseMinutes(c[3]);
		return new Window(months, days, weekdays, minutes);
	}

	private static @Nullable IntRangeSet parseSet(ChronoField field, String col, String name) {
		if ( WILDCARD.equals(col) ) {
			return null;
		}
		IntRangeSet set = new IntRangeSet();
		for ( String part : SET_DELIMITER.split(col) ) {
			if ( part.isEmpty() ) {
				throw new IllegalArgumentException("empty " + name + " range");
			}
			String[] ends = splitRange(part);
			int min = parseValue(field, ends[0]);
			int max = parseValue(field, ends[1]);
			if ( min > max ) {
				throw new IllegalArgumentException(
						String.format("%s range [%s] is descending", name, part));
			}
			set.addRange(min, max);
		}
		return set;
	}

	private static int parseValue(ChronoField field, String value) {
		// lenient text parsing would also accept one- and two-letter "narrow" names,
		// which are ambiguous (S = Sat or Sun, J = Jan, Jun or Jul); insist on a
		// number or at least three letters
		if ( value.length() < 3 && !value.chars().allMatch(Character::isDigit) ) {
			throw new IllegalArgumentException(
					"invalid value [" + value + "]: use a number or at least three letters");
		}
		IntRange r;
		try {
			r = DateUtils.parseRange(field, value, Locale.ENGLISH);
		} catch ( DateTimeException e ) {
			Locale def = Locale.getDefault();
			if ( Locale.ENGLISH.equals(def) || Locale.ENGLISH.getLanguage().equals(def.getLanguage()) ) {
				throw e;
			}
			r = DateUtils.parseRange(field, value, def);
		}
		if ( r == null || r.getMin() != r.getMax() ) {
			// a two-ended result means the value itself contained a range delimiter
			throw new IllegalArgumentException("invalid value [" + value + "]");
		}
		return field.range().checkValidIntValue(r.getMin(), field);
	}

	private static @Nullable List<IntRange> parseMinutes(String col) {
		if ( WILDCARD.equals(col) ) {
			return null;
		}
		List<IntRange> result = new ArrayList<>(2);
		for ( String part : SET_DELIMITER.split(col) ) {
			if ( part.isEmpty() ) {
				throw new IllegalArgumentException("empty time range");
			}
			String[] ends = splitRange(part);
			if ( ends[0] == ends[1] ) {
				throw new IllegalArgumentException(
						String.format("time [%s] must be a range such as %s-%s", part, part,
								part.contains(":") ? part : part + ":59"));
			}
			int min = parseMinute(ends[0]);
			int max = parseMinute(ends[1]);
			if ( min >= max ) {
				throw new IllegalArgumentException(String
						.format("time range [%s] must be ascending and at least one minute long", part));
			}
			result.add(IntRange.rangeOf(min, max));
		}
		return result;
	}

	private static int parseMinute(String value) {
		// fix24 = false so 24:00 becomes 1440, the exclusive end of the day
		IntRange r = DateUtils.parseMinuteOfDayRange(value, Locale.ENGLISH, false);
		if ( r == null || r.getMin() != r.getMax() ) {
			// a two-ended result means the value itself contained a range delimiter
			throw new IllegalArgumentException("invalid time [" + value + "]");
		}
		int m = r.getMin();
		if ( m < 0 || m > MINUTES_PER_DAY ) {
			throw new IllegalArgumentException("time [" + value + "] is out of range");
		}
		return m;
	}

	/**
	 * Split a range into its two ends.
	 *
	 * <p>
	 * A single value is returned as both ends, like {@code DateUtils} does, but
	 * the ends are returned separately so the caller can reject a descending
	 * range instead of letting {@link IntRange} reorder it.
	 * </p>
	 */
	private static String[] splitRange(String range) {
		Matcher m = DateUtils.RANGE_DELIMITER.matcher(range);
		if ( m.find() && m.start() > 0 && m.end() < range.length() ) {
			return new String[] { range.substring(0, m.start()), range.substring(m.end()) };
		}
		return new String[] { range, range };
	}

	/**
	 * Get the windows.
	 *
	 * @return the windows, never {@literal null}
	 */
	public List<Window> getWindows() {
		return windows;
	}

	/**
	 * Test if this schedule has no windows.
	 *
	 * @return {@literal true} if there are no windows
	 */
	public boolean isEmpty() {
		return windows.isEmpty();
	}

	/**
	 * Test if any window applies at a date and time.
	 *
	 * @param dateTime
	 *        the date and time
	 * @return {@literal true} if the schedule applies
	 */
	public boolean applies(LocalDateTime dateTime) {
		for ( Window w : windows ) {
			if ( w.applies(dateTime) ) {
				return true;
			}
		}
		return false;
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
		if ( windows.isEmpty() ) {
			return null;
		}
		final boolean current = applies(from);
		LocalDate date = from.toLocalDate();
		int startMinute = from.toLocalTime().toSecondOfDay() / 60 + 1;
		for ( int day = 0; day < MAX_BOUNDARY_SCAN_DAYS; day++ ) {
			List<Integer> edges = new ArrayList<>(8);
			for ( Window w : windows ) {
				if ( !w.appliesToDate(date) ) {
					continue;
				}
				if ( w.minutes() == null ) {
					edges.add(0);
					edges.add(MINUTES_PER_DAY);
				} else {
					for ( IntRange r : w.minutes() ) {
						edges.add(r.getMin());
						edges.add(r.getMax());
					}
				}
			}
			// the start of each day is an edge too, in case the date columns change
			edges.add(0);
			edges.add(MINUTES_PER_DAY);
			Collections.sort(edges);
			for ( int edge : edges ) {
				if ( edge < startMinute || edge > MINUTES_PER_DAY ) {
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

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		for ( Window w : windows ) {
			if ( buf.length() > 0 ) {
				buf.append("; ");
			}
			buf.append(w);
		}
		return buf.toString();
	}

}

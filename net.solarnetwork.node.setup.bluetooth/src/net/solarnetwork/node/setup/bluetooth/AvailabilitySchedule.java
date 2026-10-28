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
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifiers;
import de.siegmar.fastcsv.writer.CsvWriter;
import net.solarnetwork.domain.tariff.ChronoFieldsTariff;
import net.solarnetwork.domain.tariff.CsvTemporalRangeTariffParser;
import net.solarnetwork.domain.tariff.SimpleTemporalTariffSchedule;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeContainer;

/**
 * A schedule of availability windows, in the tariff schedule CSV format.
 *
 * <p>
 * The schedule is CSV with a header row and five columns: month, day of month,
 * weekday, time of day, and an availability value of {@code 1} (available) or
 * {@code 0} (not available), for example:
 * </p>
 *
 * <pre>
 * Month,Day,Weekday,Time,Available
 * ,,Mon-Fri,07:00-18:00,1
 * Dec,24-26,,,1
 * </pre>
 *
 * <p>
 * An empty cell matches every value. Lines starting with {@code #} are
 * comments. The time end is exclusive, so {@code 07:00-18:00} applies from
 * 07:00 up to but not including 18:00, and {@code 24:00} is the end of the day.
 * The first row that matches a time decides it, so a {@code 0} row placed
 * before others acts as a blackout.
 * </p>
 *
 * <p>
 * Ranges must be ascending. A descending range such as {@code 22:00-06:00} is
 * rejected rather than silently reordered, so an overnight window must be
 * written as two rows. Names are parsed in English.
 * </p>
 *
 * @author elijah
 * @version 1.0
 */
public final class AvailabilitySchedule {

	/** An example header row. */
	public static final String EXAMPLE_HEADER = "Month,Day,Weekday,Time,Available";

	/** The number of minutes in a day. */
	private static final int MINUTES_PER_DAY = 24 * 60;

	/** The maximum number of days to scan for the next boundary. */
	private static final int MAX_BOUNDARY_SCAN_DAYS = 366;

	private static final AvailabilitySchedule EMPTY = new AvailabilitySchedule(Collections.emptyList());

	private final List<ChronoFieldsTariff> rules;
	private final SimpleTemporalTariffSchedule schedule;
	private final int[] edges;

	private AvailabilitySchedule(List<ChronoFieldsTariff> rules) {
		super();
		this.rules = Collections.unmodifiableList(rules);
		this.schedule = new SimpleTemporalTariffSchedule(rules, null);
		this.schedule.setFirstMatchOnly(true);

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
	 * Parse a schedule.
	 *
	 * @param text
	 *        the schedule CSV text; {@literal null} or blank produces an empty
	 *        schedule
	 * @return the schedule, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if the schedule cannot be parsed; the message names the row
	 */
	public static AvailabilitySchedule parse(@Nullable String text) {
		if ( text == null || text.isBlank() ) {
			return EMPTY;
		}
		List<ChronoFieldsTariff> rules;
		try {
			requireHeader(text);
			rules = new CsvTemporalRangeTariffParser(Locale.ENGLISH, false, true)
					.parseTariffs(new StringReader(text));
		} catch ( IOException e ) {
			// not expected from a StringReader
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		int row = 0;
		for ( ChronoFieldsTariff rule : rules ) {
			row++;
			if ( rule.getRates().size() != 1 ) {
				throw new IllegalArgumentException(String.format(
						"Schedule rule %d must have exactly one availability value of 0 or 1", row));
			}
			BigDecimal value = rule.getRates().values().iterator().next().getAmount();
			if ( value.signum() != 0 && value.compareTo(BigDecimal.ONE) != 0 ) {
				throw new IllegalArgumentException(
						String.format("Schedule rule %d availability value [%s] must be 0 or 1", row,
								value.toPlainString()));
			}
		}
		return (rules.isEmpty() ? EMPTY : new AvailabilitySchedule(rules));
	}

	/**
	 * Parse a schedule from a metadata value.
	 *
	 * <p>
	 * The value can be the schedule CSV as a string, or the CSV rows as a list
	 * of lists, like a JSON array of arrays, where the first row is the header.
	 * A list is converted to CSV text and parsed the same way as a string, so
	 * both forms are validated identically.
	 * </p>
	 *
	 * @param value
	 *        the metadata value
	 * @return the schedule, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if the value is not a supported type or the schedule cannot be
	 *         parsed
	 */
	public static AvailabilitySchedule fromMetadata(Object value) {
		if ( value instanceof String s ) {
			return parse(s);
		}
		if ( value instanceof List<?> rows ) {
			return parse(toCsv(rows));
		}
		throw new IllegalArgumentException("Unsupported schedule metadata type "
				+ (value != null ? value.getClass().getSimpleName() : "null")
				+ ": use a CSV string or an array of rows");
	}

	private static String toCsv(List<?> rows) {
		StringWriter out = new StringWriter();
		try (CsvWriter csv = CsvWriter.builder().build(out)) {
			int rowNum = 0;
			for ( Object row : rows ) {
				rowNum++;
				if ( !(row instanceof List<?> cells) ) {
					throw new IllegalArgumentException(String
							.format("Schedule metadata row %d must be an array of values", rowNum));
				}
				List<String> fields = new ArrayList<>(cells.size());
				for ( Object cell : cells ) {
					if ( cell instanceof Map<?, ?> || cell instanceof Collection<?>
							|| (cell != null && cell.getClass().isArray()) ) {
						throw new IllegalArgumentException(String.format(
								"Schedule metadata row %d must contain only simple values", rowNum));
					}
					fields.add(cell != null ? String.valueOf(cell) : "");
				}
				csv.writeRecord(fields);
			}
		} catch ( IOException e ) {
			// not expected from a StringWriter
			throw new IllegalStateException(e);
		}
		return out.toString();
	}

	/**
	 * Reject a schedule whose first row is a rule rather than a header.
	 *
	 * <p>
	 * The tariff parser always treats the first row as the header, so without
	 * this check a missing header would silently drop the first rule.
	 * </p>
	 */
	private static void requireHeader(String text) throws IOException {
		try (CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
				.allowExtraFields(true).commentStrategy(CommentStrategy.SKIP)
				.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
						new StringReader(text))) {
			for ( CsvRecord header : csv ) {
				if ( header.getFieldCount() < 5 ) {
					// the parser reports this
					return;
				}
				String value = header.getField(4);
				try {
					new BigDecimal(value);
				} catch ( NumberFormatException e ) {
					// not a number, so a header name
					return;
				}
				throw new IllegalArgumentException(
						"The schedule must start with a header row, like " + EXAMPLE_HEADER);
			}
		}
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
	 * @return {@literal true} if the first rule that matches has an
	 *         availability value of {@code 1}
	 */
	public boolean applies(LocalDateTime dateTime) {
		if ( rules.isEmpty() ) {
			return false;
		}
		// parse() guarantees each rule has exactly one rate
		Tariff t = schedule.resolveTariff(dateTime);
		return (t != null && !t.getRates().isEmpty()
				&& t.getRates().values().iterator().next().getAmount().signum() > 0);
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
	 * @return the CSV, or an empty string if the schedule is empty
	 */
	public String toCsv() {
		if ( rules.isEmpty() ) {
			return "";
		}
		StringWriter out = new StringWriter();
		try {
			new CsvTemporalRangeTariffParser(Locale.ENGLISH).formatCsv(rules, out);
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

/* ==================================================================
 * TemporalFormatter.java - 13 Nov 2026 3:12:08 pm
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

package net.solarnetwork.node.setup.web.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.List;
import java.util.Locale;
import org.springframework.format.Formatter;
import net.solarnetwork.util.DateUtils;

/**
 * A {@link Formatter} for a temporal type that parses with a list of
 * formatters, using the first one that succeeds.
 *
 * @param <T>
 *        the temporal type
 * @author matt
 * @version 1.0
 * @since 5.4
 */
public class TemporalFormatter<T extends TemporalAccessor> implements Formatter<T> {

	/**
	 * An instant formatter that accepts the same date formats as SolarQuery.
	 *
	 * <p>
	 * A value without a time zone offset is taken as UTC.
	 * </p>
	 */
	public static final TemporalFormatter<Instant> SOLARNET_INSTANT_FORMATTER = new TemporalFormatter<>(
			Instant::from, DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_UTC,
			DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_ALT_UTC, DateUtils.ISO_DATE_TIME_ALT_UTC,
			DateTimeFormatter.ISO_INSTANT, DateTimeFormatter.ISO_DATE_TIME);

	/**
	 * A local date and time formatter that accepts the same date formats as
	 * SolarQuery.
	 */
	public static final TemporalFormatter<LocalDateTime> SOLARNET_LOCAL_DATE_TIME_FORMATTER = new TemporalFormatter<>(
			LocalDateTime::from, DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_UTC,
			DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_ALT_UTC);

	private final TemporalQuery<T> query;
	private final List<DateTimeFormatter> formatters;

	/**
	 * Constructor.
	 *
	 * @param query
	 *        the query to get the temporal type from a parsed value
	 * @param formatters
	 *        the formatters to parse with, in order; the first one is also used
	 *        to print values
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}, or no formatters are given
	 */
	public TemporalFormatter(TemporalQuery<T> query, DateTimeFormatter... formatters) {
		super();
		this.query = requireNonNullArgument(query, "query");
		this.formatters = List.of(requireNonNullArgument(formatters, "formatters"));
		if ( this.formatters.isEmpty() ) {
			throw new IllegalArgumentException("At least one formatter must be provided.");
		}
	}

	@Override
	public String print(T object, Locale locale) {
		return formatters.get(0).format(object);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws DateTimeParseException
	 *         if no formatter can parse {@code text}; the exception is the one
	 *         thrown by the first formatter
	 */
	@Override
	public T parse(String text, Locale locale) {
		DateTimeParseException error = null;
		for ( DateTimeFormatter formatter : formatters ) {
			try {
				return formatter.parse(text, query);
			} catch ( DateTimeParseException e ) {
				if ( error == null ) {
					error = e;
				}
			}
		}
		throw (error != null ? error : new DateTimeParseException("Unparseable date", text, 0));
	}

}

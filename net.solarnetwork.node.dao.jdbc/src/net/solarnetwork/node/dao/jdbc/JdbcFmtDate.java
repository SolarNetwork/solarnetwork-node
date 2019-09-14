/* ==================================================================
 * JdbcFmtDate.java - 7/10/2016 7:06:46 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc;

import java.util.Calendar;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ift.DateCellProcessor;
import org.supercsv.cellprocessor.ift.StringCellProcessor;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.util.CsvContext;

/**
 * Format dates using a Joda {@link DateTimeFormatter}.
 * 
 * @author matt
 * @version 1.1
 */
public class JdbcFmtDate extends CellProcessorAdaptor implements DateCellProcessor {

	private static final String DATE_PATTERN = "yyyy-MM-dd";
	private static final String TIME_PATTERN = "HH:mm:ss.SSS";
	private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	private final DateTimeFormatter dateFormatter;

	public JdbcFmtDate(DateTimeFormatter dateFormatter) {
		super();
		this.dateFormatter = dateFormatter;
	}

	public JdbcFmtDate(DateTimeFormatter dateFormatter, StringCellProcessor next) {
		super(next);
		this.dateFormatter = dateFormatter;
	}

	public static final class Timestamp extends JdbcFmtDate {

		public Timestamp() {
			super(DateTimeFormat.forPattern(TIMESTAMP_PATTERN).withZoneUTC());
		}

		public Timestamp(StringCellProcessor next) {
			super(DateTimeFormat.forPattern(TIMESTAMP_PATTERN).withZoneUTC(), next);
		}
	}

	public static final class Date extends JdbcFmtDate {

		public Date() {
			super(DateTimeFormat.forPattern(DATE_PATTERN));
		}

		public Date(StringCellProcessor next) {
			super(DateTimeFormat.forPattern(DATE_PATTERN), next);
		}
	}

	public static final class Time extends JdbcFmtDate {

		public Time() {
			super(DateTimeFormat.forPattern(TIME_PATTERN));
		}

		public Time(StringCellProcessor next) {
			super(DateTimeFormat.forPattern(TIME_PATTERN), next);
		}
	}

	@Override
	public <T> T execute(final Object value, final CsvContext context) {
		validateInputNotNull(value, context);

		String result;

		if ( value instanceof java.util.Date ) {
			result = dateFormatter.print(((java.util.Date) value).getTime());
		} else if ( value instanceof Calendar ) {
			result = dateFormatter.print(((Calendar) value).getTimeInMillis());
		} else if ( value instanceof ReadableInstant ) {
			result = dateFormatter.print((ReadableInstant) value);
		} else if ( value instanceof ReadablePartial ) {
			result = dateFormatter.print((ReadablePartial) value);
		} else {
			throw new SuperCsvCellProcessorException(java.util.Date.class, value, context, this);
		}

		return next.execute(result, context);
	}

}

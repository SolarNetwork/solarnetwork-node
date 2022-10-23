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

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ift.DateCellProcessor;
import org.supercsv.cellprocessor.ift.StringCellProcessor;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.util.CsvContext;

/**
 * Format dates using a Joda {@link DateTimeFormatter}.
 * 
 * @author matt
 * @version 2.0
 */
public class JdbcFmtDate extends CellProcessorAdaptor implements DateCellProcessor {

	private final DateTimeFormatter dateFormatter;

	/**
	 * Constructor.
	 * 
	 * @param dateFormatter
	 *        the formatter to use
	 */
	public JdbcFmtDate(DateTimeFormatter dateFormatter) {
		super();
		this.dateFormatter = dateFormatter;
	}

	/**
	 * Constructor.
	 * 
	 * @param dateFormatter
	 *        the formatter to use
	 * @param next
	 *        the next processor
	 */
	public JdbcFmtDate(DateTimeFormatter dateFormatter, StringCellProcessor next) {
		super(next);
		this.dateFormatter = dateFormatter;
	}

	/**
	 * Format an instant.
	 */
	public static final class Timestamp extends JdbcFmtDate {

		/**
		 * Constructor.
		 */
		public Timestamp() {
			super(DateTimeFormatter.ISO_INSTANT);
		}

		/**
		 * Constructor.
		 * 
		 * @param next
		 *        the next processor
		 */
		public Timestamp(StringCellProcessor next) {
			super(DateTimeFormatter.ISO_INSTANT, next);
		}
	}

	/**
	 * Format a local date.
	 */
	public static final class Date extends JdbcFmtDate {

		/**
		 * Constructor.
		 */
		public Date() {
			super(DateTimeFormatter.ISO_LOCAL_DATE);
		}

		/**
		 * Constructor.
		 * 
		 * @param next
		 *        the next processor
		 */
		public Date(StringCellProcessor next) {
			super(DateTimeFormatter.ISO_LOCAL_DATE, next);
		}
	}

	/**
	 * Format a local time.
	 */
	public static final class Time extends JdbcFmtDate {

		/**
		 * Constructor.
		 */
		public Time() {
			super(DateTimeFormatter.ISO_LOCAL_TIME);
		}

		/**
		 * Constructor.
		 * 
		 * @param next
		 *        the next processor
		 */
		public Time(StringCellProcessor next) {
			super(DateTimeFormatter.ISO_LOCAL_TIME, next);
		}
	}

	@Override
	public <T> T execute(final Object value, final CsvContext context) {
		validateInputNotNull(value, context);

		String result;

		if ( value instanceof java.util.Date ) {
			result = dateFormatter.format(((java.util.Date) value).toInstant());
		} else if ( value instanceof Calendar ) {
			result = dateFormatter.format(((Calendar) value).getTime().toInstant());
		} else if ( value instanceof TemporalAccessor ) {
			result = dateFormatter.format((TemporalAccessor) value);
		} else {
			throw new SuperCsvCellProcessorException(java.util.Date.class, value, context, this);
		}

		return next.execute(result, context);
	}

}

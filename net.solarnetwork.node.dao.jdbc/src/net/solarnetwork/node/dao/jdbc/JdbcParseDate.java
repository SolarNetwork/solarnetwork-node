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

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ift.DateCellProcessor;
import org.supercsv.cellprocessor.ift.StringCellProcessor;
import org.supercsv.util.CsvContext;

/**
 * Format dates using a Joda {@link DateTimeFormatter}.
 * 
 * @author matt
 * @version 1.1
 */
public abstract class JdbcParseDate extends CellProcessorAdaptor implements StringCellProcessor {

	private static final String DATE_PATTERN = "yyyy-MM-dd";
	private static final String TIME_PATTERN = "HH:mm:ss.SSS";
	private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	protected final DateTimeFormatter dateFormatter;

	private JdbcParseDate(DateTimeFormatter dateFormatter) {
		super();
		this.dateFormatter = dateFormatter;
	}

	private JdbcParseDate(DateTimeFormatter dateFormatter, final DateCellProcessor next) {
		super(next);
		this.dateFormatter = dateFormatter;
	}

	public static final class Timestamp extends JdbcParseDate {

		public Timestamp() {
			super(DateTimeFormat.forPattern(TIMESTAMP_PATTERN).withZoneUTC());
		}

		public Timestamp(DateCellProcessor next) {
			super(DateTimeFormat.forPattern(TIMESTAMP_PATTERN).withZoneUTC(), next);
		}

		@Override
		protected Object parseObject(Object value, CsvContext context) {
			return new java.sql.Timestamp(dateFormatter.parseDateTime(value.toString()).getMillis());
		}

	}

	public static final class Date extends JdbcParseDate {

		public Date() {
			super(DateTimeFormat.forPattern(DATE_PATTERN));
		}

		public Date(DateCellProcessor next) {
			super(DateTimeFormat.forPattern(DATE_PATTERN), next);
		}

		@Override
		protected Object parseObject(Object value, CsvContext context) {
			return new java.sql.Date(dateFormatter.parseLocalDate(value.toString()).toDate().getTime());
		}
	}

	public static final class Time extends JdbcParseDate {

		public Time() {
			super(DateTimeFormat.forPattern(TIME_PATTERN));
		}

		public Time(DateCellProcessor next) {
			super(DateTimeFormat.forPattern(TIME_PATTERN), next);
		}

		@Override
		protected Object parseObject(Object value, CsvContext context) {
			return new java.sql.Time(
					dateFormatter.parseLocalTime(value.toString()).toDateTimeToday().getMillis());
		}
	}

	protected abstract Object parseObject(Object value, CsvContext context);

	@Override
	public <T> T execute(final Object value, final CsvContext context) {
		validateInputNotNull(value, context);

		Object result = parseObject(value, context);

		return next.execute(result, context);
	}

}

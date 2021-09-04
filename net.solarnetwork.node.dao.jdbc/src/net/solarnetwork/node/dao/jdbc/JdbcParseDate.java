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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ift.DateCellProcessor;
import org.supercsv.cellprocessor.ift.StringCellProcessor;
import org.supercsv.util.CsvContext;

/**
 * Format dates using a {@link DateTimeFormatter}.
 * 
 * @author matt
 * @version 2.0
 */
public abstract class JdbcParseDate extends CellProcessorAdaptor implements StringCellProcessor {

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
			super(DateTimeFormatter.ISO_INSTANT);
		}

		public Timestamp(DateCellProcessor next) {
			super(DateTimeFormatter.ISO_INSTANT, next);
		}

		@Override
		protected Object parseObject(Object value, CsvContext context) {
			return java.sql.Timestamp.from(dateFormatter.parse(value.toString(), Instant::from));
		}

	}

	public static final class Date extends JdbcParseDate {

		public Date() {
			super(DateTimeFormatter.ISO_LOCAL_DATE);
		}

		public Date(DateCellProcessor next) {
			super(DateTimeFormatter.ISO_LOCAL_DATE, next);
		}

		@Override
		protected Object parseObject(Object value, CsvContext context) {
			return java.sql.Date.valueOf(dateFormatter.parse(value.toString(), LocalDate::from));
		}
	}

	public static final class Time extends JdbcParseDate {

		public Time() {
			super(DateTimeFormatter.ISO_LOCAL_TIME);
		}

		public Time(DateCellProcessor next) {
			super(DateTimeFormatter.ISO_LOCAL_TIME, next);
		}

		@Override
		protected Object parseObject(Object value, CsvContext context) {
			return java.sql.Time.valueOf(dateFormatter.parse(value.toString(), LocalTime::from));
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

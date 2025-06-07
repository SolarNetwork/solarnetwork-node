/* ==================================================================
 * ResultSetCsvWriter.java - 6/10/2016 8:39:55 AM
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

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.AbstractCsvWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.Util;

/**
 * Implementation of {@link JdbcResultSetCsvWriter}.
 *
 * @author matt
 * @version 1.0
 * @since 1.17
 */
public class ResultSetCsvWriter extends AbstractCsvWriter implements JdbcResultSetCsvWriter {

	/**
	 * Construct with a {@code Writer} and preferences.
	 *
	 * @param writer
	 *        The writer to write CSV data to.
	 * @param preference
	 *        The preferences to use.
	 */
	public ResultSetCsvWriter(Writer writer, CsvPreference preference) {
		super(writer, preference);
	}

	@Override
	public void write(ResultSet resultSet) throws SQLException, IOException {
		write(resultSet, null);
	}

	@Override
	public void write(ResultSet resultSet, CellProcessor[] cellProcessors)
			throws SQLException, IOException {
		assert resultSet != null;
		final String[] headers = resultSetHeaders(resultSet);
		incrementRowAndLineNo();
		writeRow(headers);
		writeContents(headers, resultSet, cellProcessors);
	}

	private String[] resultSetHeaders(ResultSet resultSet) throws SQLException {
		final ResultSetMetaData meta = resultSet.getMetaData();
		final int colCount = meta.getColumnCount();
		final String[] headers = new String[colCount];
		for ( int i = 0; i < colCount; i++ ) {
			headers[i] = meta.getColumnName(i + 1).toUpperCase(Locale.ROOT);
		}
		return headers;
	}

	private void writeContents(String[] headers, ResultSet resultSet, CellProcessor[] cellProcessors)
			throws SQLException, IOException {
		final int colCount = headers.length;
		final List<Object> objects = new ArrayList<Object>(headers.length);
		final List<Object> processed = (cellProcessors == null ? null
				: new ArrayList<Object>(headers.length));
		final ResultSetMetaData meta = resultSet.getMetaData();
		final Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		while ( resultSet.next() ) {
			super.incrementRowAndLineNo();
			objects.clear();
			for ( int i = 1; i <= colCount; i++ ) {
				int sqlType = meta.getColumnType(i);
				Object columnValue;
				if ( sqlType == Types.DATE ) {
					columnValue = resultSet.getDate(i, utcCalendar);
				} else if ( sqlType == Types.TIME ) {
					columnValue = resultSet.getTime(i, utcCalendar);
				} else if ( sqlType == Types.TIMESTAMP ) {
					columnValue = resultSet.getTimestamp(i, utcCalendar);
				} else {
					columnValue = resultSet.getObject(i);
				}
				objects.add(columnValue);
			}
			if ( cellProcessors != null ) {
				Util.executeCellProcessors(processed, objects, cellProcessors, getLineNumber(),
						getRowNumber());
				writeRow(processed);
			} else {
				writeRow(objects);
			}
		}
	}

}

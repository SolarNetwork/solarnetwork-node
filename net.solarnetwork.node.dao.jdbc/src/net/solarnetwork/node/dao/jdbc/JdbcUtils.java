/* ==================================================================
 * JdbcUtils.java - 6/10/2016 12:54:09 PM
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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import org.supercsv.cellprocessor.ConvertNullTo;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseBigDecimal;
import org.supercsv.cellprocessor.ParseBool;
import org.supercsv.cellprocessor.ift.CellProcessor;

/**
 * Utilities to help with JDBC.
 * 
 * @author matt
 * @version 1.0
 * @since 1.17
 */
public abstract class JdbcUtils {

	private JdbcUtils() {
		// don't construct me
	}

	/**
	 * Get a set of {@link CellProcessor} for formatting ResultSet data as CSV
	 * strings.
	 * 
	 * @param meta
	 *        The metadata.
	 * @return The processors.
	 * @throws SQLException
	 *         If any SQL error occurs.
	 */
	public static CellProcessor[] formattingProcessorsForResultSetMetaData(ResultSetMetaData meta)
			throws SQLException {
		int colCount = meta.getColumnCount();
		CellProcessor[] cellProcessors = new CellProcessor[colCount];
		for ( int i = 0; i < colCount; i++ ) {
			CellProcessor processor = null;
			int sqlType = meta.getColumnType(i + 1);
			switch (sqlType) {
				case Types.DATE:
					processor = new ConvertNullTo("", new JdbcFmtDate.Date());
					break;

				case Types.TIME:
					processor = new ConvertNullTo("", new JdbcFmtDate.Time());
					break;

				case Types.TIMESTAMP:
				case Types.TIMESTAMP_WITH_TIMEZONE:
					processor = new ConvertNullTo("", new JdbcFmtDate.Timestamp());
					break;
			}
			cellProcessors[i] = processor;
		}
		return cellProcessors;
	}

	/**
	 * Get a set of {@link CellProcessor} for parsing CSV strings into JDBC
	 * column objects.
	 * 
	 * @param csvColumns
	 *        The parsed CSV column names (i.e. from the header row).
	 * @param columnMetaData
	 *        JDBC column metadata (i.e. extracted from JDBC via
	 *        {@link #columnCsvMetaDataForDatabaseMetaData(DatabaseMetaData, String)})
	 * @return The cell processors.
	 */
	public static CellProcessor[] parsingCellProcessorsForCsvColumns(String[] csvColumns,
			Map<String, ColumnCsvMetaData> columnMetaData) {
		CellProcessor[] result = new CellProcessor[csvColumns.length];
		int i = 0;
		for ( String colName : csvColumns ) {
			ColumnCsvMetaData meta = columnMetaData.get(colName);
			result[i++] = (meta != null && meta.getCellProcessor() != null ? meta.getCellProcessor()
					: new Optional());
		}
		return result;
	}

	/**
	 * Get a mapping of JDBC column names to associated column metadata from a
	 * JDBC {@link DatabaseMetaData} object.
	 * 
	 * @param meta
	 *        The database metadata to read from.
	 * @param tableName
	 *        The table name to get column metadata for.
	 * @return The metadata.
	 * @throws SQLException
	 *         If any SQL error occurs.
	 */
	public static Map<String, ColumnCsvMetaData> columnCsvMetaDataForDatabaseMetaData(
			DatabaseMetaData meta, String tableName) throws SQLException {
		String[] names = tableName.toUpperCase().split("\\.", 2);
		String schema = (names.length == 2 ? names[0] : null);
		String table = (names.length == 2 ? names[1] : names[0]);
		ResultSet rs = meta.getColumns(null, schema, table, null);
		Map<String, ColumnCsvMetaData> results = new LinkedHashMap<String, ColumnCsvMetaData>(8);
		try {

			while ( rs.next() ) {
				String colName = rs.getString(4);
				int sqlType = rs.getInt(5);
				CellProcessor processor = null;
				switch (sqlType) {
					case Types.BOOLEAN:
						processor = new ParseBool();
						break;

					case Types.DATE:
						processor = new JdbcParseDate.Date();
						break;

					case Types.TIME:
						processor = new JdbcParseDate.Time();
						break;

					case Types.TIMESTAMP:
					case Types.TIMESTAMP_WITH_TIMEZONE:
						processor = new JdbcParseDate.Timestamp();
						break;

					case Types.BIGINT:
					case Types.DECIMAL:
					case Types.DOUBLE:
					case Types.FLOAT:
					case Types.INTEGER:
					case Types.NUMERIC:
					case Types.REAL:
					case Types.SMALLINT:
					case Types.TINYINT:
						processor = new ParseBigDecimal();
						break;
				}
				results.put(colName, new ColumnCsvMetaData(colName,
						(processor == null ? new Optional() : new Optional(processor)), sqlType));
			}
		} finally {
			rs.close();
		}

		// get primary key status
		rs = meta.getPrimaryKeys(null, schema, table);
		try {
			while ( rs.next() ) {
				String colName = rs.getString(4);
				ColumnCsvMetaData colMeta = results.get(colName);
				if ( colMeta != null ) {
					results.put(colName, colMeta.asPrimaryKeyColumn());
				}
			}
		} finally {
			rs.close();
		}
		return results;
	}

	/**
	 * Get a SQL string for inserting into a table using column metadata.
	 * 
	 * @param tableName
	 *        The table name to insert into.
	 * @param columnMetaData
	 *        The column metadata of that table, i.e. from
	 *        {@link #columnCsvMetaDataForDatabaseMetaData(DatabaseMetaData, String)}.
	 * @return The SQL statement.
	 */
	public static String insertSqlForColumnCsvMetaData(String tableName,
			Map<String, ColumnCsvMetaData> columnMetaData) {
		StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
		StringBuilder values = new StringBuilder();
		int i = 0;
		for ( Map.Entry<String, ColumnCsvMetaData> me : columnMetaData.entrySet() ) {
			if ( i > 0 ) {
				sql.append(",");
				values.append(",");
			}
			sql.append(me.getKey());
			values.append("?");
			i++;
		}
		sql.append(") VALUES (").append(values).append(")");
		return sql.toString();
	}

	/**
	 * Get a mapping of CVS column names to their associated position in an
	 * array.
	 * 
	 * @param header
	 *        The parsed CVS column header row.
	 * @return The mapping of headers. The iteration order preserves the order
	 *         of the array.
	 */
	public static Map<String, Integer> csvColumnIndexMapping(String[] header) {
		Map<String, Integer> csvColumns = new LinkedHashMap<String, Integer>();
		for ( int i = 0; i < header.length; i++ ) {
			csvColumns.put(header[i], i);
		}
		return csvColumns;
	}

}

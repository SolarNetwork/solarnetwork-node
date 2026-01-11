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

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * Utilities to help with JDBC.
 *
 * @author matt
 * @version 2.0
 * @since 1.17
 */
public abstract class JdbcUtils {

	private JdbcUtils() {
		// don't construct me
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
		String[] names = (meta.storesLowerCaseIdentifiers() ? tableName.toLowerCase(Locale.ROOT)
				: tableName.toUpperCase(Locale.ROOT)).split("\\.", 2);
		String schema = (names.length == 2 ? names[0] : null);
		String table = (names.length == 2 ? names[1] : names[0]);
		ResultSet rs = meta.getColumns(null, schema, table, null);
		Map<String, ColumnCsvMetaData> results = new LinkedCaseInsensitiveMap<>(8, Locale.ROOT);
		try {
			while ( rs.next() ) {
				String colName = rs.getString(4);
				int sqlType = rs.getInt(5);
				Function<Object, Object> processor = null;
				switch (sqlType) {
					case Types.BINARY:
					case Types.VARBINARY:
					case Types.LONGVARBINARY:
						processor = (d) -> {
							try {
								return d != null ? Hex.decodeHex(d.toString()) : null;
							} catch ( DecoderException e ) {
								throw new IllegalArgumentException(
										"Unable to decode hex value [" + d + "]");
							}
						};
						break;

					case Types.BOOLEAN:
						processor = (d) -> d != null
								? net.solarnetwork.util.StringUtils.parseBoolean(d.toString())
								: null;
						break;

					case Types.DATE:
						processor = (d) -> d != null ? java.sql.Date.valueOf(
								DateTimeFormatter.ISO_LOCAL_DATE.parse(d.toString(), LocalDate::from))
								: null;
						break;

					case Types.TIME:
						processor = (d) -> d != null ? java.sql.Time.valueOf(
								DateTimeFormatter.ISO_LOCAL_TIME.parse(d.toString(), LocalTime::from))
								: null;
						break;

					case Types.TIMESTAMP:
					case Types.TIMESTAMP_WITH_TIMEZONE:
						processor = (d) -> d != null
								? java.sql.Timestamp.from(
										DateTimeFormatter.ISO_INSTANT.parse(d.toString(), Instant::from))
								: null;
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
						processor = (d) -> d != null ? new BigDecimal(d.toString()) : null;
						break;
				}
				results.put(colName, new ColumnCsvMetaData(colName, processor, sqlType));
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
	public static Map<String, Integer> csvColumnIndexMapping(List<String> header) {
		Map<String, Integer> csvColumns = new LinkedCaseInsensitiveMap<>(header.size(), Locale.ROOT);
		int idx = 0;
		for ( String h : header ) {
			csvColumns.put(h, idx++);
		}
		return csvColumns;
	}

	/**
	 * Load a classpath SQL resource into a String.
	 *
	 * <p>
	 * The classpath resource is taken as the {@code prefix} value and {@code -}
	 * and the {@code classPathResource} combined with a {@code .sql} suffix. If
	 * that resource is not found, then the prefix is split into components
	 * separated by a {@code -} character, and the last component is dropped and
	 * then combined with {@code -} and {@code classPathResource} again to try
	 * to find a match, until there is no prefix left and just the
	 * {@code classPathResource} itself is tried.
	 * </p>
	 *
	 * <p>
	 * This method will cache the SQL resource in the given {@code Map} for
	 * quick future access.
	 * </p>
	 *
	 * @param classPathResource
	 *        the classpath resource to load as a SQL string
	 * @param resourceClass
	 *        the class to load the resource for
	 * @param prefix
	 *        a dash-delimited prefix to add to the resource name
	 * @param sqlResourceCache
	 *        a cache to use for the loaded resource
	 * @return the SQL as a string
	 * @throws RuntimeException
	 *         if the SQL resource cannot be loaded
	 * @since 1.2
	 */
	public static String getSqlResource(String classPathResource, Class<?> resourceClass, String prefix,
			Map<String, String> sqlResourceCache) {
		Class<?> myClass = resourceClass;
		String resourceName = prefix + "-" + classPathResource + ".sql";
		String key = myClass.getName() + ";" + classPathResource;
		if ( sqlResourceCache.containsKey(key) ) {
			return sqlResourceCache.get(key);
		}
		String[] prefixes = prefix.split("-");
		int prefixEndIndex = prefixes.length - 1;
		try {
			Resource r = new ClassPathResource(resourceName, myClass);
			while ( !r.exists() && prefixEndIndex >= 0 ) {
				// try by chopping down prefix, which we split on a dash character
				String subName;
				if ( prefixEndIndex > 0 ) {
					String[] subPrefixes = new String[prefixEndIndex];
					System.arraycopy(prefixes, prefixEndIndex, subPrefixes, 0, prefixEndIndex);
					subName = StringUtils.arrayToDelimitedString(subPrefixes, "-") + "-"
							+ classPathResource;
				} else {
					subName = classPathResource;
				}
				subName += ".sql";
				r = new ClassPathResource(subName, myClass);
				prefixEndIndex--;
			}
			if ( !r.exists() ) {
				throw new RuntimeException("SQL resource " + resourceName + " not found");
			}
			String result = FileCopyUtils.copyToString(new InputStreamReader(r.getInputStream()));
			if ( result != null && result.length() > 0 ) {
				sqlResourceCache.put(key, result);
			}
			return result;
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Load a SQL resource into a String.
	 *
	 * @param resource
	 *        the SQL resource to load
	 * @return the String
	 * @throws RuntimeException
	 *         if the resource cannot be loaded
	 * @since 1.2
	 */
	public static String getSqlResource(Resource resource) {
		try {
			return FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get batch SQL statements, split into multiple statements on the
	 * {@literal ;} character.
	 *
	 * @param sqlResource
	 *        the SQL resource to load
	 * @return the split SQL statements
	 * @throws RuntimeException
	 *         if the resource cannot be loaded
	 * @since 1.2
	 */
	public static String[] getBatchSqlResource(Resource sqlResource) {
		String sql = getSqlResource(sqlResource);
		if ( sql == null ) {
			return null;
		}
		return sql.split(";\\s*");
	}

	/**
	 * Set an {@link Instant} as a timestamp statement parameter.
	 *
	 * <p>
	 * The column type is assumed to be {@code TIMESTAMP} whose values are
	 * stored in UTC.
	 * </p>
	 *
	 * @param stmt
	 *        the statement
	 * @param parameterIndex
	 *        the statement parameter index to set
	 * @param time
	 *        the time to set
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 1.3
	 */
	public static void setUtcTimestampStatementValue(PreparedStatement stmt, int parameterIndex,
			Instant time) throws SQLException {
		if ( time == null ) {
			stmt.setNull(parameterIndex, Types.TIMESTAMP);
		} else {
			LocalDateTime ldt = time.atZone(ZoneOffset.UTC).toLocalDateTime();
			stmt.setObject(parameterIndex, ldt, Types.TIMESTAMP);
		}
	}

	/**
	 * Get an {@link Instant} from a timestamp result set column.
	 *
	 * <p>
	 * The column type is assumed to be {@code TIMESTAMP} whose values are
	 * stored in UTC.
	 * </p>
	 *
	 * @param rs
	 *        the result set
	 * @param columnIndex
	 *        the column index
	 * @return the new instant, or {@literal null} if the column was null
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 1.3
	 */
	public static Instant getUtcTimestampColumnValue(ResultSet rs, int columnIndex) throws SQLException {
		LocalDateTime ltd = rs.getObject(columnIndex, LocalDateTime.class);
		return (ltd != null ? ltd.toInstant(ZoneOffset.UTC) : null);
	}

	/**
	 * Set a {@link UUID} as a pair of long statement parameters.
	 *
	 * @param stmt
	 *        the statement
	 * @param parameterIndex
	 *        the statement parameter index to set the UUID upper bits; the
	 *        lower bits will be set on parameter {@code parameterIndex + 1}
	 * @param uuid
	 *        the UUID to set
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 1.3
	 */
	public static void setUuidParameters(PreparedStatement stmt, int parameterIndex, UUID uuid)
			throws SQLException {
		stmt.setLong(parameterIndex, uuid.getMostSignificantBits());
		stmt.setLong(parameterIndex + 1, uuid.getLeastSignificantBits());
	}

	/**
	 * Get a {@link UUID} from a pair of long result set columns.
	 *
	 * @param rs
	 *        the result set
	 * @param columnIndex
	 *        the column index of the UUID upper bits; the lower bits will be
	 *        read from column {@code columnIndex + 1}
	 * @return the new UUID, or {@literal null} if either column was null
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 1.3
	 */
	public static UUID getUuidColumns(ResultSet rs, int columnIndex) throws SQLException {
		long hi = rs.getLong(columnIndex);
		if ( rs.wasNull() ) {
			return null;
		}
		long lo = rs.getLong(columnIndex + 1);
		if ( rs.wasNull() ) {
			return null;
		}
		return new UUID(hi, lo);
	}

	/**
	 * Get a single SQL order clause for a given column and direction.
	 *
	 * @param columnName
	 *        the column name
	 * @param descending
	 *        {@literal true} for descending order, {@literal false} for
	 *        ascending
	 * @return the SQL clause
	 * @since 1.3
	 */
	public static String sqlOrderClause(String columnName, boolean descending) {
		return columnName + " " + (descending ? "DESC" : "ASC");
	}

}

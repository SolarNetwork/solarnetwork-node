/* ==================================================================
 * HistorianSqlUtils.java - 10 Nov 2026 8:15:12 pm
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

package net.solarnetwork.node.datumhistorian.jdbc.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.dao.LocalDateRangeCriteria;

/**
 * SQL utilities for the JDBC datum historian.
 *
 * @author matt
 * @version 1.0
 */
public final class HistorianSqlUtils {

	/**
	 * A regular expression for a valid schema name.
	 *
	 * <p>
	 * Only unquoted lower-case Postgres identifiers are allowed, so the name
	 * can be included in SQL as-is.
	 * </p>
	 */
	public static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("[a-z_][a-z0-9_]{0,62}");

	/**
	 * The SQL expression of the time zone name of a datum stream: the zone of
	 * its node, from the {@code z} table that
	 * {@link #appendNodeTimeZoneJoin(String, StringBuilder)} joins, or UTC.
	 *
	 * <p>
	 * This is the zone the {@code dh_stream_time_zone()} function of the
	 * aggregate setup gives, which the days and months of the aggregates are
	 * computed in.
	 * </p>
	 */
	public static final String STREAM_TIME_ZONE_SQL = "COALESCE(z.time_zone, 'UTC')";

	/**
	 * The IDs of fixed-offset zones, such as {@code +12:00} or
	 * {@code GMT+12:00}, which Postgres reads with the opposite sign.
	 */
	private static final Pattern OFFSET_ZONE_ID = Pattern.compile("(?:GMT|UTC|UT)?[+-].*");

	/**
	 * The Postgres names of fixed-offset zones that
	 * {@link #timeZoneName(ZoneId)} gives, like {@code <+1200>-12:00}, with
	 * groups for the sign and the rest of the Java offset ID.
	 */
	private static final Pattern POSIX_OFFSET_ZONE_NAME = Pattern
			.compile("<([+-])[0-9]{4}(?:[0-9]{2})?>[+-]([0-9]{2}:[0-9]{2}(?::[0-9]{2})?)");

	private HistorianSqlUtils() {
		// not available
	}

	/**
	 * Require a valid schema name.
	 *
	 * @param schemaName
	 *        the schema name to validate
	 * @return the schema name
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is {@code null} or does not match
	 *         {@link #SCHEMA_NAME_PATTERN}
	 */
	public static String requireValidSchemaName(@Nullable String schemaName) {
		if ( schemaName == null || !SCHEMA_NAME_PATTERN.matcher(schemaName).matches() ) {
			throw new IllegalArgumentException(
					"The schema name [%s] is not valid: it must match the pattern %s"
							.formatted(schemaName, SCHEMA_NAME_PATTERN.pattern()));
		}
		return schemaName;
	}

	/**
	 * Append a join of the {@code dh_node_time_zone} table, as a {@code z}
	 * table, to the {@code dh_datum_meta} table named {@code m} in a query, as
	 * a line.
	 *
	 * @param schemaName
	 *        the schema name, already validated
	 * @param buf
	 *        the buffer to append to
	 * @see #STREAM_TIME_ZONE_SQL
	 */
	public static void appendNodeTimeZoneJoin(String schemaName, StringBuilder buf) {
		buf.append("LEFT OUTER JOIN ").append(schemaName)
				.append(".dh_node_time_zone z ON z.node_id = m.node_id\n");
	}

	/**
	 * Get a time zone name that Postgres reads as the same zone.
	 *
	 * <p>
	 * A region zone is named by its ID, and a zone without an offset as
	 * {@code UTC}. Postgres reads a fixed-offset zone such as {@code +12:00} or
	 * {@code GMT+12:00} as a POSIX zone, whose offset has the opposite sign to
	 * the ISO 8601 offset Java gives, so these are named as a POSIX zone with
	 * the offset reversed, like {@code <+1200>-12:00}.
	 * </p>
	 *
	 * @param zone
	 *        the zone
	 * @return the zone name
	 * @see #timeZoneId(String)
	 */
	public static String timeZoneName(ZoneId zone) {
		if ( zone.normalized() instanceof ZoneOffset offset ) {
			if ( offset.getTotalSeconds() == 0 ) {
				return "UTC";
			}
			if ( OFFSET_ZONE_ID.matcher(zone.getId()).matches() ) {
				final String id = offset.getId();
				return "<" + id.replace(":", "") + ">" + (offset.getTotalSeconds() > 0 ? "-" : "+")
						+ id.substring(1);
			}
		}
		return zone.getId();
	}

	/**
	 * Get the Java zone ID of a Postgres time zone name.
	 *
	 * <p>
	 * This reverses {@link #timeZoneName(ZoneId)}: a POSIX zone name of a fixed
	 * offset, like {@code <+1200>-12:00}, is given as the Java offset ID, like
	 * {@code +12:00}. Any other name, such as a region zone ID or {@code UTC},
	 * is given as it is.
	 * </p>
	 *
	 * @param name
	 *        the Postgres zone name
	 * @return the Java zone ID, or {@code null} if {@code name} is {@code null}
	 * @see #timeZoneName(ZoneId)
	 */
	public static @Nullable String timeZoneId(@Nullable String name) {
		if ( name == null ) {
			return null;
		}
		final Matcher m = POSIX_OFFSET_ZONE_NAME.matcher(name);
		return (m.matches() ? m.group(1) + m.group(2) : name);
	}

	/**
	 * Append the local date criteria of a filter, taking each date to be in a
	 * time zone.
	 *
	 * <p>
	 * This is ported from the SolarNet
	 * {@code DatumSqlUtils.whereLocalDateRange()} method. Each criteria is a
	 * line that starts with a tab and {@code AND}, comparing the timestamp
	 * column to the local date at the time zone: the minimum (inclusive) and
	 * maximum (exclusive) local dates, like this:
	 * </p>
	 *
	 * <pre>{@code
	 * 	AND d.ts >= ? AT TIME ZONE COALESCE(z.time_zone, 'UTC')
	 * 	AND d.ts < ? AT TIME ZONE COALESCE(z.time_zone, 'UTC')
	 * }</pre>
	 *
	 * @param filter
	 *        the filter
	 * @param column
	 *        the timestamp column
	 * @param timeZoneSql
	 *        the SQL expression of the time zone name
	 * @param buf
	 *        the buffer to append to
	 * @return the number of parameters appended
	 * @see #prepareLocalDateRange(LocalDateRangeCriteria, PreparedStatement,
	 *      int)
	 */
	public static int whereLocalDateRange(LocalDateRangeCriteria filter, String column,
			String timeZoneSql, StringBuilder buf) {
		int count = 0;
		if ( filter.getLocalStartDate() != null ) {
			buf.append("\tAND ").append(column).append(" >= ? AT TIME ZONE ").append(timeZoneSql)
					.append('\n');
			count++;
		}
		if ( filter.getLocalEndDate() != null ) {
			buf.append("\tAND ").append(column).append(" < ? AT TIME ZONE ").append(timeZoneSql)
					.append('\n');
			count++;
		}
		return count;
	}

	/**
	 * Prepare the parameters of the local date criteria of a filter.
	 *
	 * <p>
	 * Each date is given as a timestamp without a time zone.
	 * </p>
	 *
	 * @param filter
	 *        the filter
	 * @param stmt
	 *        the statement
	 * @param p
	 *        the index of the last parameter set
	 * @return the index of the last parameter set
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @see #whereLocalDateRange(LocalDateRangeCriteria, String, String,
	 *      StringBuilder)
	 */
	public static int prepareLocalDateRange(LocalDateRangeCriteria filter, PreparedStatement stmt, int p)
			throws SQLException {
		if ( filter.getLocalStartDate() != null ) {
			stmt.setObject(++p, filter.getLocalStartDate(), Types.TIMESTAMP);
		}
		if ( filter.getLocalEndDate() != null ) {
			stmt.setObject(++p, filter.getLocalEndDate(), Types.TIMESTAMP);
		}
		return p;
	}

}

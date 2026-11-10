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

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

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
	 * Only unquoted lower-case Postgres identifiers are allowed, so the name can
	 * be included in SQL as-is.
	 * </p>
	 */
	public static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("[a-z_][a-z0-9_]{0,62}");

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

}

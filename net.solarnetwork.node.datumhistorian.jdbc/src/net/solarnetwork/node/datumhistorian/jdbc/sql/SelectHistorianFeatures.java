/* ==================================================================
 * SelectHistorianFeatures.java - 15 Nov 2026 4:12:08 pm
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

import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.requireValidSchemaName;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlProvider;

/**
 * Select the features of a historian schema.
 *
 * <p>
 * The single row has this column, mapped by {@link #RESULT_MAPPER}:
 * </p>
 *
 * <ol>
 * <li>{@code agg}: {@code true} if the schema has the aggregate setup of
 * {@code postgres-init-agg.sql}, that is its {@code dh_agg_hourly},
 * {@code dh_agg_daily}, {@code dh_agg_monthly}, and {@code dh_node_time_zone}
 * tables</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectHistorianFeatures implements PreparedStatementCreator, SqlProvider {

	/**
	 * The features of a historian schema.
	 *
	 * @param aggregates
	 *        {@code true} if the schema has the aggregate setup
	 */
	public record Result(boolean aggregates) {

	}

	/** A row mapper for the {@link Result} of this statement. */
	public static final RowMapper<Result> RESULT_MAPPER = (rs, rowNum) -> new Result(rs.getBoolean(1));

	/** The aggregate setup tables. */
	private static final String[] AGGREGATE_TABLES = new String[] { "dh_agg_hourly", "dh_agg_daily",
			"dh_agg_monthly", "dh_node_time_zone" };

	private final String schemaName;

	/**
	 * Constructor.
	 *
	 * @param schemaName
	 *        the schema name
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is not valid
	 */
	public SelectHistorianFeatures(String schemaName) {
		super();
		this.schemaName = requireValidSchemaName(schemaName);
	}

	@Override
	public String getSql() {
		final StringBuilder buf = new StringBuilder(256);
		buf.append("SELECT ");
		for ( int i = 0; i < AGGREGATE_TABLES.length; i++ ) {
			if ( i > 0 ) {
				buf.append("\n\tAND ");
			}
			// the schema name is validated, so can be included as-is
			buf.append("to_regclass('").append(schemaName).append('.').append(AGGREGATE_TABLES[i])
					.append("') IS NOT NULL");
		}
		buf.append(" AS agg");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		return con.prepareStatement(getSql());
	}

	/**
	 * Get the schema name.
	 *
	 * @return the schema name
	 */
	public String getSchemaName() {
		return schemaName;
	}

}

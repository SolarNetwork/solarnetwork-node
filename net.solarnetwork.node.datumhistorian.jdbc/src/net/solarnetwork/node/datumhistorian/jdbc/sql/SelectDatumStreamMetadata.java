/* ==================================================================
 * SelectDatumStreamMetadata.java - 13 Nov 2026 10:18:37 am
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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.node.dao.NodeDatumFilter;

/**
 * Select the metadata of the datum streams matching a {@link NodeDatumFilter}.
 *
 * <p>
 * The object, stream, source, and type criteria are applied the same way as by
 * {@link SelectFilteredDatum}, and the other criteria are ignored. With
 * location IDs, the streams of every node that stored datum for those locations
 * are returned, as {@code SelectFilteredDatum} merges the datum of all of them.
 * </p>
 *
 * <p>
 * Rows are ordered by node ID, location ID, and source ID, with these columns:
 * </p>
 *
 * <ol>
 * <li>{@code stream_id}</li>
 * <li>{@code node_id}</li>
 * <li>{@code source_id}</li>
 * <li>{@code loc_id}</li>
 * <li>{@code names_i}</li>
 * <li>{@code names_a}</li>
 * <li>{@code names_s}</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectDatumStreamMetadata implements PreparedStatementCreator, SqlProvider {

	private final String schemaName;
	private final DatumMetaCriteria criteria;

	/**
	 * Constructor.
	 *
	 * @param schemaName
	 *        the schema name
	 * @param filter
	 *        the filter
	 * @param nodeId
	 *        the node ID to limit the results to when {@code filter} has no
	 *        object IDs, or {@code null} for all nodes
	 * @param streamIds
	 *        the {@code stream_id} values of the streams to limit the results
	 *        to, or {@code null} or empty for all streams
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is not valid or {@code filter} is
	 *         {@code null}
	 */
	public SelectDatumStreamMetadata(String schemaName, NodeDatumFilter filter, @Nullable Long nodeId,
			Integer @Nullable [] streamIds) {
		super();
		this.schemaName = requireValidSchemaName(schemaName);
		this.criteria = new DatumMetaCriteria(this.schemaName, requireNonNullArgument(filter, "filter"),
				nodeId, streamIds);
	}

	@Override
	public String getSql() {
		final StringBuilder buf = new StringBuilder(512);
		buf.append("SELECT m.stream_id, m.node_id, m.source_id, m.loc_id")
				.append(", m.names_i, m.names_a, m.names_s\n");
		buf.append("FROM ").append(schemaName).append(".dh_datum_meta m\n");
		final StringBuilder where = new StringBuilder();
		criteria.appendSql(where);
		DatumMetaCriteria.appendWhere(buf, where);
		buf.append("ORDER BY m.node_id, m.loc_id, m.source_id");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		criteria.prepare(con, stmt, 0);
		return stmt;
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

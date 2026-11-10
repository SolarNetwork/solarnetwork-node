/* ==================================================================
 * SelectDatumStreams.java - 10 Nov 2026 8:52:18 pm
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
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlProvider;

/**
 * Select the datum streams of a node.
 *
 * <p>
 * Rows are ordered by stream ID, with these columns:
 * </p>
 *
 * <ol>
 * <li>{@code stream_id}</li>
 * <li>{@code source_id}</li>
 * <li>{@code loc_id}</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectDatumStreams
		implements PreparedStatementCreator, PreparedStatementSetter, SqlProvider {

	private final String schemaName;
	private final long nodeId;

	/**
	 * Constructor.
	 *
	 * @param schemaName
	 *        the schema name
	 * @param nodeId
	 *        the node ID
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is not valid
	 */
	public SelectDatumStreams(String schemaName, long nodeId) {
		super();
		this.schemaName = requireValidSchemaName(schemaName);
		this.nodeId = nodeId;
	}

	@Override
	public String getSql() {
		return "SELECT stream_id, source_id, loc_id\nFROM " + schemaName
				+ ".dh_datum_meta\nWHERE node_id = ?\nORDER BY stream_id";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		setValues(stmt);
		return stmt;
	}

	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		ps.setLong(1, nodeId);
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

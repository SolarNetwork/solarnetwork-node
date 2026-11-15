/* ==================================================================
 * StoreNodeTimeZone.java - 15 Nov 2026 4:12:08 pm
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
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.timeZoneName;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZoneId;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;

/**
 * Store the time zone of a node in the {@code dh_node_time_zone} table, if it
 * differs from the stored zone.
 *
 * <p>
 * The zone is stored by the name that
 * {@link HistorianSqlUtils#timeZoneName(ZoneId)} gives, which Postgres reads
 * as the same zone: a fixed-offset zone like {@code +12:00} is stored as the
 * POSIX zone {@code <+1200>-12:00}, for example.
 * </p>
 *
 * <p>
 * The update count is {@literal 1} if the zone was added or changed, and
 * {@literal 0} if the stored zone is the same. With the aggregate setup, adding
 * or changing a zone marks the daily aggregates of the node's streams stale, so
 * they and their monthly aggregates are computed again in the zone, and the
 * database rejects a zone it does not know.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class StoreNodeTimeZone implements PreparedStatementCreator, SqlProvider {

	private final String schemaName;
	private final long nodeId;
	private final ZoneId timeZone;
	private final String timeZoneName;

	/**
	 * Constructor.
	 *
	 * @param schemaName
	 *        the schema name
	 * @param nodeId
	 *        the node ID
	 * @param timeZone
	 *        the time zone
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is not valid or {@code timeZone} is
	 *         {@code null}
	 */
	public StoreNodeTimeZone(String schemaName, long nodeId, ZoneId timeZone) {
		super();
		this.schemaName = requireValidSchemaName(schemaName);
		this.nodeId = nodeId;
		this.timeZone = requireNonNullArgument(timeZone, "timeZone");
		this.timeZoneName = timeZoneName(timeZone);
	}

	@Override
	public String getSql() {
		return """
				INSERT INTO %s.dh_node_time_zone AS z (node_id, time_zone)
				VALUES (?, ?)
				ON CONFLICT (node_id) DO UPDATE
					SET time_zone = EXCLUDED.time_zone, updated = CURRENT_TIMESTAMP
					WHERE z.time_zone IS DISTINCT FROM EXCLUDED.time_zone""".formatted(schemaName);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setLong(1, nodeId);
		stmt.setString(2, timeZoneName);
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

	/**
	 * Get the node ID.
	 *
	 * @return the node ID
	 */
	public long getNodeId() {
		return nodeId;
	}

	/**
	 * Get the time zone.
	 *
	 * @return the time zone
	 */
	public ZoneId getTimeZone() {
		return timeZone;
	}

	/**
	 * Get the name the time zone is stored by.
	 *
	 * @return the time zone name
	 */
	public String getTimeZoneName() {
		return timeZoneName;
	}

}

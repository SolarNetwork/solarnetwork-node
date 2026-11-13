/* ==================================================================
 * DatumMetaCriteria.java - 13 Nov 2026 10:18:37 am
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

import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareArrayParameter;
import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareParameter;
import static net.solarnetwork.dao.jdbc.CommonSqlUtils.whereEqual;
import static net.solarnetwork.dao.jdbc.CommonSqlUtils.whereOptimizedArrayContains;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.dao.NodeDatumFilter;

/**
 * The criteria of a {@link NodeDatumFilter} that apply to the
 * {@code dh_datum_meta} table, named {@code m} in the query.
 *
 * <p>
 * These are the object, stream, source, and type criteria, applied as described
 * in {@link SelectFilteredDatum}.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
final class DatumMetaCriteria {

	/** The node ID column. */
	static final String NODE_ID_COLUMN = "m.node_id";

	private final String schemaName;
	private final NodeDatumFilter filter;
	private final @Nullable Long nodeId;
	private final Integer @Nullable [] streamIds;

	/**
	 * Constructor.
	 *
	 * @param schemaName
	 *        the schema name, already validated
	 * @param filter
	 *        the filter
	 * @param nodeId
	 *        the node ID to limit the results to when {@code filter} has no
	 *        object IDs, or {@code null} for all nodes
	 * @param streamIds
	 *        the stream IDs to limit the results to, or {@code null} or empty
	 *        for all streams
	 */
	DatumMetaCriteria(String schemaName, NodeDatumFilter filter, @Nullable Long nodeId,
			Integer @Nullable [] streamIds) {
		super();
		this.schemaName = schemaName;
		this.filter = filter;
		this.nodeId = nodeId;
		this.streamIds = streamIds;
	}

	/**
	 * Test if the filter has object IDs.
	 *
	 * @return {@code true} if the filter has at least one object ID
	 */
	boolean hasObjectIds() {
		final Long[] ids = filter.getObjectIds();
		return (ids != null && ids.length > 0);
	}

	/**
	 * Test if the filter has the location object kind.
	 *
	 * @return {@code true} if the object kind is
	 *         {@link ObjectDatumKind#Location}
	 */
	boolean isLocationKind() {
		return filter.getObjectKind() == ObjectDatumKind.Location;
	}

	/**
	 * Append the criteria, as lines that each start with a tab and {@code AND}.
	 *
	 * @param buf
	 *        the buffer to append to
	 * @see #appendWhere(StringBuilder, CharSequence)
	 */
	void appendSql(StringBuilder buf) {
		if ( hasObjectIds() ) {
			if ( isLocationKind() ) {
				whereOptimizedArrayContains(filter.getObjectIds(), "m.loc_id", buf);
			} else {
				whereOptimizedArrayContains(filter.getObjectIds(), NODE_ID_COLUMN, buf);
				buf.append("\tAND m.loc_id IS NULL\n");
			}
		} else {
			whereEqual(nodeId, NODE_ID_COLUMN, buf);
			if ( filter.getObjectKind() == ObjectDatumKind.Node ) {
				buf.append("\tAND m.loc_id IS NULL\n");
			} else if ( isLocationKind() ) {
				buf.append("\tAND m.loc_id IS NOT NULL\n");
			}
		}
		whereOptimizedArrayContains(streamIds, "m.stream_id", buf);
		if ( filter.hasSourceCriteria() ) {
			buf.append("\tAND m.source_id ~* ANY(").append(schemaName)
					.append(".ant_patterns_to_regexps(?))\n");
		}
		if ( filter.hasTypeCriteria() ) {
			// skip the streams that have never stored a matching type
			buf.append("\tAND m.type_ids && ");
			appendTypeIdsSql(buf);
			buf.append("\n");
		}
	}

	/**
	 * Append an array of the IDs of the datum types that match the type
	 * criteria.
	 *
	 * @param buf
	 *        the buffer to append to
	 * @see #prepareTypes(Connection, PreparedStatement, int)
	 */
	void appendTypeIdsSql(StringBuilder buf) {
		buf.append("ARRAY(SELECT tt.type_id FROM ").append(schemaName)
				.append(".dh_datum_type tt WHERE tt.type_names && ?)");
	}

	/**
	 * Prepare the parameters of the criteria.
	 *
	 * @param con
	 *        the connection
	 * @param stmt
	 *        the statement
	 * @param p
	 *        the index of the last parameter set
	 * @return the index of the last parameter set
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @see #appendSql(StringBuilder)
	 */
	int prepare(Connection con, PreparedStatement stmt, int p) throws SQLException {
		if ( hasObjectIds() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getObjectIds());
		} else {
			p = prepareParameter(stmt, p, nodeId);
		}
		p = prepareOptimizedArrayParameter(con, stmt, p, streamIds);
		if ( filter.hasSourceCriteria() ) {
			p = prepareArrayParameter(con, stmt, p, filter.getSourceIds());
		}
		if ( filter.hasTypeCriteria() ) {
			p = prepareTypes(con, stmt, p);
		}
		return p;
	}

	/**
	 * Prepare the parameter of an array of type IDs.
	 *
	 * @param con
	 *        the connection
	 * @param stmt
	 *        the statement
	 * @param p
	 *        the index of the last parameter set
	 * @return the index of the last parameter set
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @see #appendTypeIdsSql(StringBuilder)
	 */
	int prepareTypes(Connection con, PreparedStatement stmt, int p) throws SQLException {
		return prepareArrayParameter(con, stmt, p, filter.getTypes());
	}

	/**
	 * Append a {@code WHERE} clause of criteria.
	 *
	 * @param buf
	 *        the buffer to append to
	 * @param criteria
	 *        the criteria, as lines that each start with a tab and {@code AND};
	 *        nothing is appended if empty
	 */
	static void appendWhere(StringBuilder buf, CharSequence criteria) {
		if ( !criteria.isEmpty() ) {
			// replace the leading tab and AND of the first criteria
			buf.append("WHERE ").append(criteria, 5, criteria.length());
		}
	}

}

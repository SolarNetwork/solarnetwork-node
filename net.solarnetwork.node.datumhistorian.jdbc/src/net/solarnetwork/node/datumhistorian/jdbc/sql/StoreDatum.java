/* ==================================================================
 * StoreDatum.java - 10 Nov 2026 8:21:40 pm
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
import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareObjectParameter;
import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareParameter;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.requireValidSchemaName;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * Store a datum via the {@code dh_store_datum()} function.
 *
 * <p>
 * If a datum type ID is provided, it is passed directly to
 * {@code dh_store_datum()}. Otherwise the type is resolved in the same
 * statement via {@code dh_store_datum_type()}, using the datum class name and
 * type names. Either way the statement returns a single row with the datum type
 * ID and stream ID columns, which {@link #RESULT_MAPPER} can map into a
 * {@link Result}.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class StoreDatum implements PreparedStatementCreator, PreparedStatementSetter, SqlProvider {

	/**
	 * The result of storing a datum.
	 *
	 * @param typeId
	 *        the datum type ID
	 * @param streamId
	 *        the datum stream ID
	 */
	public static record Result(int typeId, int streamId) {

	}

	/** A row mapper for the statement result. */
	public static final RowMapper<Result> RESULT_MAPPER = (rs, rowNum) -> new Result(rs.getInt(1),
			rs.getInt(2));

	private final String schemaName;
	private final Long nodeId;
	private final NodeDatum datum;
	private final @Nullable Integer typeId;
	private final @Nullable Set<String> typeNames;
	private final String jdata;

	/**
	 * Constructor for a datum with a known type ID.
	 *
	 * @param schemaName
	 *        the schema name
	 * @param nodeId
	 *        the node ID
	 * @param datum
	 *        the datum to store
	 * @param typeId
	 *        the datum type ID
	 * @param jdata
	 *        the datum samples JSON object, with {@code i}, {@code a},
	 *        {@code s}, and {@code t} properties
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null} or {@code schemaName} is not
	 *         valid
	 */
	public StoreDatum(String schemaName, Long nodeId, NodeDatum datum, int typeId, String jdata) {
		this(schemaName, nodeId, datum, typeId, null, jdata);
	}

	/**
	 * Constructor for a datum whose type ID is not known.
	 *
	 * @param schemaName
	 *        the schema name
	 * @param nodeId
	 *        the node ID
	 * @param datum
	 *        the datum to store
	 * @param typeNames
	 *        the names of all classes and interfaces the datum class can be
	 *        assigned to, starting with the datum class name itself; a
	 *        sequenced set should be used for stable iteration order
	 * @param jdata
	 *        the datum samples JSON object, with {@code i}, {@code a},
	 *        {@code s}, and {@code t} properties
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null} or {@code schemaName} is not
	 *         valid, or {@code typeNames} is empty
	 */
	public StoreDatum(String schemaName, Long nodeId, NodeDatum datum, Set<String> typeNames,
			String jdata) {
		this(schemaName, nodeId, datum, null, requireNonNullArgument(typeNames, "typeNames"), jdata);
		if ( typeNames.isEmpty() ) {
			throw new IllegalArgumentException("The typeNames argument must not be empty.");
		}
	}

	private StoreDatum(String schemaName, Long nodeId, NodeDatum datum, @Nullable Integer typeId,
			@Nullable Set<String> typeNames, String jdata) {
		super();
		this.schemaName = requireValidSchemaName(schemaName);
		this.nodeId = requireNonNullArgument(nodeId, "nodeId");
		this.datum = requireNonNullArgument(datum, "datum");
		this.typeId = typeId;
		this.typeNames = typeNames;
		this.jdata = requireNonNullArgument(jdata, "jdata");
	}

	@Override
	public String getSql() {
		final StringBuilder buf = new StringBuilder(160);
		if ( typeId != null ) {
			buf.append("SELECT ?::INTEGER AS type_id, ");
			buf.append(schemaName).append(".dh_store_datum(?, ?, ?, ?, ?, ?) AS stream_id");
		} else {
			buf.append("SELECT t.type_id, ");
			buf.append(schemaName).append(".dh_store_datum(?, ?, ?, ?, t.type_id, ?) AS stream_id\n");
			buf.append("FROM ").append(schemaName).append(".dh_store_datum_type(?, ?) AS t(type_id)");
		}
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		setValues(stmt);
		return stmt;
	}

	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		int p = 0;
		final Integer typeId = this.typeId;
		if ( typeId != null ) {
			ps.setInt(++p, typeId);
		}

		final Instant ts = datum.getTimestamp();
		p = prepareObjectParameter(ps, p, Types.TIMESTAMP_WITH_TIMEZONE,
				ts != null ? ts.atOffset(ZoneOffset.UTC) : null, true);
		ps.setObject(++p, nodeId);
		ps.setString(++p, datum.getSourceId());

		final Long locId = (datum.getKind() == ObjectDatumKind.Location ? datum.getObjectId() : null);
		p = prepareParameter(ps, p, locId, true);

		if ( typeId != null ) {
			ps.setInt(++p, typeId);
		}
		ps.setString(++p, jdata);

		final Set<String> typeNames = this.typeNames;
		if ( typeId == null && typeNames != null ) {
			ps.setString(++p, typeNames.iterator().next());
			prepareArrayParameter(ps.getConnection(), ps, p, typeNames.toArray(String[]::new));
		}
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

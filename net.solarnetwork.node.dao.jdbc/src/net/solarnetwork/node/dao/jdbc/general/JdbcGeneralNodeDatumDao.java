/* ==================================================================
 * JdbcGeneralNodeDatumDao.java - Aug 26, 2014 7:03:34 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.general;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesContainer;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.dao.jdbc.AbstractJdbcDatumDao;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;

/**
 * JDBC-based implementation of {@link net.solarnetwork.node.dao.DatumDao} for
 * {@link NodeDatum} domain objects.
 * 
 * @author matt
 * @version 1.3
 */
public class JdbcGeneralNodeDatumDao extends AbstractJdbcDatumDao {

	/** The default tables version. */
	public static final int DEFAULT_TABLES_VERSION = 4;

	/** The table name for datum. */
	public static final String TABLE_GENERAL_NODE_DATUM = "sn_general_node_datum";

	/** The default classpath Resource for the {@code initSqlResource}. */
	public static final String DEFAULT_INIT_SQL = "derby-generalnodedatum-init.sql";

	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String DEFAULT_SQL_GET_TABLES_VERSION = "SELECT svalue FROM solarnode.sn_settings WHERE skey = "
			+ "'solarnode.sn_general_node_datum.version'";

	private ObjectMapper objectMapper;

	/**
	 * Default constructor.
	 */
	public JdbcGeneralNodeDatumDao() {
		super();
		setSqlResourcePrefix("derby-generalnodedatum");
		setTableName(TABLE_GENERAL_NODE_DATUM);
		setTablesVersion(DEFAULT_TABLES_VERSION);
		setSqlGetTablesVersion(DEFAULT_SQL_GET_TABLES_VERSION);
		setInitSqlResource(new ClassPathResource(DEFAULT_INIT_SQL, getClass()));
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED, noRollbackFor = DuplicateKeyException.class)
	public void storeDatum(NodeDatum datum) {
		try {
			storeDomainObject(datum);
		} catch ( DuplicateKeyException e ) {
			List<NodeDatum> existing = findDatum(SQL_RESOURCE_FIND_FOR_PRIMARY_KEY,
					preparedStatementSetterForPrimaryKey(datum.getTimestamp(), datum.getSourceId()),
					rowMapper());
			if ( existing.size() > 0 ) {
				// only update if the samples have changed
				DatumSamplesOperations existingSamples = existing.get(0).asSampleOperations();
				DatumSamplesOperations newSamples = datum.asSampleOperations();
				if ( newSamples.differsFrom(existingSamples) ) {
					updateDomainObject(datum, getSqlResource(SQL_RESOURCE_UPDATE_DATA));
				}
			}
		}
	}

	@Override
	protected void setUpdateStatementValues(NodeDatum datum, PreparedStatement ps) throws SQLException {
		int col = 1;
		ps.setString(col++, jsonForSamples(datum));
		ps.setTimestamp(col++, Timestamp.from(datum.getTimestamp()));
		ps.setString(col++, datum.getSourceId());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setDatumUploaded(NodeDatum datum, Instant date, String destination, String trackingId) {
		updateDatumUpload(datum, date == null ? System.currentTimeMillis() : date.toEpochMilli());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteUploadedDataOlderThan(int hours) {
		return deleteUploadedDataOlderThanHours(hours);
	}

	private RowMapper<NodeDatum> rowMapper() {
		return new RowMapper<NodeDatum>() {

			@Override
			public NodeDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
				if ( log.isTraceEnabled() ) {
					log.trace("Handling result row " + rowNum);
				}
				int col = 0;
				Instant ts = rs.getTimestamp(++col).toInstant();
				String sourceId = rs.getString(++col);
				Long locationId = (Long) rs.getObject(++col);

				DatumId id = (locationId != null ? DatumId.locationId(locationId, sourceId, ts)
						: DatumId.nodeId(null, sourceId, ts));
				DatumSamples s = null;
				String jdata = rs.getString(++col);
				if ( jdata != null ) {
					try {
						s = objectMapper.readValue(jdata, DatumSamples.class);
					} catch ( IOException e ) {
						log.error("Error deserializing JSON into GeneralNodeDatumSamples: {}",
								e.getMessage());
					}
				}
				return new SimpleDatum(id, s);
			}
		};
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<NodeDatum> getDatumNotUploaded(String destination) {
		return findDatumNotUploaded(rowMapper());
	}

	private String jsonForSamples(NodeDatum datum) {
		String json;
		try {
			json = objectMapper.writeValueAsString(((DatumSamplesContainer) datum).getSamples());
		} catch ( IOException e ) {
			log.error("Error serializing DatumSamples into JSON: {}", e.getMessage());
			json = "{}";
		}
		return json;
	}

	@Override
	protected void setStoreStatementValues(NodeDatum datum, PreparedStatement ps) throws SQLException {
		int col = 0;
		ps.setTimestamp(++col,
				Timestamp.from(datum.getTimestamp() == null ? Instant.now() : datum.getTimestamp()));
		ps.setString(++col, datum.getSourceId() == null ? "" : datum.getSourceId());
		if ( datum.getKind() == ObjectDatumKind.Location ) {
			ps.setObject(++col, datum.getObjectId());
		} else {
			ps.setNull(++col, Types.CHAR);
		}

		String json = jsonForSamples(datum);
		ps.setString(++col, json);
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

}

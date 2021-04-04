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
import java.util.Date;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.node.dao.jdbc.AbstractJdbcDatumDao;
import net.solarnetwork.node.domain.GeneralNodeDatum;

/**
 * JDBC-based implementation of {@link net.solarnetwork.node.dao.DatumDao} for
 * {@link GeneralNodeDatum} domain objects.
 * 
 * @author matt
 * @version 1.3
 */
public class JdbcGeneralNodeDatumDao extends AbstractJdbcDatumDao<GeneralNodeDatum> {

	/** The default tables version. */
	public static final int DEFAULT_TABLES_VERSION = 3;

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
	public Class<? extends GeneralNodeDatum> getDatumType() {
		return GeneralNodeDatum.class;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED, noRollbackFor = DuplicateKeyException.class)
	public void storeDatum(GeneralNodeDatum datum) {
		try {
			storeDomainObject(datum);
		} catch ( DuplicateKeyException e ) {
			List<GeneralNodeDatum> existing = findDatum(SQL_RESOURCE_FIND_FOR_PRIMARY_KEY,
					preparedStatementSetterForPrimaryKey(datum.getCreated(), datum.getSourceId()),
					rowMapper());
			if ( existing.size() > 0 ) {
				// only update if the samples have changed
				GeneralDatumSamples existingSamples = existing.get(0).getSamples();
				GeneralDatumSamples newSamples = datum.getSamples();
				if ( !newSamples.equals(existingSamples) ) {
					updateDomainObject(datum, getSqlResource(SQL_RESOURCE_UPDATE_DATA));
				}
			}
		}
	}

	@Override
	protected void setUpdateStatementValues(GeneralNodeDatum datum, PreparedStatement ps)
			throws SQLException {
		int col = 1;
		ps.setString(col++, jsonForSamples(datum));
		ps.setTimestamp(col++, new Timestamp(datum.getCreated().getTime()));
		ps.setString(col++, datum.getSourceId());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setDatumUploaded(GeneralNodeDatum datum, Date date, String destination,
			String trackingId) {
		updateDatumUpload(datum, date == null ? System.currentTimeMillis() : date.getTime());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteUploadedDataOlderThan(int hours) {
		return deleteUploadedDataOlderThanHours(hours);
	}

	private RowMapper<GeneralNodeDatum> rowMapper() {
		return new RowMapper<GeneralNodeDatum>() {

			@Override
			public GeneralNodeDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
				if ( log.isTraceEnabled() ) {
					log.trace("Handling result row " + rowNum);
				}
				GeneralNodeDatum datum = new GeneralNodeDatum();
				int col = 0;
				datum.setCreated(rs.getTimestamp(++col));
				datum.setSourceId(rs.getString(++col));

				String jdata = rs.getString(++col);
				if ( jdata != null ) {
					GeneralNodeDatumSamples s;
					try {
						s = objectMapper.readValue(jdata, GeneralNodeDatumSamples.class);
						datum.setSamples(s);
					} catch ( IOException e ) {
						log.error("Error deserializing JSON into GeneralNodeDatumSamples: {}",
								e.getMessage());
					}
				}
				return datum;
			}
		};
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<GeneralNodeDatum> getDatumNotUploaded(String destination) {
		return findDatumNotUploaded(rowMapper());
	}

	private String jsonForSamples(GeneralNodeDatum datum) {
		String json;
		try {
			json = objectMapper.writeValueAsString(datum.getSamples());
		} catch ( IOException e ) {
			log.error("Error serializing GeneralDatumSamples into JSON: {}", e.getMessage());
			json = "{}";
		}
		return json;
	}

	@Override
	protected void setStoreStatementValues(GeneralNodeDatum datum, PreparedStatement ps)
			throws SQLException {
		int col = 0;
		ps.setTimestamp(++col, new java.sql.Timestamp(
				datum.getCreated() == null ? System.currentTimeMillis() : datum.getCreated().getTime()));
		ps.setString(++col, datum.getSourceId() == null ? "" : datum.getSourceId());

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

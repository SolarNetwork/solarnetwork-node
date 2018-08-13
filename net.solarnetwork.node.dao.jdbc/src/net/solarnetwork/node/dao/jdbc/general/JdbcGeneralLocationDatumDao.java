/* ==================================================================
 * JdbcGeneralLocationDatumDao.java - Oct 20, 2014 12:11:08 PM
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralLocationDatumSamples;
import net.solarnetwork.node.dao.jdbc.AbstractJdbcDatumDao;
import net.solarnetwork.node.domain.GeneralLocationDatum;

/**
 * JDBC-based implementation of {@link net.solarnetwork.node.dao.DatumDao} for
 * {@link GeneralLocationDatum} domain objects.
 * 
 * @author matt
 * @version 1.3
 */
public class JdbcGeneralLocationDatumDao extends AbstractJdbcDatumDao<GeneralLocationDatum> {

	/** The default tables version. */
	public static final int DEFAULT_TABLES_VERSION = 2;

	/** The table name for {@link GeneralLocationDatum} data. */
	public static final String TABLE_GENERAL_LOC_DATUM = "sn_general_loc_datum";

	/** The default classpath Resource for the {@code initSqlResource}. */
	public static final String DEFAULT_INIT_SQL = "derby-generallocdatum-init.sql";

	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String DEFAULT_SQL_GET_TABLES_VERSION = "SELECT svalue FROM solarnode.sn_settings WHERE skey = "
			+ "'solarnode.sn_general_loc_datum.version'";

	private ObjectMapper objectMapper;

	/**
	 * Default constructor.
	 */
	public JdbcGeneralLocationDatumDao() {
		super();
		setSqlResourcePrefix("derby-generallocdatum");
		setTableName(TABLE_GENERAL_LOC_DATUM);
		setTablesVersion(DEFAULT_TABLES_VERSION);
		setSqlGetTablesVersion(DEFAULT_SQL_GET_TABLES_VERSION);
		setInitSqlResource(new ClassPathResource(DEFAULT_INIT_SQL, getClass()));

	}

	@Override
	public Class<? extends GeneralLocationDatum> getDatumType() {
		return GeneralLocationDatum.class;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED, noRollbackFor = DuplicateKeyException.class)
	public void storeDatum(GeneralLocationDatum datum) {
		try {
			storeDomainObject(datum);
		} catch ( DuplicateKeyException e ) {
			List<GeneralLocationDatum> existing = findDatum(SQL_RESOURCE_FIND_FOR_PRIMARY_KEY,
					preparedStatementSetterForPrimaryKey(datum.getCreated(), datum.getSourceId()),
					rowMapper());
			if ( existing.size() > 0 ) {
				// only update if the samples have changed
				GeneralDatumSamples existingSamples = existing.get(0).getSamples();
				GeneralDatumSamples newSamples = datum.getSamples();
				if ( !newSamples.equals(existingSamples) ) {
					updateDomainObject(datum, getSqlResource(SQL_RESOURCE_UPDATE_DATA));
				} else {
					log.debug("Datum unchanged; not persisted: {}", datum);
				}
			}
		}
	}

	@Override
	protected void setUpdateStatementValues(GeneralLocationDatum datum, PreparedStatement ps)
			throws SQLException {
		int col = 1;
		ps.setString(col++, jsonForSamples(datum));
		ps.setTimestamp(col++, new Timestamp(datum.getCreated().getTime()));
		ps.setString(col++, datum.getSourceId());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setDatumUploaded(final GeneralLocationDatum datum, Date date, String destination,
			String trackingId) {
		final long timestamp = (date == null ? System.currentTimeMillis() : date.getTime());
		getJdbcTemplate().update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con
						.prepareStatement(getSqlResource(SQL_RESOURCE_UPDATE_UPLOADED));
				int col = 1;
				ps.setTimestamp(col++, new java.sql.Timestamp(timestamp));
				ps.setTimestamp(col++, new java.sql.Timestamp(datum.getCreated().getTime()));
				ps.setObject(col++, datum.getLocationId());
				ps.setObject(col++, datum.getSourceId());
				return ps;
			}
		});
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteUploadedDataOlderThan(int hours) {
		return deleteUploadedDataOlderThanHours(hours);
	}

	private RowMapper<GeneralLocationDatum> rowMapper() {
		return new RowMapper<GeneralLocationDatum>() {

			@Override
			public GeneralLocationDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
				if ( log.isTraceEnabled() ) {
					log.trace("Handling result row " + rowNum);
				}
				GeneralLocationDatum datum = new GeneralLocationDatum();
				int col = 0;
				datum.setCreated(rs.getTimestamp(++col));
				datum.setLocationId(rs.getLong(++col));
				datum.setSourceId(rs.getString(++col));

				String jdata = rs.getString(++col);
				if ( jdata != null ) {
					GeneralLocationDatumSamples s;
					try {
						s = objectMapper.readValue(jdata, GeneralLocationDatumSamples.class);
						datum.setSamples(s);
					} catch ( IOException e ) {
						log.error("Error deserializing JSON into GeneralLocationDatumSamples: {}",
								e.getMessage());
					}
				}
				return datum;
			}
		};
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<GeneralLocationDatum> getDatumNotUploaded(String destination) {
		return findDatumNotUploaded(rowMapper());
	}

	private String jsonForSamples(GeneralLocationDatum datum) {
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
	protected void setStoreStatementValues(GeneralLocationDatum datum, PreparedStatement ps)
			throws SQLException {
		int col = 0;
		ps.setTimestamp(++col, new java.sql.Timestamp(
				datum.getCreated() == null ? System.currentTimeMillis() : datum.getCreated().getTime()));
		ps.setLong(++col, datum.getLocationId());
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

/* ===================================================================
 * JdbcConsumptionDatumDao.java
 * 
 * Created Dec 4, 2009 10:22:46 AM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 */

package net.solarnetwork.node.dao.jdbc.consumption;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.dao.jdbc.AbstractJdbcDatumDao;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC-based implementation of {@link net.solarnetwork.node.dao.DatumDao} for
 * {@link ConsumptionDatum} domain objects.
 * 
 * <p>
 * Ueses a {@link javax.sql.DataSource} and requires a schema named
 * {@link net.solarnetwork.node.dao.jdbc.JdbcDaoConstants#SCHEMA_NAME} with two
 * tables:
 * {@link net.solarnetwork.node.dao.jdbc.JdbcDaoConstants#TABLE_SETTINGS} to
 * hold settings and {@link #TABLE_CONSUMPTION_DATUM} to hold the actual
 * consumption data.
 * </p>
 * 
 * <p>
 * This class will check to see if the
 * {@link net.solarnetwork.node.dao.jdbc.JdbcDaoConstants#TABLE_SETTINGS} table
 * exists when the {@link #init()} method is called. If it does not, it assumes
 * the database needs to be created and will load a classpath SQL file resource
 * specified by the {@link #getInitSqlResource()}, which should create the
 * tables needed by this class. See the {@code derby-init.sql} resource in this
 * package for an example.
 * </p>
 * 
 * <p>
 * The tables must have a structure similar to this (shown in Apache Derby SQL
 * dialect):
 * </p>
 * 
 * <pre>
 * CREATE TABLE solarnode.sn_settings (
 * 	skey	VARCHAR(64) NOT NULL,
 * 	svalue	VARCHAR(255) NOT NULL,
 * 	PRIMARY KEY (skey)
 * 	)
 * 	
 * CREATE TABLE solarnode.sn_consum_datum (
 * 	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
 * 	source_id 		VARCHAR(255),
 *  price_loc_id	BIGINT,
 * 	amps			DOUBLE,
 * 	voltage			DOUBLE,
 *  watt_hour		BIGINT,
 *  uploaded       TIMESTAMP,
 * 	PRIMARY KEY (id)
 * )
 * </pre>
 * 
 * @author matt
 * @version 1.1
 */
public class JdbcConsumptionDatumDao extends AbstractJdbcDatumDao<ConsumptionDatum> {

	/** The default tables version. */
	public static final int DEFAULT_TABLES_VERSION = 8;

	/** The table name for {@link ConsumptionDatum} data. */
	public static final String TABLE_CONSUMPTION_DATUM = "sn_consum_datum";

	/** The default classpath Resource for the {@code initSqlResource}. */
	public static final String DEFAULT_INIT_SQL = "derby-consumptiondatum-init.sql";

	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String DEFAULT_SQL_GET_TABLES_VERSION = "SELECT svalue FROM solarnode.sn_settings WHERE skey = "
			+ "'solarnode.sn_consum_datum.version'";

	/**
	 * Default constructor.
	 */
	public JdbcConsumptionDatumDao() {
		super();
		setSqlResourcePrefix("derby-consumptiondatum");
		setTableName(TABLE_CONSUMPTION_DATUM);
		setTablesVersion(DEFAULT_TABLES_VERSION);
		setSqlGetTablesVersion(DEFAULT_SQL_GET_TABLES_VERSION);
		setInitSqlResource(new ClassPathResource(DEFAULT_INIT_SQL, getClass()));
	}

	@Override
	public Class<? extends ConsumptionDatum> getDatumType() {
		return ConsumptionDatum.class;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeDatum(ConsumptionDatum datum) {
		storeDomainObject(datum);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setDatumUploaded(ConsumptionDatum datum, Date date, String destination, Long trackingId) {
		updateDatumUpload(datum, date == null ? System.currentTimeMillis() : date.getTime());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteUploadedDataOlderThan(int hours) {
		return deleteUploadedDataOlderThanHours(hours);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<ConsumptionDatum> getDatumNotUploaded(String destination) {
		return findDatumNotUploaded(new RowMapper<ConsumptionDatum>() {

			@Override
			public ConsumptionDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
				if ( log.isTraceEnabled() ) {
					log.trace("Handling result row " + rowNum);
				}
				ConsumptionDatum datum = new ConsumptionDatum();
				int col = 1;
				datum.setCreated(rs.getTimestamp(col++));
				datum.setSourceId(rs.getString(col++));

				Number val = (Number) rs.getObject(col++);
				datum.setLocationId(val == null ? null : val.longValue());

				val = (Number) rs.getObject(col++);
				datum.setVolts(val == null ? null : val.floatValue());

				val = (Number) rs.getObject(col++);
				datum.setAmps(val == null ? null : val.floatValue());

				val = (Number) rs.getObject(col++);
				datum.setWattHourReading(val == null ? null : val.longValue());

				return datum;
			}
		});
	}

	@Override
	protected void setStoreStatementValues(ConsumptionDatum datum, PreparedStatement ps)
			throws SQLException {
		int col = 1;
		ps.setTimestamp(col++,
				new java.sql.Timestamp(datum.getCreated() == null ? System.currentTimeMillis() : datum
						.getCreated().getTime()));
		ps.setString(col++, datum.getSourceId());
		if ( datum.getLocationId() == null ) {
			ps.setNull(col++, Types.BIGINT);
		} else {
			ps.setLong(col++, datum.getLocationId());
		}
		if ( datum.getAmps() == null ) {
			ps.setNull(col++, Types.FLOAT);
		} else {
			ps.setFloat(col++, datum.getAmps());
		}
		if ( datum.getVolts() == null ) {
			ps.setNull(col++, Types.FLOAT);
		} else {
			ps.setFloat(col++, datum.getVolts());
		}
		if ( datum.getWattHourReading() == null ) {
			ps.setNull(col++, Types.BIGINT);
		} else {
			ps.setLong(col++, datum.getWattHourReading());
		}
	}

}

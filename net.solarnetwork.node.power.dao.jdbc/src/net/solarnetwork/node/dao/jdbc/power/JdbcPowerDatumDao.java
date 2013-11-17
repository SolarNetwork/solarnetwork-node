/* ===================================================================
 * JdbcPowerDatumDao.java
 * 
 * Created Dec 1, 2009 4:50:19 PM
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

package net.solarnetwork.node.dao.jdbc.power;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import net.solarnetwork.node.dao.jdbc.AbstractJdbcDatumDao;
import net.solarnetwork.node.power.PowerDatum;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC-based implementation of {@link net.solarnetwork.node.dao.DatumDao} for
 * {@link PowerDatum} domain objects.
 * 
 * @author matt
 * @version 1.2
 */
public class JdbcPowerDatumDao extends AbstractJdbcDatumDao<PowerDatum> {

	/** The default tables version. */
	public static final int DEFAULT_TABLES_VERSION = 11;

	/** The table name for {@link PowerDatum} data. */
	public static final String TABLE_POWER_DATUM = "sn_power_datum";

	/** The table name for PowerDatum upload data. */
	public static final String TABLE_POWER_DATUM_UPLOAD = "sn_power_datum_upload";

	/** The default classpath Resource for the {@code initSqlResource}. */
	public static final String DEFAULT_INIT_SQL = "derby-powerdatum-init.sql";

	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String DEFAULT_SQL_GET_TABLES_VERSION = "SELECT svalue FROM solarnode.sn_settings WHERE skey = "
			+ "'solarnode.sn_power_datum.version'";

	/**
	 * Default constructor.
	 */
	public JdbcPowerDatumDao() {
		super();
		setSqlResourcePrefix("derby-powerdatum");
		setTableName(TABLE_POWER_DATUM);
		setTablesVersion(DEFAULT_TABLES_VERSION);
		setSqlGetTablesVersion(DEFAULT_SQL_GET_TABLES_VERSION);
		setInitSqlResource(new ClassPathResource(DEFAULT_INIT_SQL, getClass()));
	}

	@Override
	public Class<? extends PowerDatum> getDatumType() {
		return PowerDatum.class;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeDatum(PowerDatum datum) {
		storeDomainObject(datum);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setDatumUploaded(PowerDatum datum, Date date, String destination, Long trackingId) {
		updateDatumUpload(datum, date.getTime());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteUploadedDataOlderThan(int hours) {
		return deleteUploadedDataOlderThanHours(hours);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<PowerDatum> getDatumNotUploaded(String destination) {
		return findDatumNotUploaded(new RowMapper<PowerDatum>() {

			@Override
			public PowerDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
				if ( log.isTraceEnabled() ) {
					log.trace("Handling result row " + rowNum);
				}
				PowerDatum datum = new PowerDatum();
				int col = 1;
				datum.setCreated(rs.getTimestamp(col++));
				datum.setSourceId(rs.getString(col++));

				Number val = (Number) rs.getObject(col++);
				datum.setLocationId(val == null ? null : val.longValue());

				val = (Number) rs.getObject(col++);
				datum.setWatts(val == null ? null : val.intValue());

				val = (Number) rs.getObject(col++);
				datum.setBatteryVolts(val == null ? null : val.floatValue());

				val = (Number) rs.getObject(col++);
				datum.setBatteryAmpHours(val == null ? null : val.doubleValue());

				val = (Number) rs.getObject(col++);
				datum.setDcOutputVolts(val == null ? null : val.floatValue());

				val = (Number) rs.getObject(col++);
				datum.setDcOutputAmps(val == null ? null : val.floatValue());

				val = (Number) rs.getObject(col++);
				datum.setAcOutputVolts(val == null ? null : val.floatValue());

				val = (Number) rs.getObject(col++);
				datum.setAcOutputAmps(val == null ? null : val.floatValue());

				val = (Number) rs.getObject(col++);
				datum.setWattHourReading(val == null ? null : val.longValue());

				val = (Number) rs.getObject(col++);
				datum.setAmpHourReading(val == null ? null : val.doubleValue());

				return datum;
			}
		});
	}

	@Override
	protected void setStoreStatementValues(PowerDatum datum, PreparedStatement ps) throws SQLException {
		int col = 1;
		ps.setTimestamp(col++,
				new java.sql.Timestamp(datum.getCreated() == null ? System.currentTimeMillis() : datum
						.getCreated().getTime()));
		ps.setString(col++, datum.getSourceId() == null ? "" : datum.getSourceId());
		if ( datum.getLocationId() == null ) {
			ps.setNull(col++, Types.BIGINT);
		} else {
			ps.setLong(col++, datum.getLocationId());
		}
		if ( datum.getWatts() == null ) {
			ps.setNull(col++, Types.INTEGER);
		} else {
			ps.setInt(col++, datum.getWatts());
		}
		if ( datum.getBatteryVolts() == null ) {
			ps.setNull(col++, Types.FLOAT);
		} else {
			ps.setFloat(col++, datum.getBatteryVolts());
		}
		if ( datum.getBatteryAmpHours() == null ) {
			ps.setNull(col++, Types.DOUBLE);
		} else {
			ps.setDouble(col++, datum.getBatteryAmpHours());
		}
		if ( datum.getDcOutputVolts() == null ) {
			ps.setNull(col++, Types.FLOAT);
		} else {
			ps.setFloat(col++, datum.getDcOutputVolts());
		}
		if ( datum.getDcOutputAmps() == null ) {
			ps.setNull(col++, Types.FLOAT);
		} else {
			ps.setFloat(col++, datum.getDcOutputAmps());
		}
		if ( datum.getAcOutputVolts() == null ) {
			ps.setNull(col++, Types.FLOAT);
		} else {
			ps.setFloat(col++, datum.getAcOutputVolts());
		}
		if ( datum.getAcOutputAmps() == null ) {
			ps.setNull(col++, Types.FLOAT);
		} else {
			ps.setFloat(col++, datum.getAcOutputAmps());
		}
		if ( datum.getWattHourReading() == null ) {
			ps.setNull(col++, Types.BIGINT);
		} else {
			ps.setLong(col++, datum.getWattHourReading());
		}
		if ( datum.getAmpHourReading() == null ) {
			ps.setNull(col++, Types.DOUBLE);
		} else {
			ps.setDouble(col++, datum.getAmpHourReading());
		}
	}

}

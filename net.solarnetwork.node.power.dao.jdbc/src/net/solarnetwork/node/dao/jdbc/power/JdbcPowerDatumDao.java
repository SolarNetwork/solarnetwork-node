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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.dao.jdbc.power;

import static net.solarnetwork.node.dao.jdbc.JdbcDaoConstants.SCHEMA_NAME;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import net.solarnetwork.node.DatumUpload;
import net.solarnetwork.node.dao.jdbc.AbstractJdbcDatumDao;
import net.solarnetwork.node.power.PowerDatum;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC-based implementation of 
 * {@link net.solarnetwork.node.dao.DatumDao} for {@link PowerDatum}
 * domain objects.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class JdbcPowerDatumDao
extends AbstractJdbcDatumDao<PowerDatum> {

	/** The default tables version. */
	public static final int DEFAULT_TABLES_VERSION = 7;
	
	/** The table name for {@link PowerDatum} data. */
	public static final String TABLE_POWER_DATUM = "sn_power_datum";
	
	/** The table name for PowerDatum upload data. */
	public static final String TABLE_POWER_DATUM_UPLOAD = "sn_power_datum_upload";
	
	/** The default classpath Resource for the {@code initSqlResource}. */
	public static final String DEFAULT_INIT_SQL = "derby-powerdatum-init.sql";
	
	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String DEFAULT_SQL_GET_TABLES_VERSION 
		= "SELECT svalue FROM solarnode.sn_settings WHERE skey = "
		+ "'solarnode.sn_power_datum.version'";
	
	private static final String DEFAULT_SQL_INSERT = "INSERT INTO " 
		+SCHEMA_NAME +'.'+TABLE_POWER_DATUM
		+" (source_id, price_loc_id, pv_volts, pv_amps, bat_volts, bat_amp_hrs, dc_out_volts,"
		+" dc_out_amps, ac_out_volts, ac_out_amps, kwatt_hours, " 
		+"amp_hours, error_msg) VALUES"
		+" (?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
	private static final String DEFAULT_SQL_INSERT_UPLOAD = "INSERT INTO " 
		+SCHEMA_NAME +'.'+TABLE_POWER_DATUM_UPLOAD
		+" (power_datum_id, destination, track_id) VALUES (?,?,?)";
	
	private static final String DEFAULT_SQL_DELETE_OLD = "DELETE FROM "
		+SCHEMA_NAME +'.' +TABLE_POWER_DATUM +" where id <= "
		+"(select MAX(pd.id) from " +SCHEMA_NAME +'.' +TABLE_POWER_DATUM 
		+" pd inner join " +SCHEMA_NAME +'.' +TABLE_POWER_DATUM_UPLOAD 
		+" u on u.power_datum_id = pd.id where pd.created < ?)";
	
	/** The default classpath Resource for the {@code findForUploadSqlResource}. */
	public static final String DEFAULT_FIND_FOR_UPLOAD_SQL = "find-powerdatum-for-upload.sql";
	
	/**
	 * Default constructor.
	 */
	public JdbcPowerDatumDao() {
		super();
		setTableName(TABLE_POWER_DATUM);
		setUploadTableName(TABLE_POWER_DATUM_UPLOAD);
		setTablesVersion(DEFAULT_TABLES_VERSION);
		setSqlGetTablesVersion(DEFAULT_SQL_GET_TABLES_VERSION);
		setSqlResourcePrefix("derby-powerdatum");
		setInitSqlResource(new ClassPathResource(DEFAULT_INIT_SQL, getClass()));
		setSqlDeleteOld(DEFAULT_SQL_DELETE_OLD);
		setFindForUploadSqlResource(
				new ClassPathResource(DEFAULT_FIND_FOR_UPLOAD_SQL, getClass()));
		setSqlInsertDatum(DEFAULT_SQL_INSERT);
		setSqlInsertUpload(DEFAULT_SQL_INSERT_UPLOAD);
	}

	public Class<? extends PowerDatum> getDatumType() {
		return PowerDatum.class;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Long storeDatum(PowerDatum datum) {
		return storeDomainObject(datum);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public DatumUpload storeDatumUpload(PowerDatum datum,
			String destination, Long trackingId) {
		return storeNewDatumUpload(datum, destination, trackingId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteUploadedDataOlderThan(int hours) {
		return deleteUploadedDataOlderThanHours(hours);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<PowerDatum> getDatumNotUploaded(String destination) {
		return findDatumNotUploaded(destination, new RowMapper<PowerDatum>() {
			public PowerDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
				if ( log.isTraceEnabled() ) {
					log.trace("Handling result row " +rowNum);
				}
				PowerDatum datum = new PowerDatum();
				int col = 1;
				datum.setId(rs.getLong(col++));
				datum.setSourceId(rs.getString(col++));

				Number val = (Number)rs.getObject(col++);
				datum.setLocationId(val == null ? null : val.longValue());
				
				datum.setCreated(rs.getTimestamp(col++));
				
				val = (Number)rs.getObject(col++);
				datum.setPvVolts(val == null ? null : val.floatValue());
				
				val = (Number)rs.getObject(col++);
				datum.setPvAmps(val == null ? null : val.floatValue());
				
				val = (Number)rs.getObject(col++);
				datum.setBatteryVolts(val == null ? null : val.floatValue());
				
				val = (Number)rs.getObject(col++);
				datum.setBatteryAmpHours(val == null ? null : val.doubleValue());
				
				val = (Number)rs.getObject(col++);
				datum.setDcOutputVolts(val == null ? null : val.floatValue());

				val = (Number)rs.getObject(col++);
				datum.setDcOutputAmps(val == null ? null : val.floatValue());
				
				val = (Number)rs.getObject(col++);
				datum.setAcOutputVolts(val == null ? null : val.floatValue());
				
				val = (Number)rs.getObject(col++);
				datum.setAcOutputAmps(val == null ? null : val.floatValue());
				
				val = (Number)rs.getObject(col++);
				datum.setKWattHoursToday(val == null ? null : val.doubleValue());
				
				val = (Number)rs.getObject(col++);
				datum.setAmpHoursToday(val == null ? null : val.doubleValue());
				
				datum.setErrorMessage(rs.getString(col++));
				return datum;
			}
		});
	}

	@Override
	protected void setStoreStatementValues(PowerDatum datum, PreparedStatement ps) 
	throws SQLException {
		int col = 1;
		ps.setString(col++, datum.getSourceId());
		if ( datum.getLocationId() == null ) {
			ps.setNull(col++, Types.BIGINT);
		} else {
			ps.setLong(col++, datum.getLocationId());
		}
		if ( datum.getPvVolts() == null ) {
			ps.setNull(col++, Types.FLOAT);
		} else {
			ps.setFloat(col++, datum.getPvVolts());
		}
		if ( datum.getPvAmps() == null ) {
			ps.setNull(col++, Types.FLOAT);
		} else {
			ps.setFloat(col++, datum.getPvAmps());
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
		if ( datum.getKWattHoursToday() == null ) {
			ps.setNull(col++, Types.DOUBLE);
		} else {
			ps.setDouble(col++, datum.getKWattHoursToday());
		}
		if ( datum.getAmpHoursToday() == null ) {
			ps.setNull(col++, Types.DOUBLE);
		} else {
			ps.setDouble(col++, datum.getAmpHoursToday());
		}
		if ( datum.getErrorMessage() == null ) {
			ps.setNull(col++, Types.VARCHAR);
		} else {
			ps.setString(col++, datum.getErrorMessage());
		}
	}

}

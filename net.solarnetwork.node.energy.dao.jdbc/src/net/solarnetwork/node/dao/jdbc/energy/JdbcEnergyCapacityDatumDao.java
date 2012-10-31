/* ==================================================================
 * JdbcEnergyCapacityDatumDao.java - Oct 10, 2011 2:20:32 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.dao.jdbc.energy;

import static net.solarnetwork.node.dao.jdbc.JdbcDaoConstants.SCHEMA_NAME;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import net.solarnetwork.node.DatumUpload;
import net.solarnetwork.node.dao.jdbc.AbstractJdbcDatumDao;
import net.solarnetwork.node.energy.EnergyCapacityDatum;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC-based implementation of 
 * {@link net.solarnetwork.node.dao.DatumDao} for {@link EnergyCapacityDatum}
 * domain objects.
 * 
 * @author matt
 * @version $Revision$
 */
public class JdbcEnergyCapacityDatumDao extends AbstractJdbcDatumDao<EnergyCapacityDatum> {

	/** The default tables version. */
	public static final int DEFAULT_TABLES_VERSION = 1;
	
	/** The table name for {@link EnergyCapacityDatum} data. */
	public static final String TABLE_ENERGY_CAPACITY_DATUM = "sn_energy_capacity_datum";
	
	/** The table name for {@link EnergyCapacityDatum} upload data. */
	public static final String TABLE_ENERGY_CAPACITY_DATUM_UPLOAD = "sn_energy_capacity_datum_upload";

	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String DEFAULT_SQL_GET_TABLES_VERSION 
		= "SELECT svalue FROM solarnode.sn_settings WHERE skey = "
		+ "'solarnode.sn_energy_capacity_datum.version'";
	
	private static final String DEFAULT_SQL_INSERT_UPLOAD = "INSERT INTO " 
		+SCHEMA_NAME +'.'+TABLE_ENERGY_CAPACITY_DATUM_UPLOAD
		+" (datum_id, destination, track_id) VALUES (?,?,?)";
	
	private static final String DEFAULT_SQL_DELETE_OLD = "DELETE FROM "
		+SCHEMA_NAME +'.' +TABLE_ENERGY_CAPACITY_DATUM +" where id <= "
		+"(select MAX(d.id) from " +SCHEMA_NAME +'.' +TABLE_ENERGY_CAPACITY_DATUM 
		+" d inner join " +SCHEMA_NAME +'.' +TABLE_ENERGY_CAPACITY_DATUM_UPLOAD 
		+" u on u.datum_id = d.id where d.created < ?)";
		
	/**
	 * Default constructor.
	 */
	public JdbcEnergyCapacityDatumDao() {
		super();
		setTableName(TABLE_ENERGY_CAPACITY_DATUM);
		setUploadTableName(TABLE_ENERGY_CAPACITY_DATUM_UPLOAD);
		setTablesVersion(DEFAULT_TABLES_VERSION);
		setSqlGetTablesVersion(DEFAULT_SQL_GET_TABLES_VERSION);
		setSqlResourcePrefix("derby-energycapacitydatum");
		setInitSqlResource(new ClassPathResource(getSqlResourcePrefix() +"-init.sql", getClass()));

		setSqlInsertDatum(getSqlResource(new ClassPathResource(getSqlResourcePrefix() +"-insert.sql", getClass())));
		setSqlInsertUpload(DEFAULT_SQL_INSERT_UPLOAD);
		setSqlDeleteOld(DEFAULT_SQL_DELETE_OLD);
		setFindForUploadSqlResource(
				new ClassPathResource("find-energycapacitydatum-for-upload.sql", getClass()));
	}

	@Override
	public Class<? extends EnergyCapacityDatum> getDatumType() {
		return EnergyCapacityDatum.class;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Long storeDatum(EnergyCapacityDatum datum) {
		return storeDomainObject(datum);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public DatumUpload storeDatumUpload(EnergyCapacityDatum datum,
			String destination, Long trackingId) {
		return storeNewDatumUpload(datum, destination, trackingId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteUploadedDataOlderThan(int hours) {
		return deleteUploadedDataOlderThanHours(hours);
	}
	
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<EnergyCapacityDatum> getDatumNotUploaded(String destination) {
		return findDatumNotUploaded(destination, new RowMapper<EnergyCapacityDatum>() {
			public EnergyCapacityDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
				if ( log.isTraceEnabled() ) {
					log.trace("Handling result row " +rowNum);
				}
				EnergyCapacityDatum datum = new EnergyCapacityDatum();
				int col = 1;
				datum.setId(rs.getLong(col++));

				datum.setCreated(rs.getTimestamp(col++));
				
				datum.setSourceId(rs.getString(col++));
				
				Number val = (Number)rs.getObject(col++);
				datum.setVolts(val == null ? null : val.floatValue());
				
				val = (Number)rs.getObject(col++);
				datum.setAmpHours(val == null ? null : val.doubleValue());
				
				val = (Number)rs.getObject(col++);
				datum.setWattHours(val == null ? null : val.doubleValue());

				return datum;
			}
		});
	}

	@Override
	protected void setStoreStatementValues(EnergyCapacityDatum datum, PreparedStatement ps) 
	throws SQLException {
		int col = 1;
		ps.setString(col++, datum.getSourceId());
		if ( datum.getVolts() == null ) {
			ps.setNull(col++, Types.REAL);
		} else {
			ps.setFloat(col++, datum.getVolts());
		}
		if ( datum.getAmpHours() == null ) {
			ps.setNull(col++, Types.DOUBLE);
		} else {
			ps.setDouble(col++, datum.getAmpHours());
		}
		if ( datum.getWattHours() == null ) {
			ps.setNull(col++, Types.DOUBLE);
		} else {
			ps.setDouble(col++, datum.getWattHours());
		}
	}

}

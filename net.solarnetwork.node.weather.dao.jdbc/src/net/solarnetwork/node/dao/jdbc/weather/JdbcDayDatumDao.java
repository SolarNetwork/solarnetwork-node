/* ==================================================================
 * JdbcDayDatumDao.java - Feb 15, 2011 9:11:34 AM
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
 */

package net.solarnetwork.node.dao.jdbc.weather;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import net.solarnetwork.node.dao.jdbc.AbstractJdbcDatumDao;
import net.solarnetwork.node.weather.DayDatum;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC-based implementation of {@link net.solarnetwork.node.dao.DatumDao} for
 * {@link PriceDatum} domain objects.
 * 
 * <p>
 * Uses a {@link javax.sql.DataSource} and requires a schema named
 * {@link net.solarnetwork.node.dao.jdbc.JdbcDaoConstants#SCHEMA_NAME} with two
 * tables:
 * {@link net.solarnetwork.node.dao.jdbc.JdbcDaoConstants#TABLE_SETTINGS} to
 * hold settings and {@link #TABLE_PRICE_DATUM} to hold the actual price data.
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
 * @author matt
 * @version 1.2
 */
public class JdbcDayDatumDao extends AbstractJdbcDatumDao<DayDatum> {

	/** The default tables version. */
	public static final int DEFAULT_TABLES_VERSION = 3;

	/** The table name for {@link DayDatum} data. */
	public static final String TABLE_DAY_DATUM = "sn_day_datum";

	/** The default classpath Resource for the {@code initSqlResource}. */
	public static final String DEFAULT_INIT_SQL = "derby-daydatum-init.sql";

	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String DEFAULT_SQL_GET_TABLES_VERSION = "SELECT svalue FROM solarnode.sn_settings WHERE skey = "
			+ "'solarnode.sn_day_datum.version'";

	/**
	 * Default constructor.
	 */
	public JdbcDayDatumDao() {
		super();
		setSqlResourcePrefix("derby-daydatum");
		setTableName(TABLE_DAY_DATUM);
		setTablesVersion(DEFAULT_TABLES_VERSION);
		setSqlGetTablesVersion(DEFAULT_SQL_GET_TABLES_VERSION);
		setInitSqlResource(new ClassPathResource(DEFAULT_INIT_SQL, getClass()));
	}

	@Override
	public Class<? extends DayDatum> getDatumType() {
		return DayDatum.class;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeDatum(DayDatum datum) {
		storeDomainObject(datum);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<DayDatum> getDatumNotUploaded(String destination) {
		return findDatumNotUploaded(new RowMapper<DayDatum>() {

			@Override
			public DayDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
				if ( log.isTraceEnabled() ) {
					log.trace("Handling result row " + rowNum);
				}
				DayDatum datum = new DayDatum();
				int col = 1;
				datum.setCreated(rs.getTimestamp(col++));
				datum.setLocationId(rs.getLong(col++));
				datum.setTimeZoneId(rs.getString(col++));

				Number val = (Number) rs.getObject(col++);
				datum.setLatitude(val == null ? null : val.doubleValue());

				val = (Number) rs.getObject(col++);
				datum.setLongitude(val == null ? null : val.doubleValue());

				datum.setSunrise(rs.getTime(col++));
				datum.setSunset(rs.getTime(col++));

				return datum;
			}
		});
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setDatumUploaded(DayDatum datum, Date date, String destination, Long trackingId) {
		updateDatumUpload(datum.getCreated().getTime(), datum.getLocationId(), date.getTime());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteUploadedDataOlderThan(int hours) {
		return deleteUploadedDataOlderThanHours(hours);
	}

	@Override
	protected void setStoreStatementValues(DayDatum datum, PreparedStatement ps) throws SQLException {
		int col = 1;
		ps.setTimestamp(col++,
				new java.sql.Timestamp(datum.getCreated() == null ? System.currentTimeMillis() : datum
						.getCreated().getTime()));
		ps.setLong(col++, datum.getLocationId());
		if ( datum.getTimeZoneId() == null ) {
			ps.setNull(col++, Types.VARCHAR);
		} else {
			ps.setString(col++, datum.getTimeZoneId());
		}
		if ( datum.getLatitude() == null ) {
			ps.setNull(col++, Types.DOUBLE);
		} else {
			ps.setDouble(col++, datum.getLatitude());
		}
		if ( datum.getLongitude() == null ) {
			ps.setNull(col++, Types.DOUBLE);
		} else {
			ps.setDouble(col++, datum.getLongitude());
		}
		if ( datum.getSunrise() == null ) {
			ps.setNull(col++, Types.TIME);
		} else {
			ps.setTime(col++, new java.sql.Time(datum.getSunrise().getTime()));
		}
		if ( datum.getSunset() == null ) {
			ps.setNull(col++, Types.TIME);
		} else {
			ps.setTime(col++, new java.sql.Time(datum.getSunset().getTime()));
		}
	}
}

/* ==================================================================
 * JdbcWeatherDatumDao.java - Feb 15, 2011 9:11:34 AM
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
import net.solarnetwork.node.weather.WeatherDatum;
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
 * @version 1.4
 */
public class JdbcWeatherDatumDao extends AbstractJdbcDatumDao<WeatherDatum> {

	/** The default tables version. */
	public static final int DEFAULT_TABLES_VERSION = 4;

	/** The table name for {@link WeatherDatum} data. */
	public static final String TABLE_WEATHER_DATUM = "sn_weather_datum";

	/** The default classpath Resource for the {@code initSqlResource}. */
	public static final String DEFAULT_INIT_SQL = "derby-weatherdatum-init.sql";

	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String DEFAULT_SQL_GET_TABLES_VERSION = "SELECT svalue FROM solarnode.sn_settings WHERE skey = "
			+ "'solarnode.sn_weather_datum.version'";

	/**
	 * Default constructor.
	 */
	public JdbcWeatherDatumDao() {
		super();
		setSqlResourcePrefix("derby-weatherdatum");
		setTableName(TABLE_WEATHER_DATUM);
		setTablesVersion(DEFAULT_TABLES_VERSION);
		setSqlGetTablesVersion(DEFAULT_SQL_GET_TABLES_VERSION);
		setInitSqlResource(new ClassPathResource(DEFAULT_INIT_SQL, getClass()));
	}

	@Override
	public Class<? extends WeatherDatum> getDatumType() {
		return WeatherDatum.class;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeDatum(WeatherDatum datum) {
		storeDomainObject(datum);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<WeatherDatum> getDatumNotUploaded(String destination) {
		return findDatumNotUploaded(new RowMapper<WeatherDatum>() {

			@Override
			public WeatherDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
				if ( log.isTraceEnabled() ) {
					log.trace("Handling result row " + rowNum);
				}
				WeatherDatum datum = new WeatherDatum();
				int col = 1;
				datum.setCreated(rs.getTimestamp(col++));
				datum.setLocationId(rs.getLong(col++));
				datum.setSkyConditions(rs.getString(col++));

				Number val = (Number) rs.getObject(col++);
				datum.setTemperatureCelsius(val == null ? null : val.doubleValue());

				val = (Number) rs.getObject(col++);
				datum.setHumidity(val == null ? null : val.doubleValue());

				val = (Number) rs.getObject(col++);
				datum.setBarometricPressure(val == null ? null : val.doubleValue());

				datum.setBarometerDelta(rs.getString(col++));

				val = (Number) rs.getObject(col++);
				datum.setVisibility(val == null ? null : val.doubleValue());

				val = (Number) rs.getObject(col++);
				datum.setUvIndex(val == null ? null : val.intValue());

				val = (Number) rs.getObject(col++);
				datum.setDewPoint(val == null ? null : val.doubleValue());

				return datum;
			}
		});
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setDatumUploaded(WeatherDatum datum, Date date, String destination, String trackingId) {
		updateDatumUpload(datum.getCreated().getTime(), datum.getLocationId(), date.getTime());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteUploadedDataOlderThan(int hours) {
		return deleteUploadedDataOlderThanHours(hours);
	}

	@Override
	protected void setStoreStatementValues(WeatherDatum datum, PreparedStatement ps) throws SQLException {
		int col = 1;
		ps.setTimestamp(col++,
				new java.sql.Timestamp(datum.getCreated() == null ? System.currentTimeMillis() : datum
						.getCreated().getTime()));
		ps.setLong(col++, datum.getLocationId());
		if ( datum.getSkyConditions() == null ) {
			ps.setNull(col++, Types.VARCHAR);
		} else {
			ps.setString(col++, datum.getSkyConditions());
		}
		if ( datum.getTemperatureCelsius() == null ) {
			ps.setNull(col++, Types.DOUBLE);
		} else {
			ps.setDouble(col++, datum.getTemperatureCelsius());
		}
		if ( datum.getHumidity() == null ) {
			ps.setNull(col++, Types.DOUBLE);
		} else {
			ps.setDouble(col++, datum.getHumidity());
		}
		if ( datum.getBarometricPressure() == null ) {
			ps.setNull(col++, Types.DOUBLE);
		} else {
			ps.setDouble(col++, datum.getBarometricPressure());
		}
		if ( datum.getBarometerDelta() == null ) {
			ps.setNull(col++, Types.VARCHAR);
		} else {
			ps.setString(col++, datum.getBarometerDelta());
		}
		if ( datum.getVisibility() == null ) {
			ps.setNull(col++, Types.DOUBLE);
		} else {
			ps.setDouble(col++, datum.getVisibility());
		}
		if ( datum.getUvIndex() == null ) {
			ps.setNull(col++, Types.INTEGER);
		} else {
			ps.setInt(col++, datum.getUvIndex());
		}
		if ( datum.getDewPoint() == null ) {
			ps.setNull(col++, Types.DOUBLE);
		} else {
			ps.setDouble(col++, datum.getDewPoint());
		}
	}
}

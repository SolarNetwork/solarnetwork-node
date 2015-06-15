/* ==================================================================
 * JdbcAuthorizationDao.java - 8/06/2015 8:57:04 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.datatype.XMLGregorianCalendar;
import net.solarnetwork.node.ocpp.Authorization;
import net.solarnetwork.node.ocpp.AuthorizationDao;
import ocpp.v15.cs.AuthorizationStatus;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC implementation of {@link AuthorizationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcAuthorizationDao extends AbstractOcppJdbcDao<Authorization> implements AuthorizationDao {

	/** The default tables version. */
	public static final int TABLES_VERSION = 1;

	/** The table name for {@link Authorization} data. */
	public static final String TABLE_NAME = "ocpp_auth";

	/** The default classpath Resource for the {@code initSqlResource}. */
	public static final String INIT_SQL = "derby-auth-init.sql";

	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String SQL_GET_TABLES_VERSION = "SELECT svalue FROM solarnode.sn_settings WHERE skey = "
			+ "'solarnode.ocpp_auth.version'";

	public static final String SQL_INSERT = "insert";
	public static final String SQL_UPDATE = "update";
	public static final String SQL_GET_BY_PK = "get-pk";
	public static final String SQL_DELETE_EXPIRED = "delete-expired";
	public static final String SQL_FIND_STATUS_COUNTS = "statuscounts";

	/**
	 * Default constructor.
	 */
	public JdbcAuthorizationDao() {
		super();
		setSqlResourcePrefix("derby-auth");
		setTableName(TABLE_NAME);
		setTablesVersion(TABLES_VERSION);
		setSqlGetTablesVersion(SQL_GET_TABLES_VERSION);
		setInitSqlResource(new ClassPathResource(INIT_SQL, getClass()));
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeAuthorization(final Authorization auth) {
		Authorization existing = getAuthorization(auth.getIdTag());
		if ( existing != null ) {
			updateDomainObject(auth, getSqlResource(SQL_UPDATE));
		} else {
			insertDomainObject(auth, getSqlResource(SQL_INSERT));
		}
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Authorization getAuthorization(String idTag) {
		List<Authorization> results = getJdbcTemplate().query(getSqlResource(SQL_GET_BY_PK),
				new AuthorizationRowMapper(), idTag);
		if ( results != null && results.size() > 0 ) {
			return results.get(0);
		}
		return null;
	}

	@Override
	protected void setStoreStatementValues(Authorization auth, PreparedStatement ps) throws SQLException {
		// Row order is: (created, idtag, parent_idtag, status, expires)
		ps.setTimestamp(1, new Timestamp(auth.getCreated() != null ? auth.getCreated().getTime()
				: System.currentTimeMillis()), utcCalendar);
		ps.setString(2, auth.getIdTag());
		ps.setString(3, auth.getParentIdTag());
		ps.setString(4, auth.getStatus().toString());
		if ( auth.getExpiryDate() != null ) {
			// store ts in UTC time zone
			Calendar cal = calendarForXMLDate(auth.getExpiryDate());
			Timestamp ts = new Timestamp(cal.getTimeInMillis());
			ps.setTimestamp(5, ts, cal);
		} else {
			ps.setNull(5, Types.TIMESTAMP);
		}
	}

	@Override
	protected void setUpdateStatementValues(Authorization auth, PreparedStatement ps)
			throws SQLException {
		ps.setString(1, auth.getParentIdTag());
		ps.setString(2, auth.getStatus().toString());
		if ( auth.getExpiryDate() != null ) {
			// store ts in UTC time zone
			Calendar cal = calendarForXMLDate(auth.getExpiryDate());
			Timestamp ts = new Timestamp(cal.getTimeInMillis());
			ps.setTimestamp(3, ts, cal);
		} else {
			ps.setNull(3, Types.TIMESTAMP);
		}
		ps.setString(4, auth.getIdTag());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteExpiredAuthorizations(Date olderThanDate) {
		final Calendar cal = calendarForDate(olderThanDate != null ? olderThanDate : new Date());
		final Timestamp ts = new Timestamp(cal.getTimeInMillis());
		return getJdbcTemplate().update(getSqlResource(SQL_DELETE_EXPIRED),
				new PreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps) throws SQLException {
						ps.setTimestamp(1, ts, cal);
					}

				});
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Map<AuthorizationStatus, Integer> statusCounts() {
		final Map<AuthorizationStatus, Integer> result = new LinkedHashMap<AuthorizationStatus, Integer>(
				AuthorizationStatus.values().length);
		getJdbcTemplate().query(getSqlResource(SQL_FIND_STATUS_COUNTS), new RowCallbackHandler() {

			@Override
			public void processRow(ResultSet rs) throws SQLException {
				AuthorizationStatus key = AuthorizationStatus.valueOf(rs.getString(1));
				int count = rs.getInt(2);
				result.put(key, count);
			}

		});
		return result;
	}

	private final class AuthorizationRowMapper implements RowMapper<Authorization> {

		private XMLGregorianCalendar xmlTimestamp(Timestamp ts) {
			if ( ts == null ) {
				return null;
			}
			GregorianCalendar cal = new GregorianCalendar(utcCalendar.getTimeZone());
			cal.setTimeInMillis(ts.getTime());
			return datatypeFactory.newXMLGregorianCalendar(cal);
		}

		@Override
		public Authorization mapRow(ResultSet rs, int rowNum) throws SQLException {
			Authorization auth = new Authorization();
			// Row order is: created, idtag, parent_idtag, status, expires
			Timestamp ts = rs.getTimestamp(1, utcCalendar);
			if ( ts != null ) {
				auth.setCreated(new Date(ts.getTime()));
			}
			auth.setIdTag(rs.getString(2));
			auth.setParentIdTag(rs.getString(3));
			auth.setStatus(AuthorizationStatus.valueOf(rs.getString(4)));
			auth.setExpiryDate(xmlTimestamp(rs.getTimestamp(5, utcCalendar)));
			return auth;
		}
	}

}

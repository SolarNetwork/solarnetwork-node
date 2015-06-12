/* ==================================================================
 * JdbcChargeSessionDao.java - 9/06/2015 1:03:00 pm
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import net.solarnetwork.node.ocpp.ChargeSession;
import net.solarnetwork.node.ocpp.ChargeSessionDao;
import net.solarnetwork.node.ocpp.ChargeSessionMeterReading;
import ocpp.v15.cs.AuthorizationStatus;
import ocpp.v15.cs.Location;
import ocpp.v15.cs.Measurand;
import ocpp.v15.cs.MeterValue.Value;
import ocpp.v15.cs.ReadingContext;
import ocpp.v15.cs.UnitOfMeasure;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC implementation of {@link ChargeSessionDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargeSessionDao extends AbstractOcppJdbcDao<ChargeSession> implements ChargeSessionDao {

	/** The default tables version. */
	public static final int TABLES_VERSION = 1;

	/** The table name for {@link ChargeSession} data. */
	public static final String TABLE_NAME = "ocpp_charge";

	/** The default classpath Resource for the {@code initSqlResource}. */
	public static final String INIT_SQL = "derby-charge-init.sql";

	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String SQL_GET_TABLES_VERSION = "SELECT svalue FROM solarnode.sn_settings WHERE skey = "
			+ "'solarnode.ocpp_charge.version'";

	public static final String SQL_INSERT = "insert";
	public static final String SQL_UPDATE = "update";
	public static final String SQL_GET_BY_PK = "get-pk";
	public static final String SQL_GET_BY_IDTAG = "get-idtag";
	public static final String SQL_GET_INCOMPLETE_BY_SOCKETID = "get-socket-incomplete";
	public static final String SQL_GET_INCOMPLETE_SESSIONS = "get-incomplete";
	public static final String SQL_GET_NEEDING_POSTING = "get-needsposting";
	public static final String SQL_DELETE_COMPLETED = "delete-completed";
	public static final String SQL_DELETE_INCOMPLETE = "delete-incomplete";
	public static final String SQL_INSERT_READING = "insert-reading";
	public static final String SQL_GET_READINGS_FOR_SESSION = "get-readings-sessionid";

	/**
	 * Constructor.
	 */
	public JdbcChargeSessionDao() {
		super();
		setSqlResourcePrefix("derby-charge");
		setTableName(TABLE_NAME);
		setTablesVersion(TABLES_VERSION);
		setSqlGetTablesVersion(SQL_GET_TABLES_VERSION);
		setInitSqlResource(new ClassPathResource(INIT_SQL, getClass()));
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String storeChargeSession(final ChargeSession session) {
		UUID pk = null;
		if ( session.getSessionId() == null ) {
			pk = UUID.randomUUID();
		} else {
			pk = UUID.fromString(session.getSessionId());
		}
		ChargeSession existing = getChargeSession(pk);
		if ( existing != null ) {
			updateDomainObject(session, getSqlResource(SQL_UPDATE));
		} else {
			if ( session.getSessionId() == null ) {
				session.setSessionId(pk.toString());
			}
			insertDomainObject(session, getSqlResource(SQL_INSERT));
		}
		return session.getSessionId();
	}

	@Override
	protected void setStoreStatementValues(ChargeSession session, PreparedStatement ps)
			throws SQLException {
		// Row order is: (created, sessid_hi, sessid_lo, idtag, socketid, xid, ended, posted)
		ps.setTimestamp(1, new Timestamp(session.getCreated() != null ? session.getCreated().getTime()
				: System.currentTimeMillis()), utcCalendar);
		UUID pk = UUID.fromString(session.getSessionId());
		ps.setLong(2, pk.getMostSignificantBits());
		ps.setLong(3, pk.getLeastSignificantBits());
		ps.setString(4, session.getIdTag());
		ps.setString(5, session.getSocketId());
		ps.setString(6, session.getStatus() != null ? session.getStatus().toString() : null);
		if ( session.getTransactionId() != null ) {
			ps.setLong(7, session.getTransactionId().longValue());
		} else {
			ps.setNull(7, Types.BIGINT);
		}
		if ( session.getEnded() != null ) {
			// store ts in UTC time zone
			Calendar cal = calendarForDate(session.getEnded());
			Timestamp ts = new Timestamp(cal.getTimeInMillis());
			ps.setTimestamp(8, ts, cal);
		} else {
			ps.setNull(8, Types.TIMESTAMP);
		}
		if ( session.getPosted() != null ) {
			// store ts in UTC time zone
			Calendar cal = calendarForDate(session.getPosted());
			Timestamp ts = new Timestamp(cal.getTimeInMillis());
			ps.setTimestamp(9, ts, cal);
		} else {
			ps.setNull(9, Types.TIMESTAMP);
		}
	}

	private int updateDomainObject(final ChargeSession session, final String sql) {
		return getJdbcTemplate().update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				// cols: auth_status = ?, xid = ?, ended = ?, posted = ?
				//       sessionid_hi, sessionid_lo
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setString(1, session.getStatus() != null ? session.getStatus().toString() : null);
				if ( session.getTransactionId() == null ) {
					ps.setNull(2, Types.BIGINT);
				} else {
					ps.setLong(2, session.getTransactionId().longValue());
				}
				if ( session.getEnded() != null ) {
					// store ts in UTC time zone
					Calendar cal = calendarForDate(session.getEnded());
					Timestamp ts = new Timestamp(cal.getTimeInMillis());
					ps.setTimestamp(3, ts, cal);
				} else {
					ps.setNull(3, Types.TIMESTAMP);
				}
				if ( session.getPosted() != null ) {
					// store ts in UTC time zone
					Calendar cal = calendarForDate(session.getPosted());
					Timestamp ts = new Timestamp(cal.getTimeInMillis());
					ps.setTimestamp(4, ts, cal);
				} else {
					ps.setNull(4, Types.TIMESTAMP);
				}

				UUID pk = UUID.fromString(session.getSessionId());
				ps.setLong(5, pk.getMostSignificantBits());
				ps.setLong(6, pk.getLeastSignificantBits());
				return ps;
			}
		});
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ChargeSession getChargeSession(String sessionId) {
		UUID pk = UUID.fromString(sessionId);
		return getChargeSession(pk);
	}

	private ChargeSession getChargeSession(UUID pk) {
		List<ChargeSession> results = getJdbcTemplate().query(getSqlResource(SQL_GET_BY_PK),
				new ChargeSessionRowMapper(), pk.getMostSignificantBits(), pk.getLeastSignificantBits());
		if ( results != null && results.size() > 0 ) {
			return results.get(0);
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ChargeSession getIncompleteChargeSessionForSocket(final String socketId) {
		List<ChargeSession> results = getJdbcTemplate().query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {

				PreparedStatement stmt = con.prepareStatement(
						getSqlResource(SQL_GET_INCOMPLETE_BY_SOCKETID), ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_READ_ONLY);
				stmt.setMaxRows(1);
				stmt.setString(1, socketId);
				return stmt;
			}
		}, new ChargeSessionRowMapper());
		if ( results != null && results.size() > 0 ) {
			return results.get(0);
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<ChargeSession> getIncompleteChargeSessions() {
		return getJdbcTemplate().query(getSqlResource(SQL_GET_INCOMPLETE_SESSIONS),
				new ChargeSessionRowMapper());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<ChargeSession> getChargeSessionsNeedingPosting(final int max) {
		return getJdbcTemplate().query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {

				PreparedStatement stmt = con.prepareStatement(getSqlResource(SQL_GET_NEEDING_POSTING),
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				stmt.setMaxRows(max);
				return stmt;
			}
		}, new ChargeSessionRowMapper());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void addMeterReadings(final String sessionId, final Date date, final Iterable<Value> readings) {
		if ( readings == null ) {
			return;
		}
		final UUID pk = UUID.fromString(sessionId);
		final Timestamp ts = new Timestamp(date != null ? date.getTime() : System.currentTimeMillis());
		getJdbcTemplate().execute(getSqlResource(SQL_INSERT_READING),
				new PreparedStatementCallback<Object>() {

					@Override
					public Object doInPreparedStatement(PreparedStatement ps) throws SQLException,
							DataAccessException {
						// cols: (created, sessid_hi, sessid_lo, measurand, reading, context, location, unit)
						ps.setTimestamp(1, ts);
						ps.setLong(2, pk.getMostSignificantBits());
						ps.setLong(3, pk.getLeastSignificantBits());
						for ( Value v : readings ) {
							ps.setString(4, v.getMeasurand().toString());
							ps.setString(5, v.getValue());
							ps.setString(6, v.getContext() != null ? v.getContext().toString() : null);
							ps.setString(7, v.getLocation() != null ? v.getLocation().toString() : null);
							ps.setString(8, v.getUnit() != null ? v.getUnit().toString() : null);
							ps.executeUpdate();
						}
						return null;
					}
				});
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteCompletedChargeSessions(Date olderThanDate) {
		final Calendar cal = calendarForDate(olderThanDate != null ? olderThanDate : new Date());
		final Timestamp ts = new Timestamp(cal.getTimeInMillis());
		return getJdbcTemplate().update(getSqlResource(SQL_DELETE_COMPLETED),
				new PreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps) throws SQLException {
						ps.setTimestamp(1, ts, cal);
					}

				});
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteIncompletedChargeSessions(Date olderThanDate) {
		final Calendar cal = calendarForDate(olderThanDate != null ? olderThanDate : new Date());
		final Timestamp ts = new Timestamp(cal.getTimeInMillis());
		return getJdbcTemplate().update(getSqlResource(SQL_DELETE_INCOMPLETE),
				new PreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps) throws SQLException {
						ps.setTimestamp(1, ts, cal);
					}

				});
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<ChargeSessionMeterReading> findMeterReadingsForSession(String sessionId) {
		UUID pk = UUID.fromString(sessionId);
		return getJdbcTemplate().query(getSqlResource(SQL_GET_READINGS_FOR_SESSION),
				new Object[] { pk.getMostSignificantBits(), pk.getLeastSignificantBits() },
				new ChargeSessionMeterReadingRowMapper(sessionId));
	}

	private final class ChargeSessionRowMapper implements RowMapper<ChargeSession> {

		@Override
		public ChargeSession mapRow(ResultSet rs, int rowNum) throws SQLException {
			ChargeSession row = new ChargeSession();
			// Row order is: created, sessid_hi, sessid_lo, idtag, socketid, auth_status, xid, ended, posted
			Timestamp ts = rs.getTimestamp(1, utcCalendar);
			if ( ts != null ) {
				row.setCreated(new Date(ts.getTime()));
			}
			row.setSessionId(new UUID(rs.getLong(2), rs.getLong(3)).toString());
			row.setIdTag(rs.getString(4));
			row.setSocketId(rs.getString(5));

			String s = rs.getString(6);
			if ( s != null ) {
				row.setStatus(AuthorizationStatus.valueOf(s));
			}

			Number n = (Number) rs.getObject(7);
			if ( n != null ) {
				row.setTransactionId(n.intValue());
			}

			ts = rs.getTimestamp(8, utcCalendar);
			if ( ts != null ) {
				row.setEnded(new Date(ts.getTime()));
			}

			ts = rs.getTimestamp(9, utcCalendar);
			if ( ts != null ) {
				row.setPosted(new Date(ts.getTime()));
			}

			return row;
		}
	}

	private final class ChargeSessionMeterReadingRowMapper implements
			RowMapper<ChargeSessionMeterReading> {

		private final String sessionId;

		private ChargeSessionMeterReadingRowMapper(String sessionId) {
			super();
			this.sessionId = sessionId;
		}

		@Override
		public ChargeSessionMeterReading mapRow(ResultSet rs, int rowNum) throws SQLException {
			ChargeSessionMeterReading row = new ChargeSessionMeterReading();
			row.setSessionId(sessionId);
			// Row order is: created, measurand, reading, context, location, unit
			Timestamp ts = rs.getTimestamp(1, utcCalendar);
			row.setTs(new Date(ts.getTime()));
			row.setMeasurand(Measurand.valueOf(rs.getString(2)));
			row.setValue(rs.getString(3));

			String s = rs.getString(4);
			if ( s != null ) {
				row.setContext(ReadingContext.valueOf(s));
			}

			s = rs.getString(5);
			if ( s != null ) {
				row.setLocation(Location.valueOf(s));
			}

			s = rs.getString(6);
			if ( s != null ) {
				row.setUnit(UnitOfMeasure.valueOf(s));
			}

			return row;
		}
	}
}

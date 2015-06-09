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
import ocpp.v15.MeterValue.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.PreparedStatementCreator;
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
	public void storeChargeSession(final ChargeSession session) {
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
	}

	@Override
	protected void setStoreStatementValues(ChargeSession session, PreparedStatement ps)
			throws SQLException {
		// Row order is: (created, sessid_hi, sessid_lo, idtag, socketid, xid, ended)
		ps.setTimestamp(1, new Timestamp(session.getCreated() != null ? session.getCreated().getTime()
				: System.currentTimeMillis()), utcCalendar);
		UUID pk = UUID.fromString(session.getSessionId());
		ps.setLong(2, pk.getMostSignificantBits());
		ps.setLong(3, pk.getLeastSignificantBits());
		ps.setString(4, session.getIdTag());
		ps.setString(5, session.getSocketId());
		if ( session.getTransactionId() != null ) {
			ps.setLong(6, session.getTransactionId().longValue());
		} else {
			ps.setNull(6, Types.BIGINT);
		}
		if ( session.getEnded() != null ) {
			// store ts in UTC time zone
			Calendar cal = calendarForDate(session.getEnded());
			Timestamp ts = new Timestamp(cal.getTimeInMillis());
			ps.setTimestamp(7, ts, cal);
		} else {
			ps.setNull(7, Types.TIMESTAMP);
		}
	}

	private int updateDomainObject(final ChargeSession session, final String sql) {
		return getJdbcTemplate().update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(sql);
				if ( session.getTransactionId() == null ) {
					ps.setNull(1, Types.BIGINT);
				} else {
					ps.setLong(1, session.getTransactionId().longValue());
				}
				if ( session.getEnded() != null ) {
					// store ts in UTC time zone
					Calendar cal = calendarForDate(session.getEnded());
					Timestamp ts = new Timestamp(cal.getTimeInMillis());
					ps.setTimestamp(2, ts, cal);
				} else {
					ps.setNull(2, Types.TIMESTAMP);
				}

				UUID pk = UUID.fromString(session.getSessionId());
				ps.setLong(3, pk.getMostSignificantBits());
				ps.setLong(4, pk.getLeastSignificantBits());
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
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeMeterReadings(String sessionId, Date date, Iterable<Value> readings) {
		// TODO Auto-generated method stub

	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteCompletedChargeSessions(Date olderThanDate) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteIncompletedChargeSessions(Date olderThanDate) {
		// TODO Auto-generated method stub
		return 0;
	}

	private final class ChargeSessionRowMapper implements RowMapper<ChargeSession> {

		@Override
		public ChargeSession mapRow(ResultSet rs, int rowNum) throws SQLException {
			ChargeSession row = new ChargeSession();
			// Row order is: created, sessid_hi, sessid_lo, idtag, socketid, xid, ended
			Timestamp ts = rs.getTimestamp(1, utcCalendar);
			if ( ts != null ) {
				row.setCreated(new Date(ts.getTime()));
			}
			row.setSessionId(new UUID(rs.getLong(2), rs.getLong(3)).toString());
			row.setIdTag(rs.getString(4));
			row.setSocketId(rs.getString(5));

			Number n = (Number) rs.getObject(6);
			if ( n != null ) {
				row.setTransactionId(n.intValue());
			}

			ts = rs.getTimestamp(7, utcCalendar);
			if ( ts != null ) {
				row.setEnded(new Date(ts.getTime()));
			}
			return row;
		}
	}
}

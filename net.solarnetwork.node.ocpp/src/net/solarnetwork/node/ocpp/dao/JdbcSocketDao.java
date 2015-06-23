/* ==================================================================
 * JdbcSocketDao.java - 15/06/2015 12:21:08 pm
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
import java.util.Date;
import java.util.List;
import net.solarnetwork.node.ocpp.Authorization;
import net.solarnetwork.node.ocpp.Socket;
import net.solarnetwork.node.ocpp.SocketDao;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC implementation of {@link SocketDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcSocketDao extends AbstractOcppJdbcDao<Socket> implements SocketDao {

	/** The default tables version. */
	public static final int TABLES_VERSION = 1;

	/** The table name for {@link Authorization} data. */
	public static final String TABLE_NAME = "ocpp_socket";

	/** The default classpath Resource for the {@code initSqlResource}. */
	public static final String INIT_SQL = "derby-socket-init.sql";

	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String SQL_GET_TABLES_VERSION = "SELECT svalue FROM solarnode.sn_settings WHERE skey = "
			+ "'solarnode.ocpp_socket.version'";

	public static final String SQL_INSERT = "insert";
	public static final String SQL_UPDATE = "update";
	public static final String SQL_GET_BY_PK = "get-pk";
	public static final String SQL_GET_ENABLED = "get-enabled";

	/**
	 * Default constructor.
	 */
	public JdbcSocketDao() {
		super();
		setSqlResourcePrefix("derby-socket");
		setTableName(TABLE_NAME);
		setTablesVersion(TABLES_VERSION);
		setSqlGetTablesVersion(SQL_GET_TABLES_VERSION);
		setInitSqlResource(new ClassPathResource(INIT_SQL, getClass()));
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeSocket(Socket socket) {
		Socket existing = getSocket(socket.getSocketId());
		if ( existing != null ) {
			updateDomainObject(socket, getSqlResource(SQL_UPDATE));
		} else {
			insertDomainObject(socket, getSqlResource(SQL_INSERT));
		}
	}

	@Override
	protected void setStoreStatementValues(Socket socket, PreparedStatement ps) throws SQLException {
		// cols: (created, socketid, enabled)
		ps.setTimestamp(1, new Timestamp(socket.getCreated() != null ? socket.getCreated().getTime()
				: System.currentTimeMillis()), utcCalendar);
		ps.setString(2, socket.getSocketId());
		ps.setBoolean(3, socket.isEnabled());
	}

	@Override
	protected void setUpdateStatementValues(Socket socket, PreparedStatement ps) throws SQLException {
		ps.setBoolean(1, socket.isEnabled());
		ps.setString(2, socket.getSocketId());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Socket getSocket(final String socketId) {
		List<Socket> results = getJdbcTemplate().query(getSqlResource(SQL_GET_BY_PK),
				new SocketRowMapper(), socketId);
		if ( results != null && results.size() > 0 ) {
			return results.get(0);
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public boolean isEnabled(final String socketId) {
		return getJdbcTemplate().query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(getSqlResource(SQL_GET_ENABLED));
				ps.setString(1, socketId);
				return ps;
			}
		}, new ResultSetExtractor<Boolean>() {

			@Override
			public Boolean extractData(ResultSet rs) throws SQLException, DataAccessException {
				boolean result = true;
				if ( rs.next() ) {
					result = rs.getBoolean(1);
				}
				return result;
			}
		});
	}

	private final class SocketRowMapper implements RowMapper<Socket> {

		@Override
		public Socket mapRow(ResultSet rs, int rowNum) throws SQLException {
			Socket auth = new Socket();
			// cols: created, socketid, enabled
			Timestamp ts = rs.getTimestamp(1, utcCalendar);
			if ( ts != null ) {
				auth.setCreated(new Date(ts.getTime()));
			}
			auth.setSocketId(rs.getString(2));
			auth.setEnabled(rs.getBoolean(3));
			return auth;
		}
	}

}

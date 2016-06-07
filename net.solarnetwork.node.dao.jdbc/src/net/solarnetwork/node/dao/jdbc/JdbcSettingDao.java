/* ===================================================================
 * JdbcSettingDao.java
 * 
 * Created Sep 7, 2009 3:08:37 PM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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

package net.solarnetwork.node.dao.jdbc;

import static net.solarnetwork.node.dao.jdbc.JdbcDaoConstants.SCHEMA_NAME;
import static net.solarnetwork.node.dao.jdbc.JdbcDaoConstants.TABLE_SETTINGS;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.Setting;
import net.solarnetwork.node.Setting.SettingFlag;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.support.KeyValuePair;
import net.solarnetwork.util.OptionalService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Simple JDBC-based implementation of {@link SettingDao}.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>sqlGet</dt>
 * <dd>The SQL statement to use for getting a row based on a String primary key.
 * Accepts a single parameter: the String primary key to retrieve.</dd>
 * 
 * <dt>sqlDelete</dt>
 * <dd>The SQL statement to use for deleting an existing row. Accepts a single
 * parameter: the String primary key to delete.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.1
 */
public class JdbcSettingDao extends AbstractBatchableJdbcDao<Setting> implements SettingDao {

	private static final String DEFAULT_SQL_FIND = "SELECT tkey,svalue FROM " + SCHEMA_NAME + '.'
			+ TABLE_SETTINGS + " WHERE skey = ? ORDER BY tkey";

	private static final String DEFAULT_SQL_GET = "SELECT svalue,modified,skey,tkey,flags FROM "
			+ SCHEMA_NAME + '.' + TABLE_SETTINGS + " WHERE skey = ? AND tkey = ?";

	private static final String DEFAULT_SQL_DELETE = "DELETE FROM " + SCHEMA_NAME + '.' + TABLE_SETTINGS
			+ " WHERE skey = ? AND tkey = ?";

	private static final String DEFAULT_BATCH_SQL_GET = "SELECT svalue,modified,skey,tkey,flags FROM "
			+ SCHEMA_NAME + '.' + TABLE_SETTINGS + " ORDER BY skey,tkey";

	private static final String DEFAULT_SQL_GET_DATE = "SELECT modified FROM " + SCHEMA_NAME + '.'
			+ TABLE_SETTINGS + " WHERE skey = ? AND tkey = ?";

	private static final String DEFAULT_SQL_GET_MOST_RECENT_DATE = "SELECT modified FROM " + SCHEMA_NAME
			+ '.' + TABLE_SETTINGS
			+ " WHERE SOLARNODE.BITWISE_AND(flags, ?) <> ? ORDER BY modified DESC";

	private final String sqlGet = DEFAULT_SQL_GET;
	private final String sqlDelete = DEFAULT_SQL_DELETE;
	private final String sqlFind = DEFAULT_SQL_FIND;
	private final String sqlBatchGet = DEFAULT_BATCH_SQL_GET;
	private final String sqlGetDate = DEFAULT_SQL_GET_DATE;
	private final String sqlGetMostRecentDate = DEFAULT_SQL_GET_MOST_RECENT_DATE;

	private OptionalService<EventAdmin> eventAdmin;

	@Override
	public boolean deleteSetting(String key) {
		return deleteSetting(key, "");
	}

	@Override
	public String getSetting(String key) {
		return getSetting(key, "");
	}

	@Override
	public void storeSetting(final String key, final String value) {
		storeSetting(key, "", value);
	}

	@Override
	public boolean deleteSetting(final String key, final String type) {
		TransactionTemplate tt = getTransactionTemplate();
		if ( tt != null ) {
			return tt.execute(new TransactionCallback<Boolean>() {

				@Override
				public Boolean doInTransaction(TransactionStatus status) {
					return deleteSettingInternal(key, type);
				}
			});
		} else {
			return deleteSettingInternal(key, type);
		}
	}

	private boolean deleteSettingInternal(String key, String type) {
		// check if will delete, to emit change event
		Setting setting = getJdbcTemplate().query(this.sqlGet, new ResultSetExtractor<Setting>() {

			@Override
			public Setting extractData(ResultSet rs) throws SQLException, DataAccessException {
				Setting s = null;
				if ( rs.next() ) {
					s = getBatchRowEntity(null, rs, 1);
				}
				return s;
			}
		}, key, type);
		boolean result = false;
		if ( setting != null ) {
			int res = getJdbcTemplate().update(this.sqlDelete, key, type);
			result = (res > 0);
			if ( result && setting.getFlags() != null
					&& !setting.getFlags().contains(SettingFlag.Volatile) ) {
				postSettingUpdatedEvent(key, type, setting.getValue());
			}
		}
		return result;
	}

	@Override
	public String getSetting(String key, String type) {
		List<String> res = getJdbcTemplate().query(this.sqlGet, new RowMapper<String>() {

			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(1);
			}
		}, key, type);
		if ( res != null && res.size() > 0 ) {
			return res.get(0);
		}
		return null;
	}

	@Override
	public List<KeyValuePair> getSettings(String key) {
		return getJdbcTemplate().query(this.sqlFind, new RowMapper<KeyValuePair>() {

			@Override
			public KeyValuePair mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new KeyValuePair(rs.getString(1), rs.getString(2));
			}
		}, key);
	}

	@Override
	public void storeSetting(final String key, final String type, final String value) {
		TransactionTemplate tt = getTransactionTemplate();
		if ( tt != null ) {
			tt.execute(new TransactionCallbackWithoutResult() {

				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					storeSettingInternal(key, type, value, 0);
				}
			});
		} else {
			storeSettingInternal(key, type, value, 0);
		}
	}

	@Override
	public void storeSetting(final Setting setting) {
		TransactionTemplate tt = getTransactionTemplate();
		if ( tt != null ) {
			tt.execute(new TransactionCallbackWithoutResult() {

				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					storeSettingInternal(setting.getKey(), setting.getType(), setting.getValue(),
							SettingFlag.maskForSet(setting.getFlags()));
				}
			});
		} else {
			storeSettingInternal(setting.getKey(), setting.getType(), setting.getValue(),
					SettingFlag.maskForSet(setting.getFlags()));
		}
	}

	@Override
	public Setting readSetting(String key, String type) {
		// TODO Auto-generated method stub
		return null;
	}

	private void storeSettingInternal(final String key, final String ttype, final String value,
			final int flags) {
		final String type = (ttype == null ? "" : ttype);
		final Timestamp now = new Timestamp(System.currentTimeMillis());
		// to avoid bumping modified date column when values haven't changed, we are careful here
		// to compare before actually updating
		getJdbcTemplate().query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement queryStmt = con.prepareStatement(sqlGet,
						ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE,
						ResultSet.CLOSE_CURSORS_AT_COMMIT);
				queryStmt.setString(1, key);
				queryStmt.setString(2, type);
				return queryStmt;
			}
		}, new ResultSetExtractor<Object>() {

			@Override
			public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
				if ( rs.next() ) {
					String oldValue = rs.getString(1);
					if ( !value.equals(oldValue) ) {
						rs.updateString(1, value);
						rs.updateTimestamp(2, now);
						rs.updateRow();
					}
				} else {
					rs.moveToInsertRow();
					rs.updateString(1, value);
					rs.updateTimestamp(2, now);
					rs.updateString(3, key);
					rs.updateString(4, type);
					rs.updateInt(5, flags);
					rs.insertRow();

					if ( !SettingFlag.setForMask(flags).contains(SettingFlag.Volatile) ) {
						postSettingUpdatedEvent(key, type, value);
					}
				}
				return null;
			}
		});
	}

	// --- Batch support ---

	@Override
	public Date getSettingModificationDate(final String key, final String type) {
		return getJdbcTemplate().query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement stmt = con.prepareStatement(sqlGetDate);
				stmt.setMaxRows(1);
				stmt.setString(1, key);
				stmt.setString(2, type);
				return stmt;
			}
		}, new ResultSetExtractor<Date>() {

			@Override
			public Date extractData(ResultSet rs) throws SQLException, DataAccessException {
				if ( rs.next() ) {
					return rs.getTimestamp(1);
				}
				return null;
			}
		});
	}

	@Override
	public Date getMostRecentModificationDate() {
		return getJdbcTemplate().query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement stmt = con.prepareStatement(sqlGetMostRecentDate);
				stmt.setMaxRows(1);
				final int mask = SettingFlag.maskForSet(EnumSet.of(SettingFlag.IgnoreModificationDate));
				stmt.setInt(1, mask);
				stmt.setInt(2, mask);
				return stmt;
			}
		}, new ResultSetExtractor<Date>() {

			@Override
			public Date extractData(ResultSet rs) throws SQLException, DataAccessException {
				if ( rs.next() ) {
					return rs.getTimestamp(1);
				}
				return null;
			}
		});
	}

	@Override
	protected String getBatchJdbcStatement(BatchOptions options) {
		return sqlBatchGet;
	}

	@Override
	protected Setting getBatchRowEntity(BatchOptions options, ResultSet resultSet, int rowCount)
			throws SQLException {
		Setting s = new Setting();
		s.setValue(resultSet.getString(1));
		s.setModified(resultSet.getTimestamp(2));
		s.setKey(resultSet.getString(3));
		s.setType(resultSet.getString(4));
		s.setFlags(SettingFlag.setForMask(resultSet.getInt(5)));
		return s;
	}

	@Override
	protected void updateBatchRowEntity(BatchOptions options, ResultSet resultSet, int rowCount,
			Setting entity) throws SQLException {
		resultSet.updateString(1, entity.getKey());
		resultSet.updateString(2, entity.getType());
		resultSet.updateString(3, entity.getValue());
		resultSet.updateInt(5, SettingFlag.maskForSet(entity.getFlags()));
	}

	private final void postSettingUpdatedEvent(final String key, final String type, final String value) {
		EventAdmin ea = (eventAdmin == null ? null : eventAdmin.service());
		if ( ea == null ) {
			return;
		}
		Map<String, Object> props = new HashMap<String, Object>();
		if ( key != null ) {
			props.put(SETTING_KEY, key);
		}
		if ( type != null ) {
			props.put(SETTING_TYPE, type);
		}
		if ( value != null ) {
			props.put(SETTING_VALUE, value);
		}
		Event event = new Event(SettingDao.EVENT_TOPIC_SETTING_CHANGED, props);
		ea.postEvent(event);
	}

	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * An optional {@link EventAdmin} service to use.
	 * 
	 * @param eventAdmin
	 *        The event admin service to use.
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}

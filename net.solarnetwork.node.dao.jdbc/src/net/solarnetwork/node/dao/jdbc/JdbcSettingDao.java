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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Timestamp;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.domain.Setting.SettingFlag;
import net.solarnetwork.node.domain.SettingNote;
import net.solarnetwork.service.OptionalService;

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
 * @version 2.2
 */
public class JdbcSettingDao extends AbstractBatchableJdbcDao<Setting> implements SettingDao {

	/** The SQL resource for a non-typed fetch. */
	public static final String SQL_RESOURCE_NON_TYPED_GET = "non-typed-get";

	/** The SQL resource for a typed fetch. */
	public static final String SQL_RESOURCE_TYPED_GET = "typed-get";

	/** The SQL resource for a typed fetch for note. */
	public static final String SQL_RESOURCE_TYPED_GET_NOTE = "typed-get-note";

	/** The SQL resource for a search. */
	public static final String SQL_RESOURCE_FIND = "find";

	/** The SQL resource for a search for notes for a key. */
	public static final String SQL_RESOURCE_FIND_NOTES_FOR_KEY = "find-notes-for-key";

	/** The SQL resource for a fetch for update. */
	public static final String SQL_RESOURCE_BATCH_GET_FOR_UPDATE = "batch-get-for-update";

	/** The SQL resource for a a batch fetch. */
	public static final String SQL_RESOURCE_BATCH_GET = "batch-get";

	/** The SQL resource for a fetch for date. */
	public static final String SQL_RESOURCE_GET_DATE = "get-date";

	/** The SQL resource for a fetch for most recent date. */
	public static final String SQL_RESOURCE_GET_MOST_RECENT_DATE = "get-most-recent-date";

	private OptionalService<EventAdmin> eventAdmin;

	/**
	 * Constructor.
	 */
	public JdbcSettingDao() {
		super();
		setSqlResourcePrefix("settings");
	}

	@Override
	public boolean deleteSetting(String key) {
		return deleteSetting(key, null);
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

	private String sqlForUpdate(String sql) {
		return (getSqlForUpdateSuffix() != null ? sql + getSqlForUpdateSuffix() : sql);
	}

	private boolean deleteSettingInternal(final String key, final String type) {
		// check if will delete, to emit change event
		final String sql;
		//check if we are taking type into consideration
		if ( type == null ) {
			sql = sqlForUpdate(getSqlResource(SQL_RESOURCE_NON_TYPED_GET));
		} else {
			sql = sqlForUpdate(getSqlResource(SQL_RESOURCE_TYPED_GET));
		}
		Setting setting = getJdbcTemplate().query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement queryStmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
				queryStmt.setString(1, key);
				if ( type != null ) {
					queryStmt.setString(2, type);
				}
				return queryStmt;
			}
		}, new ResultSetExtractor<Setting>() {

			@Override
			public Setting extractData(ResultSet rs) throws SQLException, DataAccessException {
				Setting s = null;
				while ( rs.next() ) {
					s = getBatchRowEntity(null, rs, 1);
					rs.deleteRow();
				}
				return s;
			}
		});

		boolean result = (setting != null);
		if ( setting != null && setting.getFlags() != null
				&& !setting.getFlags().contains(SettingFlag.Volatile) ) {
			postSettingUpdatedEvent(key, type, setting.getValue());
		}
		return result;
	}

	@Override
	public String getSetting(String key, String type) {
		List<String> res = getJdbcTemplate().query(getSqlResource(SQL_RESOURCE_TYPED_GET),
				new RowMapper<String>() {

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
	public List<KeyValuePair> getSettingValues(String key) {
		return getJdbcTemplate().query(getSqlResource(SQL_RESOURCE_FIND), new RowMapper<KeyValuePair>() {

			@Override
			public KeyValuePair mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new KeyValuePair(rs.getString(1), rs.getString(2));
			}
		}, key);
	}

	@Override
	public List<SettingNote> notesForKey(String key) {
		return getJdbcTemplate().query(getSqlResource(SQL_RESOURCE_FIND_NOTES_FOR_KEY),
				new RowMapper<SettingNote>() {

					@Override
					public SettingNote mapRow(ResultSet rs, int rowNum) throws SQLException {
						return Setting.note(rs.getString(1), rs.getString(2), rs.getString(3));
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
					storeSettingInternal(key, type, value, null, 0, false);
				}
			});
		} else {
			storeSettingInternal(key, type, value, null, 0, false);
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
							setting.getNote(), SettingFlag.maskForSet(setting.getFlags()), true);
				}
			});
		} else {
			storeSettingInternal(setting.getKey(), setting.getType(), setting.getValue(),
					setting.getNote(), SettingFlag.maskForSet(setting.getFlags()), true);
		}
	}

	@Override
	public void storeNotes(Iterable<? extends SettingNote> notes) {
		TransactionTemplate tt = getTransactionTemplate();
		if ( tt != null ) {
			tt.execute(new TransactionCallbackWithoutResult() {

				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					for ( SettingNote note : notes ) {
						storeNoteInternal(note.getKey(), note.getType(), note.getNote());
					}
				}
			});
		} else {
			for ( SettingNote note : notes ) {
				storeNoteInternal(note.getKey(), note.getType(), note.getNote());
			}
		}
	}

	private void storeNoteInternal(final String key, final String ttype, final String note) {
		final String type = (ttype == null ? "" : ttype);
		final Timestamp now = new Timestamp(System.currentTimeMillis());
		final String sql = sqlForUpdate(getSqlResource(SQL_RESOURCE_TYPED_GET_NOTE));
		final String noteToSave = (note != null && !note.isEmpty() ? note : null);
		// to avoid bumping modified date column when values haven't changed, we are careful here
		// to compare before actually updating
		getJdbcTemplate().execute(new ConnectionCallback<Boolean>() {

			@Override
			public Boolean doInConnection(Connection con) throws SQLException, DataAccessException {
				PreparedStatement stmt = null;
				ResultSet rs = null;
				boolean updated = false;

				try {
					stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
							ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
					stmt.setString(1, key);
					stmt.setString(2, type);

					if ( stmt.execute() ) {
						rs = stmt.getResultSet();
						if ( rs.next() ) {
							String oldNote = rs.getString(1);
							if ( note != oldNote && ((oldNote != null && !oldNote.equals(noteToSave))
									|| (noteToSave != null && !noteToSave.equals(oldNote))) ) {
								rs.updateString(1, noteToSave);
								updated = true;
							}
							if ( updated ) {
								rs.updateTimestamp(2, now);
								rs.updateRow();
							}
						} else {
							rs.moveToInsertRow();
							rs.updateString(1, noteToSave);
							rs.updateTimestamp(2, now);
							rs.updateString(3, key);
							rs.updateString(4, type);
							rs.updateString(5, ""); // non-null column
							rs.insertRow();
							updated = true;
						}
					}
				} finally {
					if ( stmt != null ) {
						SQLWarning warning = stmt.getWarnings();
						if ( warning != null ) {
							log.warn("SQL warning saving setting {}.{} note to {}", key, type,
									noteToSave, warning);
						}
						stmt.close();
					}
					if ( rs != null ) {
						rs.close();
					}
				}
				return updated;
			}
		});
	}

	@Override
	public Setting readSetting(String key, String type) {
		List<Setting> res = getJdbcTemplate().query(getSqlResource(SQL_RESOURCE_TYPED_GET),
				new RowMapper<Setting>() {

					@Override
					public Setting mapRow(ResultSet rs, int rowNum) throws SQLException {
						return getBatchRowEntity(null, rs, rowNum);
					}
				}, key, type);
		if ( res != null && res.size() > 0 ) {
			return res.get(0);
		}
		return null;
	}

	private void storeSettingInternal(final String key, final String ttype, final String value,
			final String note, final int flags, final boolean updateNote) {
		final String type = (ttype == null ? "" : ttype);
		final Timestamp now = new Timestamp(System.currentTimeMillis());
		final String sql = sqlForUpdate(getSqlResource(SQL_RESOURCE_TYPED_GET));
		final String noteToSave = (note != null && !note.isEmpty() ? note : null);
		// to avoid bumping modified date column when values haven't changed, we are careful here
		// to compare before actually updating
		getJdbcTemplate().execute(new ConnectionCallback<Boolean>() {

			@Override
			public Boolean doInConnection(Connection con) throws SQLException, DataAccessException {
				PreparedStatement stmt = null;
				ResultSet rs = null;
				boolean updated = false;

				try {
					stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
							ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
					stmt.setString(1, key);
					stmt.setString(2, type);

					if ( stmt.execute() ) {
						rs = stmt.getResultSet();
						if ( rs.next() ) {
							String oldValue = rs.getString(1);
							if ( !value.equals(oldValue) ) {
								rs.updateString(1, value);
								updated = true;
							}
							if ( updateNote ) {
								String oldNote = rs.getString(6);
								if ( note != oldNote && ((oldNote != null && !oldNote.equals(noteToSave))
										|| (noteToSave != null && !noteToSave.equals(oldNote))) ) {
									rs.updateString(6, noteToSave);
									updated = true;
								}
							}
							if ( updated ) {
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
							if ( updateNote ) {
								rs.updateString(6, noteToSave);
							}
							rs.insertRow();
							updated = true;
						}
					}
				} finally {
					if ( stmt != null ) {
						SQLWarning warning = stmt.getWarnings();
						if ( warning != null ) {
							log.warn("SQL warning saving setting {}.{} to {}", key, type, value,
									warning);
						}
						stmt.close();
					}
					if ( rs != null ) {
						rs.close();
					}
				}
				if ( updated && !SettingFlag.setForMask(flags).contains(SettingFlag.Volatile) ) {
					postSettingUpdatedEvent(key, type, value);
				}
				return updated;
			}
		});
	}

	// --- Batch support ---

	@Override
	public Date getSettingModificationDate(final String key, final String type) {
		return getJdbcTemplate().query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement stmt = con.prepareStatement(getSqlResource(SQL_RESOURCE_GET_DATE));
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
				PreparedStatement stmt = con
						.prepareStatement(getSqlResource(SQL_RESOURCE_GET_MOST_RECENT_DATE));
				stmt.setMaxRows(1);
				final int mask = SettingFlag.maskForSet(EnumSet.of(SettingFlag.IgnoreModificationDate));
				stmt.setInt(1, mask);
				stmt.setInt(2, mask);
				stmt.setMaxRows(1);
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
		return (options != null && options.isUpdatable()
				? getSqlResource(SQL_RESOURCE_BATCH_GET_FOR_UPDATE)
				: getSqlResource(SQL_RESOURCE_BATCH_GET));
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
		s.setNote(resultSet.getString(6));
		return s;
	}

	@Override
	protected void updateBatchRowEntity(BatchOptions options, ResultSet resultSet, int rowCount,
			Setting entity) throws SQLException {
		// SELECT svalue,modified,skey,tkey,flags
		resultSet.updateString(1, entity.getValue());
		resultSet.updateTimestamp(2, new Timestamp(System.currentTimeMillis()));
		resultSet.updateString(3, entity.getKey());
		resultSet.updateString(4, entity.getType());
		resultSet.updateInt(5, SettingFlag.maskForSet(entity.getFlags()));
		resultSet.updateString(6, entity.getNote());
	}

	private final void postSettingUpdatedEvent(final String key, final String type, final String value) {
		EventAdmin ea = (eventAdmin == null ? null : eventAdmin.service());
		if ( ea == null ) {
			return;
		}
		Map<String, Object> props = new HashMap<>();
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

	/**
	 * Get the EventAdmin service.
	 *
	 * @return the EventAdmin service
	 */
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

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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.dao.jdbc;

import static net.solarnetwork.node.dao.jdbc.JdbcDaoConstants.SCHEMA_NAME;
import static net.solarnetwork.node.dao.jdbc.JdbcDaoConstants.TABLE_SETTINGS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import net.solarnetwork.node.Setting;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.support.KeyValuePair;

import org.springframework.jdbc.core.RowMapper;

/**
 * Simple JDBC-based implemenation of {@link SettingDao}.
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>sqlGet</dt>
 *   <dd>The SQL statement to use for getting a row based on a String primary
 *   key. Accepts a single parameter: the String primary key to retrieve.</dd>
 *   
 *   <dt>sqlInsert</dt>
 *   <dd>The SQL statement to use for inserting a new row. Accepts two
 *   parameters: a String primary key and a String value.</dd>
 *   
 *   <dt>sqlUpdate</dt>
 *   <dd>The SQL statement to use for updating an existing row. Accepts two
 *   parameters: a String value and a String primary key.</dd>
 *   
 *   <dt>sqlDelete</dt>
 *   <dd>The SQL statement to use for deleting an existing row. Accepts a
 *   single parameter: the String primary key to delete.</dd>
 * </dl>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class JdbcSettingDao extends AbstractBatchableJdbcDao<Setting> implements SettingDao {

	private static final String DEFAULT_SQL_GET = "SELECT svalue FROM " 
		+SCHEMA_NAME +'.'+TABLE_SETTINGS
		+" WHERE skey = ? ORDER BY tkey";
	
	private static final String DEFAULT_SQL_INSERT = "INSERT INTO " 
		+SCHEMA_NAME +'.'+TABLE_SETTINGS
		+" (skey, svalue) VALUES (?,?)";
	
	private static final String DEFAULT_SQL_UPDATE = "UPDATE " 
		+SCHEMA_NAME +'.'+TABLE_SETTINGS
		+" SET svalue = ? WHERE skey = ?";
	
	private static final String DEFAULT_SQL_DELETE = "DELETE FROM " 
		+SCHEMA_NAME +'.'+TABLE_SETTINGS
		+" WHERE skey = ?";

	private static final String DEFAULT_SQL_FIND = "SELECT tkey, svalue FROM " 
		+SCHEMA_NAME +'.'+TABLE_SETTINGS
		+" WHERE skey = ? ORDER BY tkey";
	
	private static final String DEFAULT_TYPE_SQL_GET = "SELECT svalue FROM " 
		+SCHEMA_NAME +'.'+TABLE_SETTINGS
		+" WHERE skey = ? AND tkey = ?";
	
	private static final String DEFAULT_TYPE_SQL_INSERT = "INSERT INTO " 
		+SCHEMA_NAME +'.'+TABLE_SETTINGS
		+" (skey, tkey, svalue) VALUES (?,?,?)";
	
	private static final String DEFAULT_TYPE_SQL_UPDATE = "UPDATE " 
		+SCHEMA_NAME +'.'+TABLE_SETTINGS
		+" SET svalue = ? WHERE skey = ? AND tkey = ?";
	
	private static final String DEFAULT_TYPE_SQL_DELETE = "DELETE FROM " 
		+SCHEMA_NAME +'.'+TABLE_SETTINGS
		+" WHERE skey = ? AND tkey = ?";
	
	private static final String DEFAULT_BATCH_SQL_GET = "SELECT skey,tkey,svalue FROM " 
			+SCHEMA_NAME +'.'+TABLE_SETTINGS +" ORDER BY skey,tkey";


	private String sqlGet = DEFAULT_SQL_GET;
	private String sqlInsert = DEFAULT_SQL_INSERT;
	private String sqlUpdate = DEFAULT_SQL_UPDATE;
	private String sqlDelete = DEFAULT_SQL_DELETE;
	private String sqlFind = DEFAULT_SQL_FIND;
	private String sqlTypeGet = DEFAULT_TYPE_SQL_GET;
	private String sqlTypeInsert = DEFAULT_TYPE_SQL_INSERT;
	private String sqlTypeUpdate = DEFAULT_TYPE_SQL_UPDATE;
	private String sqlTypeDelete = DEFAULT_TYPE_SQL_DELETE;
	private String sqlBatchGet = DEFAULT_BATCH_SQL_GET;
	
	@Override
	public boolean deleteSetting(String key) {
		int res = getSimpleJdbcTemplate().update(this.sqlDelete, key);
		return res > 0;
	}

	@Override
	public String getSetting(String key) {
		List<String> res = getSimpleJdbcTemplate().query(this.sqlGet, new RowMapper<String>() {
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(1);
			}
		}, key);
		if ( res != null && res.size() > 0 ) {
			return res.get(0);
		}
		return null;
	}

	@Override
	public void storeSetting(String key, String value) {
		int updated = getSimpleJdbcTemplate().update(this.sqlUpdate, value, key);
		if ( updated < 1 ) {
			updated = getSimpleJdbcTemplate().update(this.sqlInsert, key, value);
		}
	}

	@Override
	public boolean deleteSetting(String key, String type) {
		int res = getSimpleJdbcTemplate().update(this.sqlTypeDelete, key, type);
		return res > 0;
	}

	@Override
	public String getSetting(String key, String type) {
		List<String> res = getSimpleJdbcTemplate().query(this.sqlTypeGet, new RowMapper<String>() {
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
		return getSimpleJdbcTemplate().query(this.sqlFind, 
				new RowMapper<KeyValuePair>() {
			public KeyValuePair mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new KeyValuePair(rs.getString(1), rs.getString(2));
			}
		}, key);
	}

	@Override
	public void storeSetting(String key, String type, String value) {
		int updated = getSimpleJdbcTemplate().update(this.sqlTypeUpdate, value, key, type);
		if ( updated < 1 ) {
			updated = getSimpleJdbcTemplate().update(this.sqlTypeInsert, key, type, value);
		}
	}

	// --- Batch support ---
	
	@Override
	protected String getBatchJdbcStatement(BatchOptions options) {
		return sqlBatchGet;
	}

	@Override
	protected Setting getBatchRowEntity(BatchOptions options,
			ResultSet resultSet, int rowCount) throws SQLException {
		Setting s = new Setting();
		s.setKey(resultSet.getString(1));
		s.setType(resultSet.getString(2));
		s.setValue(resultSet.getString(3));
		return s;
	}

	@Override
	protected void updateBatchRowEntity(BatchOptions options,
			ResultSet resultSet, int rowCount, Setting entity) throws SQLException {
		resultSet.updateString(1, entity.getKey());
		resultSet.updateString(2, entity.getType());
		resultSet.updateString(3, entity.getValue());
	}

}

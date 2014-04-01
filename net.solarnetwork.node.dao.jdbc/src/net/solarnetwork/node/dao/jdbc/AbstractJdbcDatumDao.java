/* ===================================================================
 * AbstractDatumUploadJdbcDao.java
 * 
 * Created Jul 28, 2009 11:16:36 AM
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
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import net.solarnetwork.node.Mock;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.Datum;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

/**
 * Abstract DAO implementation with support for DAOs that need to manage
 * "upload" tasks.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>sqlDeleteOld</dt>
 * <dd>SQL statement for deleting "old" datum rows that have already been
 * "uploaded" to a central server, thus freeing up space in the local node's
 * database. See the {@link #deleteUploadedDataOlderThanHours(int)} for
 * info.</p>
 * 
 * <dt>maxFetchForUpload</dt>
 * <dd>The maximum number of rows to return in the
 * {@link #findDatumNotUploaded(String, RowMapper)} method. Defaults to
 * {@link #DEFAULT_MAX_FETCH_FOR_UPLOAD}.</dd>
 * 
 * <dt>sqlInsertDatum</dt>
 * <dd>The SQL to use for inserting a new datum. This is used by the
 * {@link #storeDatum(Datum)} method.</dd>
 * 
 * <dt>ignoreMockData</dt>
 * <dd>If <em>true</em> then do not actually store any domain object that
 * implements the {@link Mock} interface. This defaults to <em>true</em>, but
 * during development it can be useful to configure this as <em>false</em> for
 * testing.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.1
 * @param <T>
 *        the domain object type managed by this DAO
 */
public abstract class AbstractJdbcDatumDao<T extends Datum> extends AbstractJdbcDao<T> implements
		DatumDao<T> {

	/** The default value for the {@code maxFetchForUpload} property. */
	public static final int DEFAULT_MAX_FETCH_FOR_UPLOAD = 60;

	public static final String SQL_RESOURCE_INSERT = "insert";
	public static final String SQL_RESOURCE_DELETE_OLD = "delete-old";
	public static final String SQL_RESOURCE_FIND_FOR_UPLOAD = "find-upload";
	public static final String SQL_RESOURCE_UPDATE_UPLOADED = "update-upload";

	private int maxFetchForUpload = DEFAULT_MAX_FETCH_FOR_UPLOAD;
	private boolean ignoreMockData = true;

	/**
	 * Execute a SQL update to delete data that has already been "uploaded" and
	 * is older than a specified number of hours.
	 * 
	 * <p>
	 * This executes SQL from the {@code sqlDeleteOld} property, setting a
	 * single timestamp parameter as the current time minus {@code hours} hours.
	 * The general idea is for the SQL to join to some "upload" table to find
	 * the rows in the "datum" table that have been uploaded and are older than
	 * the specified number of hours. For example:
	 * </p>
	 * 
	 * <pre>
	 * DELETE FROM solarnode.sn_some_datum p WHERE p.id IN 
	 * (SELECT pd.id FROM solarnode.sn_some_datum pd 
	 * INNER JOIN solarnode.sn_some_datum_upload u 
	 * ON u.power_datum_id = pd.id WHERE pd.created < ?)
	 * </pre>
	 * 
	 * @param hours
	 *        the number of hours hold to delete
	 * @return the number of rows deleted
	 */
	protected int deleteUploadedDataOlderThanHours(final int hours) {
		return getJdbcTemplate().update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				String sql = getSqlResource(SQL_RESOURCE_DELETE_OLD);
				log.debug("Preparing SQL to delete old datum [{}] with hours [{}]", sql, hours);
				PreparedStatement ps = con.prepareStatement(sql);
				Calendar c = Calendar.getInstance();
				c.add(Calendar.HOUR, -hours);
				ps.setTimestamp(1, new Timestamp(c.getTimeInMillis()), c);
				return ps;
			}
		});
	}

	/**
	 * Find datum entities that have not been uploaded to a specific
	 * destination.
	 * 
	 * <p>
	 * This executes SQL from the {@code findForUploadSql} property. It uses the
	 * {@code maxFetchForUpload} property to limit the number of rows returned,
	 * so the call may not return all rows available from the database (this is
	 * to conserve memory and process the data in small batches).
	 * </p>
	 * 
	 * @param destination
	 *        the destination to look for
	 * @param rowMapper
	 *        a {@link RowMapper} implementation to instantiate entities from
	 *        found rows
	 * @return the matching rows, never <em>null</em>
	 */
	protected List<T> findDatumNotUploaded(final RowMapper<T> rowMapper) {
		List<T> result = getJdbcTemplate().query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				String sql = getSqlResource(SQL_RESOURCE_FIND_FOR_UPLOAD);
				if ( log.isTraceEnabled() ) {
					log.trace("Preparing SQL to find datum not uploaded [" + sql
							+ "] with maxFetchForUpload [" + maxFetchForUpload + ']');
				}
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setFetchDirection(ResultSet.FETCH_FORWARD);
				ps.setFetchSize(maxFetchForUpload);
				ps.setMaxRows(maxFetchForUpload);
				return ps;
			}
		}, rowMapper);
		if ( log.isDebugEnabled() ) {
			log.debug("Found " + result.size() + " datum entities not uploaded");
		}
		return result;
	}

	/**
	 * Store a new domain object using the {@link #getSqlInsertDatum()} SQL.
	 * 
	 * <p>
	 * If {@link #isIgnoreMockData()} returns <em>true</em> and {@code datum} is
	 * an instance of {@link Mock} then this method will not persist the object
	 * and will simply return {@code -1}.
	 * </p>
	 * 
	 * @param datum
	 *        the datum to persist
	 * @return the entity primary key
	 */
	protected void storeDomainObject(final T datum) {
		if ( ignoreMockData && datum instanceof Mock ) {
			if ( log.isDebugEnabled() ) {
				log.debug("Not persisting Mock datum: " + datum);
			}
			return;
		}
		insertDomainObject(datum, getSqlResource(SQL_RESOURCE_INSERT));
	}

	/**
	 * Mark a Datum as uploaded.
	 * 
	 * <p>
	 * This method will call {@link #updateDatumUpload(long, Object, long)}
	 * passing in {@link T#getCreated()}, {@link T#getSourceId()}, and
	 * {@code timestamp}.
	 * </p>
	 * 
	 * @param datum
	 *        the datum that was uploaded
	 * @param timestamp
	 *        the date the upload happened
	 */
	protected void updateDatumUpload(final T datum, final long timestamp) {
		updateDatumUpload(datum.getCreated().getTime(), datum.getSourceId(), timestamp);
	}

	/**
	 * Mark a Datum as uploaded.
	 * 
	 * <p>
	 * This method will execute the {@link #SQL_RESOURCE_UPDATE_UPLOADED} SQL
	 * setting the following parameters:
	 * </p>
	 * 
	 * <ol>
	 * <li>Timestamp parameter based on {@code timestamp}</li>
	 * <li>Timestamp parameter based on {@code created}</li>
	 * <li>Object parameter based on {@code id}</li>
	 * </ol>
	 * 
	 * @param created
	 *        the date the object was created
	 * @param id
	 *        the object's source or location ID
	 * @param timestamp
	 *        the date the upload happened
	 */
	protected void updateDatumUpload(final long created, final Object id, final long timestamp) {
		getJdbcTemplate().update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con
						.prepareStatement(getSqlResource(SQL_RESOURCE_UPDATE_UPLOADED));
				int col = 1;
				ps.setTimestamp(col++, new java.sql.Timestamp(timestamp));
				ps.setTimestamp(col++, new java.sql.Timestamp(created));
				ps.setObject(col++, id);
				return ps;
			}
		});
	}

	public int getMaxFetchForUpload() {
		return maxFetchForUpload;
	}

	public void setMaxFetchForUpload(int maxFetchForUpload) {
		this.maxFetchForUpload = maxFetchForUpload;
	}

	public boolean isIgnoreMockData() {
		return ignoreMockData;
	}

	public void setIgnoreMockData(boolean ignoreMockData) {
		this.ignoreMockData = ignoreMockData;
	}

}

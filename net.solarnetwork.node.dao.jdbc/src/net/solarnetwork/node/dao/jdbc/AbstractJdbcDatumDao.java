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
 * $Id$
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

import net.solarnetwork.node.Datum;
import net.solarnetwork.node.DatumUpload;
import net.solarnetwork.node.Mock;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.support.BasicDatumUpload;

import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

/**
 * Abstract DAO implementation with support for DAOs that need to manage 
 * "upload" tasks.
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>sqlDeleteOld</dt>
 *   <dd>SQL statement for deleting "old" datum rows that have already been
 *   "uploaded" to a central server, thus freeing up space in the local node's
 *   database. See the {@link #deleteUploadedDataOlderThanHours(int)} for info.</p>
 *   
 *   <dt>findForUploadSqlResource</dt>
 *   <dd>A resource that contains the SQL query to use in the 
 *   {@link #findDatumNotUploaded(String, RowMapper)} method. If this property
 *   is configured and {@code findForUploadSql} is not when {@link #init()} is
 *   called, this class will load this SQL resource and store it on the 
 *   {@code findForUploadSql} property.</dd>
 *   
 *   <dt>findForUploadSql</dt>
 *   <dd>The SQL to use in the {@link #findDatumNotUploaded(String, RowMapper)} method.
 *   An alternative to configuring this property is to use the 
 *   {@code findForUploadSqlResource} to configure this SQL via a resource. If this
 *   property is configured, the {@code findForUploadSqlResource} property will be
 *   ignored.</dd>
 *   
 *   <dt>maxFetchForUpload</dt>
 *   <dd>The maximum number of rows to return in the 
 *   {@link #findDatumNotUploaded(String, RowMapper)} method. Defaults to 
 *   {@link #DEFAULT_MAX_FETCH_FOR_UPLOAD}.</dd>
 *   
 *   <dt>sqlInsertDatum</dt>
 *   <dd>The SQL to use for inserting a new datum. This is used by the 
 *   {@link #storeDatum(Datum)} method.</dd>
 *   
 *   <dt>sqlInsertUpload</dt>
 *   <dd>The SQL to use for inserting a new "upload" datum. This is used in the
 *   {@link #storeDatumUpload(DatumUpload)} method.</dd>
 *   
 *   <dt>uploadTableName</dt>
 *   <dd>The name of the table that stores the upload datum data.</dd>
 *   
 *   <dt>ignoreMockData</dt>
 *   <dd>If <em>true</em> then do not actually store any domain object that
 *   implements the {@link Mock} interface. This defaults to <em>true</em>, 
 *   but during development it can be useful to configure this as
 *   <em>false</em> for testing.</dd>
 * </dl>
 *
 * @author matt
 * @version $Revision$ $Date$
 * @param <T> the domain object type managed by this DAO
 */
public abstract class AbstractJdbcDatumDao<T extends Datum>
extends AbstractJdbcDao<T>
implements DatumDao<T> {

	/** The default value for the {@code maxFetchForUpload} property. */
	public static final int DEFAULT_MAX_FETCH_FOR_UPLOAD = 60;

	private String sqlInsertDatum = null;
	private String sqlDeleteOld = null;
	private Resource findForUploadSqlResource = null;
	private String findForUploadSql = null;
	private int maxFetchForUpload = DEFAULT_MAX_FETCH_FOR_UPLOAD;
	private String sqlInsertUpload = null;
	private String uploadTableName = null;
	private boolean ignoreMockData = true;
	
	/**
	 * Initialize this class after properties are set.
	 * 
	 * <p>This method will use the {@code findForUploadSqlResource} property to
	 * initialize the {@code findForUploadSql} if {@code findForUploadSql} is not
	 * configured. It will call {@link #getSqlResource(Resource)} and store the
	 * result on the {@code findForUploadSql} property.</p>
	 */
	@Override
	public void init() {
		super.init();
		
		if ( this.findForUploadSql == null && this.findForUploadSqlResource != null ) {
			// load SQL resources
			this.findForUploadSql = getSqlResource(findForUploadSqlResource);
		}
	}

	/**
	 * Execute a SQL update to delete data that has already been "uploaded" and
	 * is older than a specified number of hours.
	 * 
	 * <p>This executes SQL from the {@code sqlDeleteOld} property, setting 
	 * a single timestamp parameter as the current time minus {@code hours} hours.
	 * The general idea is for the SQL to join to some "upload" table to
	 * find the rows in the "datum" table that have been uploaded and are
	 * older than the specified number of hours. For example:</p>
	 * 
	 * <pre>DELETE FROM solarnode.sn_some_datum p WHERE p.id IN 
	 * (SELECT pd.id FROM solarnode.sn_some_datum pd 
	 * INNER JOIN solarnode.sn_some_datum_upload u 
	 * ON u.power_datum_id = pd.id WHERE pd.created < ?)</pre>
	 * 
	 * @param hours the number of hours hold to delete
	 * @return the number of rows deleted
	 */
	protected int deleteUploadedDataOlderThanHours(final int hours) {
		return getJdbcTemplate().update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con)
					throws SQLException {
				log.debug("Preparing SQL to delete old datum [{}] with hours [{}]",
						sqlDeleteOld, hours);
				PreparedStatement ps = con.prepareStatement(sqlDeleteOld);
				Calendar c = Calendar.getInstance();
				c.add(Calendar.HOUR, -hours);
				ps.setTimestamp(1, new Timestamp(c.getTimeInMillis()), c);
				return ps;
			}
		});
	}
	
	/**
	 * Find datum entities that have not been uploaded to a specific destination.
	 * 
	 * <p>This executes SQL from the {@code findForUploadSql} property, setting a 
	 * single string parameter as the specified {@code detination} to find. It 
	 * uses the {@code maxFetchForUpload} property to limit the number of rows
	 * returned, so the call may not return all rows available from the database
	 * (this is to conserve memory and process the data in small batches).</p>
	 * 
	 * <p>An example SQL statement looks like:</p>
	 * 
	 * <pre>SELECT c.id, 
	 *  c.created,
	 * 	c.voltage, 
	 * 	c.amps, 
	 * 	c.error_msg
	 * FROM solarnode.sn_consum_datum c 
	 * LEFT OUTER JOIN (
	 * 	SELECT u.consum_datum_id, u.destination
	 * 	FROM solarnode.sn_consum_datum_upload u
	 * 	WHERE u.destination = ?
	 * 	) AS sub ON sub.consum_datum_id = p.id
	 * WHERE sub.destination IS NULL
	 * ORDER BY c.id</pre>
	 * 
	 * @param destination the destination to look for
	 * @param rowMapper a {@link RowMapper} implementation to instantiate entities from 
	 * found rows
	 * @return the matching rows, never <em>null</em>
	 */
	protected List<T> findDatumNotUploaded(final String destination, final RowMapper<T> rowMapper) {
		List<T> result = getJdbcTemplate().query(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con)
					throws SQLException {
				if ( log.isTraceEnabled() ) {
					log.trace("Preparing SQL to find datum not uploaded ["
							+findForUploadSql +"] with maxFetchForUpload [" 
							+maxFetchForUpload +']');
				}
				PreparedStatement ps = con.prepareStatement(findForUploadSql);
				ps.setFetchDirection(ResultSet.FETCH_FORWARD);
				ps.setFetchSize(maxFetchForUpload);
				ps.setMaxRows(maxFetchForUpload);
				ps.setString(1, destination);
				return ps;
			}
		}, rowMapper);
		if ( log.isDebugEnabled() ) {
			log.debug("Found " +result.size() +" datum entities not uploaded");
		}
		return result;
	}
	
	/**
	 * Persist a new {@link DatumUpload} entity.
	 * 
	 * <p>This method assumes the implementation of {@link DatumUpload}
	 * has not added any fields that need persisting. It will execute the SQL from the 
	 * {@code sqlInsertUpload} property, setting the following parameters:</p>
	 * 
	 * <ol>
	 *   <li>datumId</li>
	 *   <li>destination</li>
	 *   <li>trackingId</li>
	 * </ol>
	 * 
	 * <p>An example SQL statement looks like:</p>
	 * 
	 * <pre>INSERT INTO solarnode.sn_some_datum_upload
	 * (u.some_datum_id, destination, track_id) VALUES (?,?,?)</pre>
	 * 
	 * @param upload
	 */
	protected void storeDatumUpload(final DatumUpload upload) {
		getJdbcTemplate().update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con)
					throws SQLException {
				PreparedStatement ps = con.prepareStatement(sqlInsertUpload);
				int col = 1;
				ps.setLong(col++, upload.getDatumId());
				ps.setString(col++, upload.getDestination());
				ps.setLong(col++, upload.getTrackingId());
				return ps;
			}
		});
	}
	
	/**
	 * Create and persist a new {@link DatumUpload} entity.
	 * 
	 * @param datum the datum that has been uploaded
	 * @param destination the upload destination
	 * @param trackingId the remote tracking ID
	 * @return new upload entity
	 */
	protected DatumUpload storeNewDatumUpload(T datum, String destination, Long trackingId) {
		BasicDatumUpload upload = new BasicDatumUpload(datum, null, destination, trackingId);
		storeDatumUpload(upload);
		return upload;
	}
	
	/**
	 * This implementation returns an array with two elements, the
	 * {@link #getTableName()} and {@link #getUploadTableName()} values,
	 * unless {@code uploadTableName} is null in which case 
	 * {@link AbstractJdbcDao#getTableNames()} is returned.
	 */
	@Override
	public String[] getTableNames() {
		if ( uploadTableName == null ) {
			return super.getTableNames();
		}
		return new String[] {
				getTableName(),
				getUploadTableName(),
		};
	}

	/**
	 * Store a new domain object using the {@link #getSqlInsertDatum()} SQL.
	 * 
	 * <p>If {@link #isIgnoreMockData()} returns <em>true</em> and 
	 * {@code datum} is an instance of {@link Mock} then this method will
	 * not persist the object and will simply return {@code -1}.</p>
	 * 
	 * @param datum the datum to persist
	 * @return the entity primary key
	 */
	protected Long storeDomainObject(final T datum) {
		if ( ignoreMockData && datum instanceof Mock ) {
			if ( log.isDebugEnabled() ) {
				log.debug("Not persisting Mock datum: " +datum);
			}
			return -1L;
		}
		return storeDomainObject(datum, sqlInsertDatum);
	}
	
	/**
	 * @return the sqlDeleteOld
	 */
	public String getSqlDeleteOld() {
		return sqlDeleteOld;
	}
	
	/**
	 * @param sqlDeleteOld the sqlDeleteOld to set
	 */
	public void setSqlDeleteOld(String sqlDeleteOld) {
		this.sqlDeleteOld = sqlDeleteOld;
	}
	
	/**
	 * @return the findForUploadSqlResource
	 */
	public Resource getFindForUploadSqlResource() {
		return findForUploadSqlResource;
	}
	
	/**
	 * @param findForUploadSqlResource the findForUploadSqlResource to set
	 */
	public void setFindForUploadSqlResource(Resource findForUploadSqlResource) {
		this.findForUploadSqlResource = findForUploadSqlResource;
	}
	
	/**
	 * @return the findForUploadSql
	 */
	public String getFindForUploadSql() {
		return findForUploadSql;
	}
	
	/**
	 * @param findForUploadSql the findForUploadSql to set
	 */
	public void setFindForUploadSql(String findForUploadSql) {
		this.findForUploadSql = findForUploadSql;
	}
	
	/**
	 * @return the maxFetchForUpload
	 */
	public int getMaxFetchForUpload() {
		return maxFetchForUpload;
	}
	
	/**
	 * @param maxFetchForUpload the maxFetchForUpload to set
	 */
	public void setMaxFetchForUpload(int maxFetchForUpload) {
		this.maxFetchForUpload = maxFetchForUpload;
	}
	
	/**
	 * @return the sqlInsertUpload
	 */
	public String getSqlInsertUpload() {
		return sqlInsertUpload;
	}
	
	/**
	 * @param sqlInsertUpload the sqlInsertUpload to set
	 */
	public void setSqlInsertUpload(String sqlInsertUpload) {
		this.sqlInsertUpload = sqlInsertUpload;
	}

	/**
	 * @return the sqlInsertDatum
	 */
	public String getSqlInsertDatum() {
		return sqlInsertDatum;
	}

	/**
	 * @param sqlInsertDatum the sqlInsertDatum to set
	 */
	public void setSqlInsertDatum(String sqlInsertDatum) {
		this.sqlInsertDatum = sqlInsertDatum;
	}

	/**
	 * @return the uploadTableName
	 */
	public String getUploadTableName() {
		return uploadTableName;
	}

	/**
	 * @param uploadTableName the uploadTableName to set
	 */
	public void setUploadTableName(String uploadTableName) {
		this.uploadTableName = uploadTableName;
	}

	/**
	 * @return the ignoreMockData
	 */
	public boolean isIgnoreMockData() {
		return ignoreMockData;
	}

	/**
	 * @param ignoreMockData the ignoreMockData to set
	 */
	public void setIgnoreMockData(boolean ignoreMockData) {
		this.ignoreMockData = ignoreMockData;
	}

}

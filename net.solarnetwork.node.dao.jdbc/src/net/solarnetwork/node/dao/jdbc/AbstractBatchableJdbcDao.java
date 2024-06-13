/* ==================================================================
 * AbstractBatchableJdbcDao.java - Nov 5, 2012 11:12:42 AM
 *
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.node.dao.BasicBatchResult;
import net.solarnetwork.node.dao.BatchableDao;

/**
 * Base class for {@link BatchableDao} implementations.
 *
 * @param <T>
 *        the type of domain object this DAO supports
 * @author matt
 * @version 1.5
 */
public abstract class AbstractBatchableJdbcDao<T> extends JdbcDaoSupport implements BatchableDao<T> {

	private TransactionTemplate transactionTemplate;
	private String sqlForUpdateSuffix = " FOR UPDATE";
	private String sqlResourcePrefix = null;

	private final Map<String, String> sqlResourceCache = new HashMap<>(10);

	/**
	 * A class-level logger.
	 *
	 * @since 1.3
	 */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 */
	public AbstractBatchableJdbcDao() {
		super();
	}

	/**
	 * Get the SQL statement to use for batch processing.
	 *
	 * @param options
	 *        the requested batch options
	 * @return the SQL query
	 */
	protected abstract String getBatchJdbcStatement(BatchOptions options);

	/**
	 * Get an entity from the current row in a ResultSet for batch processing.
	 *
	 * @param options
	 *        the requested batch options
	 * @param resultSet
	 *        the current ResultSet, positioned on the next row
	 * @param rowCount
	 *        the current count of rows processed (1-based)
	 * @return the entity
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	protected abstract T getBatchRowEntity(BatchOptions options, ResultSet resultSet, int rowCount)
			throws SQLException;

	/**
	 * Update the current row in a ResulSet for batch processing.
	 *
	 * <p>
	 * The {@link ResultSet#updateRow()} method should <strong>not</strong> be
	 * called within this method.
	 * </p>
	 *
	 * @param options
	 *        the requested batch options
	 * @param resultSet
	 *        the current ResultSet, positioned on the next row
	 * @param rowCount
	 *        the current count of rows processed (1-based)
	 * @param entity
	 *        the entity data to update
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	protected abstract void updateBatchRowEntity(BatchOptions options, ResultSet resultSet, int rowCount,
			T entity) throws SQLException;

	@Override
	public BatchResult batchProcess(final BatchCallback<T> callback, final BatchOptions options) {
		if ( transactionTemplate != null ) {
			return transactionTemplate.execute(new TransactionCallback<BatchResult>() {

				@Override
				public net.solarnetwork.node.dao.BatchableDao.BatchResult doInTransaction(
						TransactionStatus status) {
					return batchProcessInternal(callback, options);
				}
			});
		} else {
			return batchProcessInternal(callback, options);
		}
	}

	private BatchResult batchProcessInternal(final BatchCallback<T> callback,
			final BatchOptions options) {
		final String querySql = getBatchJdbcStatement(options);
		final AtomicInteger rowCount = new AtomicInteger(0);
		getJdbcTemplate().execute(new ConnectionCallback<Object>() {

			@Override
			public net.solarnetwork.node.dao.BatchableDao.BatchResult doInConnection(Connection con)
					throws SQLException, DataAccessException {
				PreparedStatement queryStmt = null;
				ResultSet queryResult = null;
				DatabaseMetaData meta = con.getMetaData();
				int scrollType = (options.isUpdatable()
						? (meta.supportsResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE)
								? ResultSet.TYPE_SCROLL_SENSITIVE
								: ResultSet.TYPE_SCROLL_INSENSITIVE)
						: ResultSet.TYPE_FORWARD_ONLY);
				int concurType = (options.isUpdatable() ? ResultSet.CONCUR_UPDATABLE
						: ResultSet.CONCUR_READ_ONLY);
				try {
					queryStmt = con.prepareStatement(querySql, scrollType, concurType,
							ResultSet.CLOSE_CURSORS_AT_COMMIT);
					queryResult = queryStmt.executeQuery();
					while ( queryResult.next() ) {
						T entity = getBatchRowEntity(options, queryResult, rowCount.incrementAndGet());
						BatchCallbackResult rowResult = callback.handle(entity);
						switch (rowResult) {
							case CONTINUE:
								break;
							case STOP:
								return null;
							case DELETE:
								queryResult.deleteRow();
								break;
							case UPDATE:
							case UPDATE_STOP:
								updateBatchRowEntity(options, queryResult, rowCount.intValue(), entity);
								queryResult.updateRow();
								if ( rowResult == BatchCallbackResult.UPDATE_STOP ) {
									return null;
								}
								break;
						}
					}
				} finally {
					if ( queryResult != null ) {
						queryResult.close();
					}
					if ( queryStmt != null ) {
						queryStmt.close();
					}
				}

				return null;
			}
		});
		return new BasicBatchResult(rowCount.intValue());
	}

	/**
	 * Load a classpath SQL resource into a string.
	 *
	 * @param classPathResource
	 *        the classpath resource to load as a SQL string
	 * @return the SQL
	 * @see JdbcUtils#getSqlResource(String, Class, String, Map)
	 * @since 1.5
	 */
	protected String getSqlResource(String classPathResource) {
		return JdbcUtils.getSqlResource(classPathResource, getClass(), getSqlResourcePrefix(),
				sqlResourceCache);
	}

	/**
	 * Load a SQL resource into a String.
	 *
	 * @param resource
	 *        the SQL resource to load
	 * @return the SQL
	 * @see JdbcUtils#getSqlResource(Resource)
	 * @since 1.5
	 */
	protected String getSqlResource(Resource resource) {
		return JdbcUtils.getSqlResource(resource);
	}

	/**
	 * Get batch SQL statements, split into multiple statements on the
	 * {@literal ;} character.
	 *
	 * @param sqlResource
	 *        the SQL resource to load
	 * @return split SQL
	 * @see JdbcUtils#getBatchSqlResource(Resource)
	 * @since 1.5
	 */
	protected String[] getBatchSqlResource(Resource sqlResource) {
		return JdbcUtils.getBatchSqlResource(sqlResource);
	}

	/**
	 * Get the transaction template.
	 *
	 * @return the template
	 */
	public TransactionTemplate getTransactionTemplate() {
		return transactionTemplate;
	}

	/**
	 * Set the transaction template.
	 *
	 * @param transactionTemplate
	 *        the template to set
	 */
	public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * Set a SQL fragment to append to SQL statements where an updatable result
	 * set is desired.
	 *
	 * @return the SQL suffix, or {@literal null} if not desired
	 * @since 1.4
	 */
	public String getSqlForUpdateSuffix() {
		return sqlForUpdateSuffix;
	}

	/**
	 * Set a SQL fragment to append to SQL statements where an updatable result
	 * set is desired.
	 *
	 * <p>
	 * This defaults to {@literal FOR UPDATE}. <b>Note</b> a space must be
	 * included at the beginning. Set to {@literal null} to disable.
	 * </p>
	 *
	 * @param sqlForUpdateSuffix
	 *        the suffix to set
	 * @since 1.4
	 */
	public void setSqlForUpdateSuffix(String sqlForUpdateSuffix) {
		this.sqlForUpdateSuffix = sqlForUpdateSuffix;
	}

	/**
	 * Get a SQL resource prefix.
	 *
	 * @return the prefix
	 * @since 1.5
	 */
	public String getSqlResourcePrefix() {
		return sqlResourcePrefix;
	}

	/**
	 * Set a SQL resource prefix.
	 *
	 * @param sqlResourcePrefix
	 *        the prefix to set
	 * @since 1.5
	 */
	public void setSqlResourcePrefix(String sqlResourcePrefix) {
		this.sqlResourcePrefix = sqlResourcePrefix;
	}

}

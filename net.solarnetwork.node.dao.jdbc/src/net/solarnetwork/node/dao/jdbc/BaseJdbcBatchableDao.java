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
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.dao.BasicBatchResult;
import net.solarnetwork.dao.BatchableDao;
import net.solarnetwork.dao.Entity;

/**
 * Base class for {@link BatchableDao} implementations.
 *
 * @param <T>
 *        the type of domain object this DAO supports
 * @param <K>
 *        the primary key type
 * @author matt
 * @version 1.2
 * @since 1.29
 */
public abstract class BaseJdbcBatchableDao<T extends Entity<K>, K> extends BaseJdbcGenericDao<T, K>
		implements BatchableDao<T> {

	private TransactionTemplate transactionTemplate;
	private String sqlForUpdateSuffix = " FOR UPDATE";

	/**
	 * Init with an an entity name and table version, deriving various names
	 * based on conventions.
	 *
	 * @param objectType
	 *        the entity type
	 * @param keyType
	 *        the key type
	 * @param rowMapper
	 *        a mapper to use when mapping entity query result rows to entity
	 *        objects
	 * @param tableNameTemplate
	 *        a template with a {@code %s} parameter for the SQL table name
	 * @param entityName
	 *        The entity name to use. This name forms the basis of the default
	 *        SQL resource prefix, table name, tables version query, and SQL
	 *        init resource.
	 * @param version
	 *        the tables version, to manage DDL migrations
	 */
	public BaseJdbcBatchableDao(Class<? extends T> objectType, Class<? extends K> keyType,
			RowMapper<T> rowMapper, String tableNameTemplate, String entityName, int version) {
		super(objectType, keyType, rowMapper, tableNameTemplate, entityName, version);
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
	 * Prepare the batch statement.
	 *
	 * <p>
	 * This implementation does nothing. Extending classes might need to set
	 * parameters.
	 * </p>
	 *
	 * @param options
	 *        the batch options
	 * @param con
	 *        the SQL connection
	 * @param queryStmt
	 *        the SQL statement
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	protected void prepareBatchStatement(BatchOptions options, Connection con,
			PreparedStatement queryStmt) throws SQLException {
		// extending classes can override if needed
	}

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

	/**
	 * Callback when deleting the current row in a ResulSet during batch
	 * processing.
	 *
	 * <p>
	 * The {@link ResultSet#deleteRow()} method should <strong>not</strong> be
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
	 *        the entity to be deleted
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 1.1
	 */
	protected void willDeleteBatchRowEntity(BatchOptions options, ResultSet resultSet, int rowCount,
			T entity) throws SQLException {
		// extending classes can override
	}

	@Override
	public BatchResult batchProcess(final BatchCallback<T> callback, final BatchOptions options) {
		if ( transactionTemplate != null ) {
			return transactionTemplate.execute(new TransactionCallback<BatchResult>() {

				@Override
				public net.solarnetwork.dao.BatchableDao.BatchResult doInTransaction(
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
			public BatchableDao.BatchResult doInConnection(Connection con)
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
					prepareBatchStatement(options, con, queryStmt);
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
								willDeleteBatchRowEntity(options, queryResult, rowCount.intValue(),
										entity);
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
	@Override
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
	@Override
	public void setSqlForUpdateSuffix(String sqlForUpdateSuffix) {
		this.sqlForUpdateSuffix = sqlForUpdateSuffix;
	}

}

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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import net.solarnetwork.node.dao.BasicBatchResult;
import net.solarnetwork.node.dao.BatchableDao;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Base class for {@link BatchableDao} implementations.
 * 
 * @param <T>
 *        the type of domain object this DAO supports
 * @author matt
 * @version 1.1
 */
public abstract class AbstractBatchableJdbcDao<T> extends JdbcDaoSupport implements BatchableDao<T> {

	private TransactionTemplate transactionTemplate;

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
	protected abstract void updateBatchRowEntity(BatchOptions options, ResultSet resultSet,
			int rowCount, T entity) throws SQLException;

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

	private BatchResult batchProcessInternal(final BatchCallback<T> callback, final BatchOptions options) {
		final String querySql = getBatchJdbcStatement(options);
		final AtomicInteger rowCount = new AtomicInteger(0);
		getJdbcTemplate().execute(new ConnectionCallback<Object>() {

			@Override
			public net.solarnetwork.node.dao.BatchableDao.BatchResult doInConnection(Connection con)
					throws SQLException, DataAccessException {
				PreparedStatement queryStmt = null;
				ResultSet queryResult = null;
				try {
					queryStmt = con.prepareStatement(querySql,
							(options.isUpdatable() ? ResultSet.TYPE_SCROLL_SENSITIVE
									: ResultSet.TYPE_FORWARD_ONLY),
							(options.isUpdatable() ? ResultSet.CONCUR_UPDATABLE
									: ResultSet.CONCUR_READ_ONLY), ResultSet.CLOSE_CURSORS_AT_COMMIT);
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

	public TransactionTemplate getTransactionTemplate() {
		return transactionTemplate;
	}

	public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

}

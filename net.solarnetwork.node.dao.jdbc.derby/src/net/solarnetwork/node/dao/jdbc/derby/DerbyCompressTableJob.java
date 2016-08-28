/* ===================================================================
 * DerbyCompressTableJob.java
 * 
 * Created Sep 29, 2008 10:33:04 AM
 * 
 * Copyright (c) 2008 Solarnetwork.net Dev Team.
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

package net.solarnetwork.node.dao.jdbc.derby;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.node.job.AbstractJob;

/**
 * Job to run the Derby SYSCS_UTIL.SYSCS_INPLACE_COMPRESS_TABLE procedure to
 * free up unused disk space.
 * 
 * <p>
 * This is important to run on nodes using Derby with limited disk space,
 * especially when the {@link net.solarnetwork.node.job.DatumDaoCleanerJob} is
 * also used.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>jdbcOperations</dt>
 * <dd>The {@link JdbcOperations} to use for executing SQL statements.</dd>
 * 
 * <dt>schema</dt>
 * <dd>The database schema name to compress, when combined with the
 * {@code table} property..</dd>
 * 
 * <dt>table</dt>
 * <dd>The database table name to compress, when combined with the
 * {@code schema} property.</dd>
 * 
 * <dt>purgeRows</dt>
 * <dd>Boolean flag to pass to the SYSCS_UTIL.SYSCS_INPLACE_COMPRESS_TABLE
 * function. Defaults to <em>true</em>.</dd>
 * 
 * <dt>defragmentRows</dt>
 * <dd>Boolean flag to pass to the SYSCS_UTIL.SYSCS_INPLACE_COMPRESS_TABLE
 * function. Defaults to <em>true</em>.</dd>
 * 
 * <dt>truncateEnd</dt>
 * <dd>Boolean flag to pass to the SYSCS_UTIL.SYSCS_INPLACE_COMPRESS_TABLE
 * function. Defaults to <em>true</em>.</dd>
 * </dl>
 *
 * @author matt
 * @version 2.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class DerbyCompressTableJob extends AbstractJob {

	private static final String COMPRESS_CALL = "CALL SYSCS_UTIL.SYSCS_INPLACE_COMPRESS_TABLE(?, ?, ?, ?, ?)";

	private JdbcOperations jdbcOperations;
	private String schema = "SOLARNODE";
	private String table = "SN_POWER_DATUM";
	private boolean purgeRows = true;
	private boolean defragmentRows = true;
	private boolean truncateEnd = true;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		if ( log.isDebugEnabled() ) {
			log.debug(
					"Compressing Derby table " + schema + '.' + table + " with purgeRows = " + purgeRows
							+ ", defragmentRows = " + defragmentRows + ", truncateEnd = " + truncateEnd);
		}
		jdbcOperations.execute(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				if ( log.isTraceEnabled() ) {
					log.trace("Preparing Derby compress table call [" + COMPRESS_CALL + ']');
				}
				return con.prepareCall(COMPRESS_CALL);
			}
		}, new CallableStatementCallback<Object>() {

			@Override
			public Object doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				int idx = 1;
				cs.setString(idx++, schema);
				cs.setString(idx++, table);
				cs.setShort(idx++, purgeRows ? (short) 1 : (short) 0);
				cs.setShort(idx++, defragmentRows ? (short) 1 : (short) 0);
				cs.setShort(idx++, truncateEnd ? (short) 1 : (short) 0);
				boolean result = cs.execute();
				if ( log.isTraceEnabled() ) {
					log.trace("Derby compress table call returned [" + result + ']');
				}
				return null;
			}
		});
		if ( log.isInfoEnabled() ) {
			log.info("Compressed Derby table " + schema + '.' + table);
		}
	}

	/**
	 * @return the jdbcOperations
	 */
	public JdbcOperations getJdbcOperations() {
		return jdbcOperations;
	}

	/**
	 * @param jdbcOperations
	 *        the jdbcOperations to set
	 */
	public void setJdbcOperations(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}

	/**
	 * @return the schema
	 */
	public String getSchema() {
		return schema;
	}

	/**
	 * @param schema
	 *        the schema to set
	 */
	public void setSchema(String schema) {
		this.schema = schema;
	}

	/**
	 * @return the table
	 */
	public String getTable() {
		return table;
	}

	/**
	 * @param table
	 *        the table to set
	 */
	public void setTable(String table) {
		this.table = table;
	}

	/**
	 * @return the purgeRows
	 */
	public boolean isPurgeRows() {
		return purgeRows;
	}

	/**
	 * @param purgeRows
	 *        the purgeRows to set
	 */
	public void setPurgeRows(boolean purgeRows) {
		this.purgeRows = purgeRows;
	}

	/**
	 * @return the defragmentRows
	 */
	public boolean isDefragmentRows() {
		return defragmentRows;
	}

	/**
	 * @param defragmentRows
	 *        the defragmentRows to set
	 */
	public void setDefragmentRows(boolean defragmentRows) {
		this.defragmentRows = defragmentRows;
	}

	/**
	 * @return the truncateEnd
	 */
	public boolean isTruncateEnd() {
		return truncateEnd;
	}

	/**
	 * @param truncateEnd
	 *        the truncateEnd to set
	 */
	public void setTruncateEnd(boolean truncateEnd) {
		this.truncateEnd = truncateEnd;
	}

}

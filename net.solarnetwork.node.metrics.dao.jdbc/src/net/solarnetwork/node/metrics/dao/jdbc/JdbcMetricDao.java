/* ==================================================================
 * JdbcMetricDao.java - 14/07/2024 11:10:34 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.metrics.dao.jdbc;

import static java.lang.String.format;
import static net.solarnetwork.node.metrics.dao.jdbc.Constants.TABLE_NAME_TEMPALTE;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.node.dao.jdbc.BaseJdbcBatchableDao;
import net.solarnetwork.node.metrics.dao.BasicMetricFilter;
import net.solarnetwork.node.metrics.dao.MetricDao;
import net.solarnetwork.node.metrics.dao.MetricFilter;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.metrics.domain.MetricKey;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.StatTracker;

/**
 * JDBC implementation of {@link MetricDao}.
 *
 * @author matt
 * @version 1.3
 */
public class JdbcMetricDao extends BaseJdbcBatchableDao<Metric, MetricKey>
		implements MetricDao, SettingSpecifierProvider {

	/**
	 * The default SQL template for the {@code sqlGetTablesVersion} property.
	 * The {@link #getTableName()} value is used in the pattern.
	 */
	public static final String SQL_GET_TABLES_VERSION_TEMPLATE = "SELECT svalue FROM solarnode.mtr_metric_meta WHERE skey = 'solarnode.%s.version'";

	/** The JDBC table name. */
	public static final String TABLE_NAME = "metric";

	/** The JDBC table version. */
	public static final int VERSION = 1;

	/** The {@code stats.logFrequency} default value. */
	public static final int DEFAULT_STAT_LOG_FREQUENCY = 100;

	/** An internal batch parameter for a {@link PreparedStatementSetter}. */
	private static final String BATCH_PARAM_PSC = "_pss";

	/**
	 * Enumeration of SQL resources.
	 */
	public enum SqlResource {

		/** Get a count of stored records. */
		Count("count"),

		;

		private final String resource;

		private SqlResource(String resource) {
			this.resource = resource;
		}

		/**
		 * Get the SQL resource name.
		 *
		 * @return the resource
		 */
		public String getResource() {
			return resource;
		}
	}

	private final StatTracker stats;

	/**
	 * Constructor.
	 */
	public JdbcMetricDao() {
		super(Metric.class, MetricKey.class, MetricRowMapper.INSTANCE, TABLE_NAME_TEMPALTE, TABLE_NAME,
				VERSION);
		setSqlResourcePrefix(TABLE_NAME);
		setSqlGetTablesVersion(format(SQL_GET_TABLES_VERSION_TEMPLATE, TABLE_NAME));
		setInitSqlResource(
				new ClassPathResource(format(INIT_SQL_FORMAT, getSqlResourcePrefix()), getClass()));
		this.stats = new StatTracker("JdbcMetricDao", null, log, DEFAULT_STAT_LOG_FREQUENCY);
	}

	@Override
	public void init() {
		super.init();
	}

	@Override
	public MetricKey save(Metric entity) {
		insertDomainObject(entity, getSqlResource(SQL_INSERT));
		stats.increment(MetricDaoStat.MetricsStored);
		postEntityEvent(entity.getId(), entity, EntityEventType.STORED);
		return entity.getId();
	}

	@Override
	public FilterResults<Metric, MetricKey> findFiltered(MetricFilter filter, List<SortDescriptor> sorts,
			Long offset, Integer max) {
		SelectMetrics sql = new SelectMetrics(filter);
		List<Metric> results = getJdbcTemplate().query(sql, getRowMapper());

		Long totalResultCount = null;
		if ( !filter.isWithoutTotalResultsCount() ) {
			totalResultCount = getJdbcTemplate().query(sql.countPreparedStatementCreator(),
					new ResultSetExtractor<Long>() {

						@Override
						public Long extractData(ResultSet rs) throws SQLException, DataAccessException {
							return rs.next() ? rs.getLong(1) : null;
						}
					});
		}

		return new BasicFilterResults<>(results, totalResultCount,
				(filter.getOffset() != null ? filter.getOffset() : 0), results.size());
	}

	@Override
	protected String getBatchJdbcStatement(BatchOptions options) {
		requireNonNullArgument(options, "options");
		Map<String, Object> params = requireNonNullArgument(options.getParameters(),
				"options.parameters");

		MetricFilter filter = null;
		if ( options.getParameters() != null
				&& options.getParameters().get(BATCH_PARAM_FILTER) instanceof MetricFilter ) {
			filter = (MetricFilter) options.getParameters().get(BATCH_PARAM_FILTER);
		} else {
			filter = new BasicMetricFilter();
		}
		SelectMetrics sql = new SelectMetrics(filter);
		params.put(BATCH_PARAM_PSC, sql);
		return sql.getSql();
	}

	@Override
	protected void prepareBatchStatement(BatchOptions options, Connection con,
			PreparedStatement queryStmt) throws SQLException {
		requireNonNullArgument(options, "options");
		Map<String, Object> params = requireNonNullArgument(options.getParameters(),
				"options.parameters");
		if ( !(params.get(BATCH_PARAM_PSC) instanceof PreparedStatementSetter) ) {
			throw new IllegalStateException(
					"PreparedStatementSetter not available on " + BATCH_PARAM_PSC + " parameter.");
		}
		PreparedStatementSetter pss = (PreparedStatementSetter) params.get(BATCH_PARAM_PSC);
		pss.setValues(queryStmt);
	}

	@Override
	protected Metric getBatchRowEntity(BatchOptions options, ResultSet resultSet, int rowCount)
			throws SQLException {
		return getRowMapper().mapRow(resultSet, rowCount);
	}

	@Override
	protected void updateBatchRowEntity(BatchOptions options, ResultSet resultSet, int rowCount,
			Metric entity) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void willDeleteBatchRowEntity(BatchOptions options, ResultSet queryResult, int intValue,
			Metric entity) throws SQLException {
		super.willDeleteBatchRowEntity(options, queryResult, intValue, entity);
		if ( TransactionSynchronizationManager.isSynchronizationActive() ) {
			TransactionSynchronization txSynchronization = new TransactionSynchronization() {

				@Override
				public void afterCommit() {
					stats.increment(MetricDaoStat.MetricsDeleted);
					postEntityEvent(entity.getId(), entity, EntityEventType.DELETED);
				}

			};
			TransactionSynchronizationManager.registerSynchronization(txSynchronization);
		} else {
			stats.increment(MetricDaoStat.MetricsDeleted);
		}
	}

	@Override
	public int deleteFiltered(MetricFilter filter) {
		DeleteMetrics sql = new DeleteMetrics(filter);
		int result = getJdbcTemplate().update(sql);
		if ( result > 0 ) {
			stats.increment(MetricDaoStat.MetricsDeleted, result);
		}
		return result;
	}

	@Override
	protected Object[] primaryKeyArguments(MetricKey id) {
		return new Object[] { id.getTimestamp(), id.getType(), id.getName() };
	}

	@Override
	protected void setStoreStatementValues(Metric obj, PreparedStatement ps) throws SQLException {
		ps.setObject(1, obj.getTimestamp());
		ps.setString(2, obj.getType());
		ps.setString(3, obj.getName());
		ps.setDouble(4, obj.getValue());
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.metrics.dao.jdbc.metrics";
	}

	@Override
	public String getDisplayName() {
		return "JDBC Metrics DAO";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);
		result.add(new BasicTitleSettingSpecifier("status", getStatusMessage(), true, true));
		result.add(new BasicTextFieldSettingSpecifier("statLogFrequency",
				String.valueOf(DEFAULT_STAT_LOG_FREQUENCY)));
		return result;
	}

	private String getStatusMessage() {
		// @formatter:off
		long rowCount = 0;
		try {
			rowCount = rowCount();
		} catch ( Exception e ) {
			log.warn("Error finding metric row count.", e);
		}
		return getMessageSource().getMessage("status.msg",
				new Object[] {
						rowCount,
						stats.get(MetricDaoStat.MetricsStored),
						stats.get(MetricDaoStat.MetricsDeleted) },
				Locale.getDefault());
		// @formatter:on
	}

	private long rowCount() {
		final Number rowCountNum = getJdbcTemplate()
				.queryForObject(getSqlResource(SqlResource.Count.getResource()), Number.class);
		return (rowCountNum == null ? 0 : rowCountNum.longValue());
	}

	/**
	 * Get the statistics.
	 *
	 * @return the statistics, never {@literal null}
	 */
	public final StatTracker getStats() {
		return stats;
	}

	/**
	 * Get the statistic log frequency.
	 *
	 * @return the log frequency
	 */
	public final int getStatLogFrequency() {
		return stats.getLogFrequency();
	}

	/**
	 * Set the statistic log frequency.
	 *
	 * @param logFrequency
	 *        the log frequency to set
	 */
	public final void setStatLogFrequency(int logFrequency) {
		stats.setLogFrequency(logFrequency);
	}

}

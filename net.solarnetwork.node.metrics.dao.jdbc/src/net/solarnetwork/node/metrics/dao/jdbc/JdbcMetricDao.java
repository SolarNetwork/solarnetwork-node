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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.core.io.ClassPathResource;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.node.dao.jdbc.BaseJdbcGenericDao;
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
 * @version 1.0
 */
public class JdbcMetricDao extends BaseJdbcGenericDao<Metric, MetricKey>
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
		return entity.getId();
	}

	@Override
	public FilterResults<Metric, MetricKey> findFiltered(MetricFilter filter, List<SortDescriptor> sorts,
			Integer offset, Integer max) {
		SelectMetrics sql = new SelectMetrics(filter);
		List<Metric> results = getJdbcTemplate().query(sql, getRowMapper());
		return new BasicFilterResults<>(results);
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

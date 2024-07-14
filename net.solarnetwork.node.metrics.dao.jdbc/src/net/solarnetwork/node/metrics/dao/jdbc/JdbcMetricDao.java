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
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.node.dao.jdbc.BaseJdbcGenericDao;
import net.solarnetwork.node.metrics.dao.MetricDao;
import net.solarnetwork.node.metrics.dao.MetricFilter;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.metrics.domain.MetricKey;

/**
 * JDBC implementation of {@link MetricDao}.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcMetricDao extends BaseJdbcGenericDao<Metric, MetricKey> implements MetricDao {

	/**
	 * The default SQL template for the {@code sqlGetTablesVersion} property.
	 * The {@link #getTableName()} value is used in the pattern.
	 */
	public static final String SQL_GET_TABLES_VERSION_TEMPLATE = "SELECT svalue FROM solarnode.mtr_metric_meta WHERE skey = 'solarnode.%s.version'";

	/** The JDBC table name. */
	public static final String TABLE_NAME = "metric";

	/** The JDBC table version. */
	public static final int VERSION = 1;

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
	protected void setUpdateStatementValues(Metric obj, PreparedStatement ps) throws SQLException {
		setInsertStatementValues(obj, ps, 0);
	}

	private void setInsertStatementValues(Metric obj, PreparedStatement ps, int offset)
			throws SQLException {
		ps.setObject(1 + offset, obj.getTimestamp());
		ps.setString(2 + offset, obj.getType());
		ps.setString(3 + offset, obj.getName());
		ps.setDouble(4 + offset, obj.getValue());
	}

}

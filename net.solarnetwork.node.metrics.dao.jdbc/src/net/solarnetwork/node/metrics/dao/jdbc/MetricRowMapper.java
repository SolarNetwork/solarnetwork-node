/* ==================================================================
 * MetricRowMapper.java - 14/07/2024 11:11:54 am
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.node.metrics.domain.Metric;

/**
 * A row mapper for {@link Metric} entities.
 *
 * <p>
 * The column order supported by this mapper is:
 * </p>
 *
 * <ol>
 * <li>ts (TIMESTAMP)</li>
 * <li>type (VARCHAR)</li>
 * <li>name (VARCHAR)</li>
 * <li>value (DOUBLE PRECISION)</li>
 * <li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class MetricRowMapper implements RowMapper<Metric> {

	/** A default instance. */
	public static final RowMapper<Metric> INSTANCE = new MetricRowMapper();

	/**
	 * Constructor.
	 */
	public MetricRowMapper() {
		super();
	}

	@Override
	public Metric mapRow(ResultSet rs, int rowNum) throws SQLException {
		Instant ts = JdbcUtils.instantColumn(rs, 1);
		String type = rs.getString(2);
		String name = rs.getString(3);
		double val = rs.getDouble(4);
		return Metric.metricValue(ts, type, name, val);
	}

}

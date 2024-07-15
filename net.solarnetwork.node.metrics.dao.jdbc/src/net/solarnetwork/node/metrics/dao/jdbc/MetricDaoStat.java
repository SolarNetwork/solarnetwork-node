/* ==================================================================
 * MetricDaoStat.java - 15/07/2024 3:15:10 pm
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

/**
 * Statics for the metric DAO.
 *
 * @author matt
 * @version 1.0
 */
public enum MetricDaoStat {

	/** Messages stored. */
	MetricsStored("metrics stored"),

	/** Messages deleted. */
	MetricsDeleted("metrics deleted"),

	;

	private final String description;

	private MetricDaoStat(String description) {
		this.description = description;
	}

	/**
	 * Get the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}

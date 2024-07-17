/* ==================================================================
 * BasicMetricFilter.java - 14/07/2024 10:57:36 am
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

package net.solarnetwork.node.metrics.dao;

import java.time.Instant;
import net.solarnetwork.domain.SimplePagination;
import net.solarnetwork.node.metrics.domain.MetricAggregate;

/**
 * Basic implementation of {@link MetricFilter}.
 *
 * @author matt
 * @version 1.0
 */
public class BasicMetricFilter extends SimplePagination implements MetricFilter {

	private Instant startDate;
	private Instant endDate;
	private String[] types;
	private String[] names;
	private MetricAggregate[] aggregates;
	private boolean withoutTotalResultsCount = true;

	/**
	 * Constructor.
	 */
	public BasicMetricFilter() {
		super();
	}

	@Override
	public final Instant getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date.
	 *
	 * @param startDate
	 *        the date to set
	 */
	public final void setStartDate(Instant startDate) {
		this.startDate = startDate;
	}

	@Override
	public final Instant getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date.
	 *
	 * @param endDate
	 *        the date to set
	 */
	public final void setEndDate(Instant endDate) {
		this.endDate = endDate;
	}

	@Override
	public final String[] getTypes() {
		return types;
	}

	/**
	 * Set the metric types.
	 *
	 * @param types
	 *        the types to set
	 */
	public final void setTypes(String[] types) {
		this.types = types;
	}

	/**
	 * Set the metric type.
	 *
	 * <p>
	 * This will clear out all existing {@link #getTypes()} values.
	 * </p>
	 *
	 * @param type
	 *        the type to set
	 */
	public void setType(String type) {
		setTypes(type != null && !type.isEmpty() ? new String[] { type } : null);
	}

	@Override
	public final String[] getNames() {
		return names;
	}

	/**
	 * Set the metric names.
	 *
	 * @param names
	 *        the names to set
	 */
	public final void setNames(String[] names) {
		this.names = names;
	}

	/**
	 * Set the metric name.
	 *
	 * <p>
	 * This will clear out all existing {@link #getNames()} values.
	 * </p>
	 *
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		setNames(name != null && !name.isEmpty() ? new String[] { name } : null);
	}

	@Override
	public final MetricAggregate[] getAggregates() {
		return aggregates;
	}

	/**
	 * Set the metric aggregates.
	 *
	 * @param aggregates
	 *        the aggregates to set
	 */
	public final void setAggregates(MetricAggregate[] aggregates) {
		this.aggregates = aggregates;
	}

	/**
	 * Toggle the "without total results" mode.
	 *
	 * @param mode
	 *        the mode to set
	 */
	public void setWithoutTotalResultsCount(boolean mode) {
		this.withoutTotalResultsCount = mode;
	}

	@Override
	public boolean isWithoutTotalResultsCount() {
		return withoutTotalResultsCount;
	}
}

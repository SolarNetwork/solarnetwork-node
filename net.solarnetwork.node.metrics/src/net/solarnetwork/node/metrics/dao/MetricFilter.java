/* ==================================================================
 * MetricFilter.java - 14/07/2024 10:51:28 am
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

import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.node.metrics.domain.MetricAggregate;

/**
 * Query filter API for {link net.solarnetwork.node.metrics.domain.Metric}
 * entities.
 *
 * @author matt
 * @version 1.0
 */
public interface MetricFilter extends DateRangeCriteria, PaginationCriteria {

	/**
	 * Get the first metric type.
	 *
	 * <p>
	 * This returns the first available type from the {@link #getTypes()} array,
	 * or {@literal null} if not available.
	 * </p>
	 *
	 * @return the first type, or {@literal null} if not available
	 */
	default String getType() {
		final String[] types = getTypes();
		return (types != null && types.length > 0 ? types[0] : null);
	}

	/**
	 * Get an array of metric types.
	 *
	 * @return array of types (may be {@literal null})
	 */
	String[] getTypes();

	/**
	 * Test if the filter has a type criteria specified.
	 *
	 * @return {@literal true} if {@link #getType()} is non-null and not empty
	 */
	default boolean hasTypeCriteria() {
		return getType() != null && !getType().isEmpty();
	}

	/**
	 * Get the first metric name.
	 *
	 * <p>
	 * This returns the first available name from the {@link #getNames()} array,
	 * or {@literal null} if not available.
	 * </p>
	 *
	 * @return the first name, or {@literal null} if not available
	 */
	default String getName() {
		final String[] names = getNames();
		return (names != null && names.length > 0 ? names[0] : null);
	}

	/**
	 * Get an array of metric names.
	 *
	 * @return array of names (may be {@literal null})
	 */
	String[] getNames();

	/**
	 * Test if the filter has a name criteria specified.
	 *
	 * @return {@literal true} if {@link #getName()} is non-null and not empty
	 */
	default boolean hasNameCriteria() {
		return getName() != null && !getName().isEmpty();
	}

	/**
	 * Get an array of desired output metric aggregates.
	 *
	 * @return the desired aggregates (may be {@literal null})
	 */
	MetricAggregate[] getAggregates();

	/**
	 * Test if the filter has an aggregate criteria specified.
	 *
	 * @return {@literal true} if {@link #getAggregates()} is non-null and not
	 *         empty
	 */
	default boolean hasAggregateCriteria() {
		return getAggregates() != null && getAggregates().length > 0;
	}

}

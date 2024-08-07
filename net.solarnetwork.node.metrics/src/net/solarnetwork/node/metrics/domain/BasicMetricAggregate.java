/* ==================================================================
 * BasicMetricAggregate.java - 14/07/2024 5:26:37 pm
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

package net.solarnetwork.node.metrics.domain;

/**
 * Basic metric aggregates, that have no parameters.
 *
 * @author matt
 * @version 1.1
 */
public enum BasicMetricAggregate implements MetricAggregate {

	/** A minimum. */
	Minimum(MetricAggregate.METRIC_TYPE_MINIMUM),

	/** A maximum. */
	Maximum(MetricAggregate.METRIC_TYPE_MAXIMUM),

	/** An average. */
	Average(MetricAggregate.METRIC_TYPE_AVERAGE),

	/**
	 * A count.
	 *
	 * @since 1.1
	 */
	Count(MetricAggregate.METRIC_TYPE_COUNT),

	/**
	 * A sum.
	 *
	 * @since 1.1
	 */
	Sum(MetricAggregate.METRIC_TYPE_SUM),

	;

	private final String type;

	private BasicMetricAggregate(String type) {
		this.type = type;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public Object[] getParameters() {
		return null;
	}

	/**
	 * Get an enum instance for a type value.
	 *
	 * <p>
	 * The {@code type} is compared in a case-insensitive manner against both
	 * {@link #getType()} and the {@code name} values.
	 * </p>
	 *
	 * @param type
	 *        the type value to get an enumeration instance for
	 * @return the enumeration instance
	 * @throws IllegalArgumentException
	 *         if {@code type} is not a valid value
	 * @since 1.1
	 */
	public static BasicMetricAggregate forType(String type) {
		for ( BasicMetricAggregate agg : BasicMetricAggregate.values() ) {
			if ( agg.type.equalsIgnoreCase(type) || agg.name().equalsIgnoreCase(type) ) {
				return agg;
			}
		}
		throw new IllegalArgumentException(
				"The type [" + type + "] is not a supported BasicMetricAggregate value.");
	}

}

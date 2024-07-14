/* ==================================================================
 * MetricAggregate.java - 14/07/2024 5:24:53 pm
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

import net.solarnetwork.util.StringUtils;

/**
 * A metric aggregate.
 *
 * @author matt
 * @version 1.0
 */
public interface MetricAggregate {

	/** A metric type for an aggregate minimum sample. */
	String METRIC_TYPE_MINIMUM = "min";

	/** A metric type for an aggregate maximum sample. */
	String METRIC_TYPE_MAXIMUM = "max";

	/** A metric type for an aggregate maximum sample. */
	String METRIC_TYPE_AVERAGE = "avg";

	/** A metric type for an aggregate quantile sample. */
	String METRIC_TYPE_QUANTILE = "q";

	/**
	 * Get a unique type name.
	 *
	 * @return the type
	 */
	String getType();

	/**
	 * Get optional parameters.
	 *
	 * @return an optional list of parameters
	 */
	Object[] getParameters();

	/**
	 * Test if the aggregate provides any parameters.
	 *
	 * @return {@literal true} if {@link #getParameters()} is not
	 *         {@literal null} or empty
	 */
	default boolean hasParameter() {
		final Object[] p = getParameters();
		return (p != null && p.length > 0);
	}

	/**
	 * Get a parameter as a number.
	 *
	 * @param idx
	 *        the index of the parameter to get as a number
	 * @return the number, or {@literal null} if the parameter does not exist or
	 *         cannot be converted to a number
	 */
	default Number numberParameter(int idx) {
		final Object[] p = getParameters();
		if ( p == null || idx >= p.length ) {
			return null;
		}
		Object o = p[idx];
		if ( o instanceof Double ) {
			return (Double) o;
		} else if ( o instanceof Number ) {
			return ((Number) o).doubleValue();
		}
		return StringUtils.numberValue(o.toString());
	}

	/**
	 * Get a key name.
	 *
	 * <p>
	 * This implementation returns {@link #getType()}. Extending implementations
	 * may wish to return something else.
	 * </p>
	 *
	 * @return the key name
	 */
	default String key() {
		return getType();
	}

}

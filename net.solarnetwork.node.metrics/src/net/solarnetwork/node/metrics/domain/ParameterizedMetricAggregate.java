/* ==================================================================
 * ParameterizedMetricAggregate.java - 14/07/2024 5:29:18 pm
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

import java.util.function.Function;
import net.solarnetwork.util.ObjectUtils;

/**
 * Basic parameterized {@link MetricAggregate}.
 *
 * @author matt
 * @version 1.0
 */
public class ParameterizedMetricAggregate implements MetricAggregate {

	/**
	 * Key provider function that returns the aggregate type.
	 */
	public static final Function<MetricAggregate, String> DEFAULT_KEY = MetricAggregate::getType;

	/**
	 * Key provider function that appends an integer percentage suffix to the
	 * aggregate type.
	 */
	public static final Function<MetricAggregate, String> INTEGER_PERCENT_KEY = (agg) -> {
		Number p = agg.numberParameter(0);
		if ( p == null ) {
			return agg.getType();
		}
		return agg.getType() + ':' + (int) (p.doubleValue() * 100.0);
	};

	/** Quantile metric with a 25% parameter. */
	public static final MetricAggregate METRIC_TYPE_QUANTILE_25 = new ParameterizedMetricAggregate(
			METRIC_TYPE_QUANTILE, new Object[] { 0.25 }, INTEGER_PERCENT_KEY);

	/** Quantile metric with a 75% parameter. */
	public static final MetricAggregate METRIC_TYPE_QUANTILE_75 = new ParameterizedMetricAggregate(
			METRIC_TYPE_QUANTILE, new Object[] { 0.75 }, INTEGER_PERCENT_KEY);

	private final String type;
	private final Object[] parameters;
	private final Function<MetricAggregate, String> keyProvider;

	/**
	 * Constructor.
	 *
	 * @param type
	 *        the type
	 * @param parameters
	 *        the parameters
	 * @param keyProvider
	 *        function to provide the key value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ParameterizedMetricAggregate(String type, Object[] parameters,
			Function<MetricAggregate, String> keyProvider) {
		super();
		this.type = ObjectUtils.requireNonNullArgument(type, "type");
		this.parameters = ObjectUtils.requireNonEmptyArgument(parameters, "parameters");
		this.keyProvider = ObjectUtils.requireNonNullArgument(keyProvider, "keyProvider");
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public Object[] getParameters() {
		return parameters;
	}

	@Override
	public String key() {
		return keyProvider.apply(this);
	}

	/**
	 * Get the key provider.
	 *
	 * @return the key provider
	 */
	public final Function<MetricAggregate, String> keyProvider() {
		return keyProvider;
	}

}

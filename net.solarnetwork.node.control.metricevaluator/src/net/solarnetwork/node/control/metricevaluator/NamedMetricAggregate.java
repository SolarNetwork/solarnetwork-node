/* ==================================================================
 * NamedMetricAggregate.java - 25/07/2024 7:10:14 am
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

package net.solarnetwork.node.control.metricevaluator;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.metrics.domain.MetricAggregate;

/**
 * A {@link MetricAggregate} with an associated name.
 *
 * @author matt
 * @version 1.0
 */
public final class NamedMetricAggregate implements MetricAggregate {

	private final MetricAggregate delegate;
	private final String name;

	/**
	 * Create a named metric aggregate.
	 *
	 * <p>
	 * The given delegate's {@code key} will be used as the name.
	 * </p>
	 *
	 * @param delegate
	 *        the delegate aggregate
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public static NamedMetricAggregate namedMetricAggregate(MetricAggregate delegate) {
		return new NamedMetricAggregate(delegate, requireNonNullArgument(delegate, "delegate").key());
	}

	/**
	 * Create a named metric aggregate.
	 *
	 * @param delegate
	 *        the delegate aggregate
	 * @param name
	 *        the name
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public static NamedMetricAggregate namedMetricAggregate(MetricAggregate delegate, String name) {
		return new NamedMetricAggregate(delegate, name);
	}

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the delegate aggregate
	 * @param name
	 *        the name
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public NamedMetricAggregate(MetricAggregate delegate, String name) {
		super();
		this.delegate = requireNonNullArgument(delegate, "delegate");
		this.name = requireNonNullArgument(name, "name");
	}

	@Override
	public String getType() {
		return delegate.getType();
	}

	@Override
	public Object[] getParameters() {
		return delegate.getParameters();
	}

	@Override
	public boolean hasParameter() {
		return delegate.hasParameter();
	}

	@Override
	public Number numberParameter(int idx) {
		return delegate.numberParameter(idx);
	}

	@Override
	public String key() {
		return delegate.key();
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NamedMetricAggregate{");
		builder.append(keyNameValue());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get a key name value that includes the name mapping.
	 *
	 * @return the key name value
	 */
	public String keyNameValue() {
		String k = key();
		return (k.equals(name) ? k : k + '=' + name);
	}

	/**
	 * Get an aggregate parameter name value for a given metric.
	 *
	 * @param m
	 *        the metric
	 * @return the aggregate parameter name
	 */
	public String parameterNameValue(Metric m) {
		StringBuilder buf = new StringBuilder();
		buf.append(m.getName());
		buf.append('_');
		buf.append(name.replace(":", ""));
		return buf.toString();
	}

}

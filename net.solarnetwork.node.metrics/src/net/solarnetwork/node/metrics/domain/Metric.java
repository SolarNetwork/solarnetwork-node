/* ==================================================================
 * Metric.java - 14/07/2024 7:41:50 am
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

import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.domain.Differentiable;
import net.solarnetwork.util.ObjectUtils;

/**
 * A time-specific named number value.
 *
 * @author matt
 * @version 1.0
 */
public class Metric extends BasicEntity<MetricKey> implements Differentiable<Metric> {

	/** A metric type for a sample (reading) captured from a data source. */
	public static final String METRIC_TYPE_SAMPLE = "s";

	private static final long serialVersionUID = 5073245960163407375L;

	private final double value;

	/**
	 * Create a new metric instance.
	 *
	 * @param ts
	 *        the timestamp
	 * @param type
	 *        the type
	 * @param name
	 *        the name
	 * @param value
	 *        the value
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public static Metric metricValue(Instant ts, String type, String name, double value) {
		return new Metric(new MetricKey(ts, type, name), value);
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param value
	 *        the value
	 * @throws IllegalArgumentException
	 *         if the {@code id} argument is {@literal null}
	 */
	public Metric(MetricKey id, double value) {
		super(ObjectUtils.requireNonNullArgument(id, "id"), null);
		this.value = value;
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the other charge point to copy
	 */
	public Metric(Metric other) {
		this(other.getId(), other.value);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Metric{ts=");
		builder.append(getTimestamp());
		builder.append(", type=");
		builder.append(getType());
		builder.append(", name=");
		builder.append(getName());
		builder.append(", value=");
		builder.append(getValue());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 *
	 * <p>
	 * The {@code id} and {@code created} properties are not compared by this
	 * method.
	 * </p>
	 *
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(Metric other) {
		if ( other == null ) {
			return false;
		}
		return Objects.equals(value, other.value);
	}

	@Override
	public boolean differsFrom(Metric other) {
		return !isSameAs(other);
	}

	/**
	 * Get the timestamp.
	 *
	 * @return the timestamp
	 */
	public Instant getTimestamp() {
		return getId().getTimestamp();
	}

	/**
	 * Get the type.
	 *
	 * @return the type
	 */
	public String getType() {
		return getId().getType();
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return getId().getName();
	}

	/**
	 * Get the value.
	 *
	 * @return the value
	 */
	public double getValue() {
		return value;
	}

}

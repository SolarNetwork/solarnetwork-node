/* ==================================================================
 * MetricKey.java - 14/07/2024 7:44:56 am
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

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.util.ObjectUtils;

/**
 * Primary key for a metric.
 *
 * @author matt
 * @version 1.0
 */
public class MetricKey implements Serializable, Cloneable, Comparable<MetricKey> {

	private static final long serialVersionUID = 987923456336920228L;

	/** The timestamp. */
	private final Instant timestamp;

	/** The type. */
	private final String type;

	/** The name. */
	private final String name;

	/**
	 * Constructor.
	 *
	 * @param timestamp
	 *        the timestamp
	 * @param type
	 *        the type
	 * @param name
	 *        the name
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MetricKey(Instant timestamp, String type, String name) {
		super();
		this.timestamp = ObjectUtils.requireNonNullArgument(timestamp, "timestamp");
		this.type = ObjectUtils.requireNonNullArgument(type, "type");
		this.name = ObjectUtils.requireNonNullArgument(name, "name");
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MetricKey{timestamp=");
		builder.append(timestamp);
		builder.append(", type=");
		builder.append(type);
		builder.append(", name=");
		builder.append(name);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public int compareTo(MetricKey o) {
		int result = timestamp.compareTo(o.timestamp);
		if ( result == 0 ) {
			result = type.compareTo(o.type);
			if ( result == 0 ) {
				result = name.compareTo(o.name);
			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, type, name);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof MetricKey) ) {
			return false;
		}
		MetricKey other = (MetricKey) obj;
		return Objects.equals(timestamp, other.timestamp) && Objects.equals(type, other.type)
				&& Objects.equals(name, other.name);
	}

	/**
	 * Get the timestamp.
	 *
	 * @return the timestamp
	 */
	public final Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * Get the type.
	 *
	 * @return the type
	 */
	public final String getType() {
		return type;
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

}

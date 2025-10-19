/* ==================================================================
 * DateStringKey.java - 19/10/2025 6:26:05 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.domain.BaseId;

/**
 * A composite key of a string and timestamp.
 *
 * @author matt
 * @version 1.0
 */
public class StringDateKey extends BaseId implements Serializable, Cloneable, Comparable<StringDateKey> {

	@Serial
	private static final long serialVersionUID = 5231664826434959612L;

	/** The key. */
	private final String key;

	/** The timestamp. */
	private final Instant timestamp;

	/**
	 * Constructor.
	 *
	 * @param key
	 *        the key
	 * @param timestamp
	 *        the time stamp
	 */
	public StringDateKey(String key, Instant timestamp) {
		super();
		this.key = key;
		this.timestamp = timestamp;
	}

	@Override
	public StringDateKey clone() {
		return (StringDateKey) super.clone();
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("k=");
		if ( key != null ) {
			buf.append(key);
		}
		buf.append(";t=");
		if ( timestamp != null ) {
			buf.append(timestamp.getEpochSecond()).append('.').append(timestamp.getNano());
		}
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		if ( key != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("key=");
			buf.append(key);
		}
		if ( timestamp != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("timestamp=");
			buf.append(timestamp);
		}
	}

	@Override
	public int compareTo(StringDateKey o) {
		if ( this == o ) {
			return 0;
		}
		if ( o == null ) {
			return -1;
		}
		int result = 0;
		if ( key != o.key ) {
			if ( key == null ) {
				return 1;
			} else if ( o.key == null ) {
				return -1;
			}
			result = key.compareTo(o.key);
			if ( result != 0 ) {
				return result;
			}
		}
		if ( timestamp == o.timestamp ) {
			return 0;
		} else if ( timestamp == null ) {
			return 1;
		} else if ( o.timestamp == null ) {
			return -1;
		}
		return timestamp.compareTo(o.timestamp);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, timestamp);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof StringDateKey other) ) {
			return false;
		}
		return Objects.equals(key, other.key) && Objects.equals(timestamp, other.timestamp);
	}

	/**
	 * Get the key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get the timestamp.
	 *
	 * @return the timestamp
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

}

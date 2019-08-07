/* ==================================================================
 * DurationRange.java - 7/08/2019 2:52:13 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.esi.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * A duration range.
 * 
 * @author matt
 * @version 1.0
 */
public class DurationRange {

	private Duration min;
	private Duration max;

	/**
	 * Default constructor.
	 */
	public DurationRange() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param min
	 *        the minimum duration
	 * @param max
	 *        the maximum duration
	 */
	public DurationRange(Duration min, Duration max) {
		super();
		this.min = min;
		this.max = max;
	}

	/**
	 * Create a new duration out of second values.
	 * 
	 * @param min
	 *        the minimum value, in seconds
	 * @param max
	 *        the maximum value, in seconds
	 * @return the range instance
	 */
	public static DurationRange ofSeconds(long min, long max) {
		return new DurationRange(Duration.ofSeconds(min), Duration.ofSeconds(max));
	}

	public DurationRange copy() {
		return new DurationRange(getMin(), getMax());
	}

	@Override
	public int hashCode() {
		return Objects.hash(max, min);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof DurationRange) ) {
			return false;
		}
		DurationRange other = (DurationRange) obj;
		return Objects.equals(max, other.max) && Objects.equals(min, other.min);
	}

	@Override
	public String toString() {
		return "DurationRange{min=" + min + ", max=" + max + "}";
	}

	/**
	 * Get the minimum duration.
	 * 
	 * @return the minimum duration
	 */
	public Duration getMin() {
		return min;
	}

	/**
	 * Set the minimum duration.
	 * 
	 * @param min
	 *        the duration to set
	 */
	public void setMin(Duration min) {
		this.min = min;
	}

	/**
	 * Get the minimum duration, never {@literal null}.
	 */
	public Duration min() {
		Duration d = getMin();
		if ( d == null ) {
			d = Duration.ZERO;
		}
		return d;
	}

	/**
	 * Get the maximum duration.
	 * 
	 * @return the maximum duration
	 */
	public Duration getMax() {
		return max;
	}

	/**
	 * Set the maximum duration.
	 * 
	 * @param max
	 *        the duration to set
	 */
	public void setMax(Duration max) {
		this.max = max;
	}

	/**
	 * Get the maximum duration, never {@literal null}.
	 */
	public Duration max() {
		Duration d = getMax();
		if ( d == null ) {
			d = Duration.ZERO;
		}
		return d;
	}
}

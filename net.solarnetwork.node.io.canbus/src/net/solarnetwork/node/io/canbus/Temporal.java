/* ==================================================================
 * Temporal.java - 20/09/2019 1:12:47 pm
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

package net.solarnetwork.node.io.canbus;

import java.math.BigDecimal;

/**
 * API for something that has a relation to time within the CAN bus.
 * 
 * @author matt
 * @version 1.0
 */
public interface Temporal {

	/**
	 * Get a number of seconds.
	 * 
	 * @return the seconds
	 */
	int getSeconds();

	/**
	 * Get a number of microseconds.
	 * 
	 * @return the microseconds
	 */
	int getMicroseconds();

	/**
	 * Get the fractional seconds represented by the {@code seconds} and
	 * {@code microseconds}.
	 * 
	 * @return the fractional seconds, never {@literal null}
	 */
	default BigDecimal getFractionalSeconds() {
		return fractionalMicroseconds(getSeconds(), getMicroseconds());
	}

	/**
	 * Get the fractional seconds represented by the {@code seconds} and
	 * {@code microseconds} as a string.
	 * 
	 * @return the string value
	 */
	default String toFractionalSecondsString() {
		return String.format("%d.%06d", getSeconds(), getMicroseconds());
	}

	/**
	 * Get a fractional seconds value out of whole second and microsecond
	 * values.
	 * 
	 * @param seconds
	 *        the whole seconds
	 * @param microseconds
	 *        the whole microseconds
	 * @return the fractional seconds value
	 */
	static BigDecimal fractionalMicroseconds(int seconds, int microseconds) {
		return new BigDecimal(seconds).add(new BigDecimal(microseconds).scaleByPowerOfTen(-6));
	}

}

/* ==================================================================
 * GpioEdgeDetectionMode.java - 2/06/2023 7:23:28 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.gpiod;

import io.dvlopt.linux.gpio.GpioEdgeDetection;
import net.solarnetwork.domain.CodedValue;

/**
 * Enumeration of GPIO edge detection modes.
 * 
 * @author matt
 * @version 1.0
 */
public enum GpioEdgeDetectionMode implements CodedValue {

	/** The line is monitored only for rising signals. */
	Rising(1),

	/** The line is monitored only for falling signals. */
	Falling(2),

	/** The line is monitored for both rising and falling signals. */
	RisingAndFalling(3);

	private final int code;

	private GpioEdgeDetectionMode(int code) {
		this.code = code;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Convert to a {@link GpioEdgeDetection} instance.
	 * 
	 * @return the converted value
	 */
	public GpioEdgeDetection toEdgeDetection() {
		switch (this) {
			case Rising:
				return GpioEdgeDetection.RISING;

			case Falling:
				return GpioEdgeDetection.FALLING;

			case RisingAndFalling:
				return GpioEdgeDetection.RISING_AND_FALLING;
		}
		throw new UnsupportedOperationException(
				String.format("Cannot convert [%s] to GpioEdgeDetection", this));
	}

}

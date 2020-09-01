/* ==================================================================
 * EnableMode.java - 1/09/2020 7:08:30 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.ams.lux.tsl2591;

import net.solarnetwork.domain.Bitmaskable;

/**
 * Bitmask enumeration of modes, for use with the {@link Register#Enable}
 * register.
 * 
 * @author matt
 * @version 1.0
 */
public enum EnableMode implements Bitmaskable {

	Power(0, "Device power"),

	AmbientLightSensor(1, "Ambient light sensor"),

	AmbientLightSensorInterrupt(4, "Ambient light sensor interrupt"),

	SleepAfterInterrupt(6, "Sleep after interrupt"),

	NoPersistInterrupt(7, "No persist interrupt");

	private final int code;
	private final String description;

	private EnableMode(int code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get the description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	@Override
	public int bitmaskBitOffset() {
		return code;
	}

	/**
	 * Get an enum for a code value.
	 * 
	 * @param code
	 *        the code to get an enum for
	 * @return the enum with the given {@code code}, or {@literal null} if
	 *         {@code code} is {@literal 0}
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static EnableMode forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( EnableMode c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("EnableMode code [" + code + "] not supported");
	}

}

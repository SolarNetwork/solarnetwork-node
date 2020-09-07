/* ==================================================================
 * IntegrationTime.java - 31/08/2020 4:10:49 PM
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

import net.solarnetwork.domain.CodedValue;

/**
 * Enumeration of integration time values.
 * 
 * @author matt
 * @version 1.0
 */
public enum IntegrationTime implements CodedValue {

	/** 100ms integration time. */
	Time100ms(0, 100),

	/** 200ms integration time. */
	Time200ms(1, 200),

	/** 300ms integration time. */
	Time300ms(2, 300),

	/** 400ms integration time. */
	Time400ms(3, 400),

	/** 500ms integration time. */
	Time500ms(4, 500),

	/** 600ms integration time. */
	Time600ms(5, 600);

	private final int code;
	private final long duration;

	private IntegrationTime(int code, long duration) {
		this.code = code;
		this.duration = duration;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get the duration.
	 * 
	 * @return the duration, in milliseconds
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * Get an enumeration value for a code value.
	 * 
	 * @param code
	 *        the code
	 * @return the status, never {@literal null} and set to {@link #Time100ms}
	 *         if not any other valid code
	 */
	public static IntegrationTime forCode(int code) {
		final byte c = (byte) code;
		for ( IntegrationTime v : values() ) {
			if ( v.code == c ) {
				return v;
			}
		}
		return IntegrationTime.Time100ms;
	}

}

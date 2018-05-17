/* ==================================================================
 * DataUtils.java - 17/05/2018 3:47:31 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.schneider.meter;

import org.joda.time.LocalDateTime;

/**
 * Utilities for dealing with Schneider meter data.
 * 
 * @author matt
 * @version 1.0
 * @since 2.4
 */
public final class DataUtils {

	/**
	 * Parse a DateTime value from raw Modbus register values.
	 * 
	 * <p>
	 * The {@code data} array is expected to have a length of {@code 4}.
	 * </p>
	 * 
	 * @param data
	 *        the data array
	 * @return the parsed date, or {@literal null} if not available
	 */
	public static LocalDateTime parseDateTime(final int[] data) {
		LocalDateTime result = null;
		if ( data != null && data.length == 4 ) {
			int year = 2000 + (data[0] & 0x7F);
			int month = (data[1] & 0xF00) >> 8;
			int day = (data[1] & 0x1F);
			int hour = (data[2] & 0x1F00) >> 8;
			int minute = (data[2] & 0x3F);
			int ms = (data[3]); // this is really seconds + milliseconds
			int sec = ms / 1000;
			ms = ms - (sec * 1000);
			result = new LocalDateTime(year, month, day, hour, minute, sec, ms);
		}
		return result;
	}

}

/* ==================================================================
 * DateUtils.java - Oct 22, 2014 2:52:19 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.util;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Utility functions for dealing with dates and times.
 * 
 * @author matt
 * @version 1.0
 */
public final class DateUtils {

	private static final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormat.forPattern("HH:mm");
	private static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormat
			.forPattern("yyyy-MM-dd");

	private DateUtils() {
		// can't construct me
	}

	/**
	 * Parse a standard local time value, in {@code HH:mm} form.
	 * 
	 * @param value
	 *        the time value
	 * @return the LocalTime object
	 */
	public static LocalTime parseLocalTime(String value) {
		return LOCAL_TIME_FORMATTER.parseLocalTime(value);
	}

	/**
	 * Format a standard local time value, in {@code HH:mm} form.
	 * 
	 * @param value
	 *        the LocalTime to format
	 * @return the formatted value
	 */
	public static String format(LocalTime value) {
		return LOCAL_TIME_FORMATTER.print(value);
	}

	/**
	 * Parse a standard local date value, in {@code yyyy-MM-dd} form.
	 * 
	 * @param value
	 *        the date value
	 * @return the LocalDate object
	 */
	public static LocalTime parseLocalDate(String value) {
		return LOCAL_DATE_FORMATTER.parseLocalTime(value);
	}

	/**
	 * Format a standard local date value, in {@code yyyy-MM-dd} form.
	 * 
	 * @param value
	 *        the LocalDate to format
	 * @return the formatted value
	 */
	public static String format(LocalDate value) {
		return LOCAL_DATE_FORMATTER.print(value);
	}

}

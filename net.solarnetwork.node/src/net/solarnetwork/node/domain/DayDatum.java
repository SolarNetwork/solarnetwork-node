/* ==================================================================
 * DayDatum.java - Oct 22, 2014 2:41:56 PM
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

package net.solarnetwork.node.domain;

import org.joda.time.LocalTime;

/**
 * Solar day related datum.
 * 
 * @author matt
 * @version 1.0
 */
public interface DayDatum {

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link DayDatum#getSunrise()} values.
	 */
	static final String SUNRISE_KEY = "sunrise";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link DayDatum#getSunset()} values.
	 */
	static final String SUNSET_KEY = "sunset";

	/**
	 * Get the sunrise time.
	 * 
	 * @return the sunrise
	 */
	LocalTime getSunrise();

	/**
	 * Get the sunset time.
	 * 
	 * @return the sunset
	 */
	LocalTime getSunset();

}

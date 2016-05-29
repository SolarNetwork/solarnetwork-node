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

import java.math.BigDecimal;
import org.joda.time.LocalTime;

/**
 * Solar day related datum.
 * 
 * @author matt
 * @version 1.1
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
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link DayDatum#getSunrise()} values.
	 * 
	 * @since 1.1
	 */
	static final String MOONRISE_KEY = "moonrise";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link DayDatum#getSunset()} values.
	 * 
	 * @since 1.1
	 */
	static final String MOONSET_KEY = "moonset";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link DayDatum#getTemperatureMaximum()} values.
	 * 
	 * @since 1.1
	 */
	static final String TEMPERATURE_MAXIMUM_KEY = "tempMax";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link DayDatum#getTemperatureMinimum()} values.
	 * 
	 * @since 1.1
	 */
	static final String TEMPERATURE_MINIMUM_KEY = "tempMin";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} status sample key
	 * for {@link DayDatum#getSkyConditions()} values.
	 */
	static final String SKY_CONDITIONS_KEY = "sky";

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

	/**
	 * Get the sunrise time.
	 * 
	 * @return the moonrise
	 * @since 1.1
	 */
	LocalTime getMoonrise();

	/**
	 * Get the moonset time.
	 * 
	 * @return the moonset
	 * @since 1.1
	 */
	LocalTime getMoonset();

	/**
	 * Get the minimum temperature for the day.
	 * 
	 * @return The minimum temperature.
	 * @since 1.1
	 */
	BigDecimal getTemperatureMinimum();

	/**
	 * Get the maximum temperature for the day.
	 * 
	 * @return The maximum temperature.
	 * @since 1.1
	 */
	BigDecimal getTemperatureMaximum();

	/**
	 * Get a textual description of the sky conditions, e.g. "clear", "cloudy",
	 * etc.
	 * 
	 * @return general sky conditions
	 * @since 1.1
	 */
	String getSkyConditions();
}

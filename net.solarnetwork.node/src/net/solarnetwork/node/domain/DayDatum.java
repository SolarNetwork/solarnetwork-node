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

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import java.math.BigDecimal;
import java.time.LocalTime;
import net.solarnetwork.util.DateUtils;

/**
 * Solar day related datum.
 * 
 * @author matt
 * @version 2.0
 */
public interface DayDatum
		extends AtmosphericDatum, net.solarnetwork.domain.datum.DayDatum, MutableNodeDatum {

	/**
	 * Set the sunrise time.
	 * 
	 * @param value
	 *        the sunrise
	 */
	default void setSunriseTime(LocalTime value) {
		asMutableSampleOperations().putSampleValue(Status, SUNRISE_KEY, DateUtils.format(value));
	}

	/**
	 * Set the sunset time.
	 * 
	 * @param value
	 *        the sunset
	 */
	default void setSunsetTime(LocalTime value) {
		asMutableSampleOperations().putSampleValue(Status, SUNSET_KEY, DateUtils.format(value));
	}

	/**
	 * Set the moon rise time.
	 * 
	 * @param value
	 *        the moon rise
	 */
	default void setMoonriseTime(LocalTime value) {
		asMutableSampleOperations().putSampleValue(Status, MOONRISE_KEY, DateUtils.format(value));
	}

	/**
	 * Set the moon set time.
	 * 
	 * @param value
	 *        the moon set
	 */
	default void setMoonsetTime(LocalTime value) {
		asMutableSampleOperations().putSampleValue(Status, MOONSET_KEY, DateUtils.format(value));
	}

	/**
	 * Set the minimum temperature for the day.
	 * 
	 * @param value
	 *        the minimum temperature
	 */
	default void setTemperatureMinimum(BigDecimal value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, TEMPERATURE_MINIMUM_KEY, value);
	}

	/**
	 * Set the maximum temperature for the day.
	 * 
	 * @param value
	 *        the maximum temperature
	 */
	default void setTemperatureMaximum(BigDecimal value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, TEMPERATURE_MAXIMUM_KEY, value);
	}

	/**
	 * Set a brief textual description of the overall conditions, e.g. "Sunshine
	 * and some clouds. High 18C. Winds N at 10 to 15 km/h."
	 * 
	 * @param value
	 *        general overall conditions description
	 */
	default void setBriefOverview(String value) {
		asMutableSampleOperations().putSampleValue(Status, BRIEF_OVERVIEW_KEY, value);
	}

}

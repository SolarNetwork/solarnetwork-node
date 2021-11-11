/* ==================================================================
 * OwmClient.java - 14/09/2018 4:52:14 PM
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

package net.solarnetwork.node.weather.owm;

import java.util.Collection;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.DayDatum;

/**
 * API for accessing OWM information.
 * 
 * @author matt
 * @version 2.0
 */
public interface OwmClient {

	/**
	 * Get the API key used by the client.
	 * 
	 * @return the API key
	 */
	String getApiKey();

	/**
	 * Lookup the current day information for a specific OWM location
	 * identifier.
	 * 
	 * @param identifier
	 *        The location identifier value to lookup day information for.
	 * @param timeZoneId
	 *        The time zone ID to use for local date/time values. The OWM API
	 *        does not return the local time zone the weather data is in, so
	 *        this value is used to interpret things like sunrise/sunset into
	 *        local times.
	 * @return The day information, or {@literal null} if not available
	 */
	DayDatum getCurrentDay(String identifier, String timeZoneId);

	/**
	 * Lookup the current conditions for a specific OWM location identifier.
	 * 
	 * @param identifier
	 *        The location identifier value to lookup conditions for.
	 * @return The conditions, or {@literal null} if not available
	 */
	AtmosphericDatum getCurrentConditions(String identifier);

	/**
	 * Lookup the current hourly forecast information for a specific OWM
	 * location identifier.
	 * 
	 * @param identifier
	 *        The location identifier value to lookup day information for.
	 * @return The weather information, never {@literal null}
	 */
	public Collection<AtmosphericDatum> getHourlyForecast(String identifier);

}

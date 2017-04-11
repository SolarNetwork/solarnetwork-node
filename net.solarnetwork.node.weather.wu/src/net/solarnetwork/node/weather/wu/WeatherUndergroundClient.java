/* ==================================================================
 * WeatherUndergroundClient.java - 7/04/2017 4:31:35 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.wu;

import java.util.Collection;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.domain.DayDatum;

/**
 * API for accessing Weather Underground information.
 * 
 * @author matt
 * @version 1.1
 */
public interface WeatherUndergroundClient {

	/**
	 * Get the API key used by the client.
	 * 
	 * @return the API key
	 * @since 1.1
	 */
	String getApiKey();

	/**
	 * Query for locations based on the IP address of the requester.
	 * 
	 * @return A collection of matching results, never {@code null}.
	 */
	Collection<WeatherUndergroundLocation> findLocationsForIpAddress();

	/**
	 * Query for locations based on name.
	 * 
	 * @param name
	 *        The name to look for, which can be a substring.
	 * @param country
	 *        An optional 2-character country code to limit the search to. Pass
	 *        {@code null} for any country.
	 * @return A collection of matching results, never {@code null}.
	 */
	Collection<WeatherUndergroundLocation> findLocations(String name, String country);

	/**
	 * Lookup the current conditions for a specific Weather Underground location
	 * identifier.
	 * 
	 * @param identifier
	 *        The location identifier value to lookup conditions for.
	 * @return The conditions, or {@code null} if not available
	 */
	AtmosphericDatum getCurrentConditions(String identifier);

	/**
	 * Query for 24-hour forecast conditions for a specific Weather Underground
	 * location identifier.
	 * 
	 * @param identifier
	 *        The location identifier value to lookup the hourly forecast for.
	 * @return The forecast data, never {@code null}
	 */
	Collection<AtmosphericDatum> getHourlyForecast(String identifier);

	/**
	 * Lookup the current day information for a specific Weather Underground
	 * location identifier.
	 * 
	 * @param identifier
	 *        The location identifier value to lookup day information for.
	 * @return The day information, or {@code null} if not available
	 */
	DayDatum getCurrentDay(String identifier);

	/**
	 * Lookup 3-day forecast conditions (starting tomorrow) for a specific
	 * Weather Underground location identifier.
	 * 
	 * @param identifier
	 *        The location identifier value to lookup the hourly forecast for.
	 * @return The forecast data, never {@code null}
	 */
	Collection<DayDatum> getThreeDayForecast(String identifier);

	/**
	 * Lookup 10-day forecast conditions (starting tomorrow) for a specific
	 * Weather Underground location identifier.
	 * 
	 * @param identifier
	 *        The location identifier value to lookup the hourly forecast for.
	 * @return The forecast data, never {@code null}
	 */
	Collection<DayDatum> getTenDayForecast(String identifier);

}

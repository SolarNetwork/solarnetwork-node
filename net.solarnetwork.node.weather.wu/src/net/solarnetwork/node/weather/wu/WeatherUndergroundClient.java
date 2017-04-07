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

/**
 * API for accessing Weather Underground information.
 * 
 * @author matt
 * @version 1.0
 */
public interface WeatherUndergroundClient {

	/**
	 * Query for locations based on the IP address of the requester.
	 * 
	 * @return A collection of matching results, never {@code null}.
	 */
	Collection<WeatherUndergroundLocation> findLocationsForIpAddress();

}

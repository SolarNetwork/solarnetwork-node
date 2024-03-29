/* ==================================================================
 * MetserviceClient.java - 28/05/2016 1:30:01 pm
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.nz.metservice;

import java.util.Collection;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.DayDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * API for accessing Metservice information.
 * 
 * @author matt
 * @version 2.0
 */
public interface MetserviceClient {

	/**
	 * Read the current "rise set" data for a location.
	 * 
	 * @param locationIdentifier
	 *        The location identifier.
	 * @return The day data.
	 */
	DayDatum readCurrentRiseSet(String locationIdentifier);

	/**
	 * Read the current "local observations" data for a location. This includes
	 * both day-level observations and current weather observations.
	 * 
	 * @param locationIdentifier
	 *        The location identifier.
	 * @return A collection of {@link DayDatum} and {@link AtmosphericDatum}.
	 */
	Collection<NodeDatum> readCurrentLocalObservations(String locationIdentifier);

	/**
	 * Read the "local forecast" data for a location.
	 * 
	 * @param locationIdentifier
	 *        The location identifier.
	 * @return A collection of {@link DayDatum}, which can be for dates in the
	 *         future.
	 */
	Collection<DayDatum> readLocalForecast(String locationIdentifier);

	/**
	 * Read the "hourly observations and forecast" data for a location,
	 * returning the forecast data as datum.
	 * 
	 * @param locationIdentifier
	 *        The location identifier.
	 * @return A collection of {@link AtmosphericDatum}, which can be for dates
	 *         in the future.
	 */
	Collection<AtmosphericDatum> readHourlyForecast(String locationIdentifier);

}

/* ==================================================================
 * YrClient.java - 19/05/2017 2:34:47 PM
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

package net.solarnetwork.node.weather.yr;

import java.util.Collection;
import net.solarnetwork.node.domain.AtmosphericDatum;

/**
 * API for accessing Yr information.
 * 
 * @author matt
 * @version 1.0
 */
public interface YrClient {

	/**
	 * Query for 24-hour forecast conditions for a specific Yr location
	 * identifier.
	 * 
	 * <p>
	 * Location identifiers should be specified as the full URL path of the
	 * location, e.g. {@literal /Norway/Telemark/Sauherad/Gvarv} or
	 * {@literal /New_Zealand/Otago/Glenorchy~6204577}.
	 * 
	 * @param identifier
	 *        The location identifier value to lookup the hourly forecast for.
	 * @return The forecast data, never {@code null}
	 */
	Collection<AtmosphericDatum> getHourlyForecast(String identifier);

}

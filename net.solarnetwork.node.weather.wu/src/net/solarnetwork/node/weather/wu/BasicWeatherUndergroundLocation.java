/* ==================================================================
 * BasicWeatherUndergroundLocation.java - 7/04/2017 5:49:53 PM
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

import net.solarnetwork.domain.Location;
import net.solarnetwork.domain.SimpleLocation;

/**
 * Basic implementation of {@link WeatherUndergroundLocation}.
 * 
 * @author matt
 * @version 1.1
 */
public class BasicWeatherUndergroundLocation extends SimpleLocation
		implements WeatherUndergroundLocation {

	private static final long serialVersionUID = -7816265519353984983L;

	private String identifier;

	public BasicWeatherUndergroundLocation() {
		super();
	}

	public BasicWeatherUndergroundLocation(Location loc) {
		super(loc);
		if ( loc instanceof WeatherUndergroundLocation ) {
			WeatherUndergroundLocation wuLoc = (WeatherUndergroundLocation) loc;
			this.identifier = wuLoc.getIdentifier();
		}
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Set the Weather Underground API identifier.
	 * 
	 * @param identifier
	 *        the identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

}

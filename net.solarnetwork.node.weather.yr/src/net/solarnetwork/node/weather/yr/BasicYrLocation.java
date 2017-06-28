/* ==================================================================
 * YrLocation.java - 19/05/2017 3:41:21 PM
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

import net.solarnetwork.domain.BasicLocation;
import net.solarnetwork.domain.Location;

/**
 * Basic implementation of {@link YrLocation}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicYrLocation extends BasicLocation implements YrLocation {

	private String identifier;

	public BasicYrLocation() {
		super();
	}

	public BasicYrLocation(Location loc) {
		super(loc);
		if ( loc instanceof YrLocation ) {
			YrLocation yrLoc = (YrLocation) loc;
			this.identifier = yrLoc.getIdentifier();
		}
	}

	/**
	 * Get the Yr location unique identifier.
	 * 
	 * @return the identifier
	 */
	@Override
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Set the Yr identifier for the location.
	 * 
	 * @param identifier
	 *        the identifier to set
	 */
	public void setIdentifier(String identifier) {
		if ( identifier != null && identifier.endsWith("/") ) {
			// strip out trailing slash, if extracted from XML credit/@url
			this.identifier = identifier.substring(0, identifier.length() - 1);
		} else {
			this.identifier = identifier;
		}
	}

}

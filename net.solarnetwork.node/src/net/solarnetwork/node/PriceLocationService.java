/* ==================================================================
 * PriceLocationService.java - Feb 19, 2011 2:29:08 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node;

/**
 * API for querying for a price location.
 * 
 * @author matt
 * @version $Revision$
 */
public interface PriceLocationService {
	
	/** An unknown source, which is always available. */
	static final String UNKNOWN_SOURCE = "Unknown";
	
	/** An unknown location, which is always available for the UNKNOWN source. */
	static final String UNKNOWN_LOCATION = "Unknown";

	/**
	 * Look up a PriceLocation based on a source name and location name.
	 * 
	 * @param sourceName the source of the price data (e.g. electricityinfo.co.nz)
	 * @param locationName the price location within the source (e.g. HAY2201)
	 * @return the matching location, or <em>null</em> if not found
	 */
	PriceLocation findLocation(String sourceName, String locationName);
	
}

package net.solarnetwork.node.weather.ibm.wc;

import net.solarnetwork.domain.Location;

/**
 * API for identifying locations when accessing the Weather Company data
 * @author matt frost
 *
 */
public interface WCLocation extends Location{
	
	/**
	 * Get a unique identifier for this location.
	 * 
	 * This identifier is used to query for exact locations in the Weather Company API.
	 * 
	 * @return the identifier
	 */
	String getIdentifier();
	
	

}

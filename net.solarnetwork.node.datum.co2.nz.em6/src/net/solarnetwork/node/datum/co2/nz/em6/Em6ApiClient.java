/* ==================================================================
 * Em6ApiClient.java - 10/03/2023 12:01:25 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.co2.nz.em6;

import java.util.Collection;
import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * Client API for EM6 data.
 * 
 * @author matt
 * @version 1.0
 */
public interface Em6ApiClient {

	/**
	 * Get the current carbon intensity.
	 * 
	 * @return the carbon intensity as a collection of NodeDatum
	 */
	Collection<NodeDatum> currentCarbonIntensity();

}

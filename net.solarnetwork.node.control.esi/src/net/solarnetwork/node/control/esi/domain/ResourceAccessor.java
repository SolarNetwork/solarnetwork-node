/* ==================================================================
 * Resource.java - 10/08/2019 11:44:51 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.esi.domain;

import java.util.Locale;
import net.solarnetwork.service.Identifiable;

/**
 * API for accessing Resource information.
 * 
 * @author matt
 * @version 2.0
 */
public interface ResourceAccessor extends Identifiable {

	/**
	 * Get a status message.
	 * 
	 * @param locale
	 *        the desired locale of the message
	 * @return the message
	 */
	String getStatusMessage(Locale locale);

	/**
	 * Get the maximum load this resource can demand, in W.
	 * 
	 * @return the power maximum
	 */
	Long getLoadPowerMax();

	/**
	 * Get the expected power factor of load, between -1..1.
	 * 
	 * @return the power factor
	 */
	Float getLoadPowerFactor();

	/**
	 * Get the maximum supply resource can offer, in W.
	 * 
	 * @return the power maximum
	 */
	Long getSupplyPowerMax();

	/**
	 * Get the expected power factor of supply, between -1..1.
	 * 
	 * @return the power factor
	 */
	Float getSupplyPowerFactor();

	/**
	 * 
	 * /** Get the theoretical storage capacity of this resource, in Wh.
	 * 
	 * @return the capacity
	 */
	Long getStorageEnergyCapacity();

	/**
	 * Get the expected minimum/maximum response time to start/finish executing
	 * load or supply changes.
	 * 
	 * @return the response time
	 */
	DurationRange getResponseTime();

}

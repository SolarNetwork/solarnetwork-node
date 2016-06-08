/* ==================================================================
 * EnergyStorageDatum.java - 16/02/2016 7:37:54 pm
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

package net.solarnetwork.node.domain;

/**
 * Standardized API for energy storage system related datum to implement.
 * 
 * @author matt
 * @version 1.0
 */
public interface EnergyStorageDatum extends Datum {

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getAvailableEnergyPercentage()} values.
	 */
	String PERCENTAGE_KEY = "percent";

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getAvailableEnergy()} values.
	 */
	String AVAILABLE_WATT_HOURS_KEY = "availWattHours";

	/**
	 * Get the percentage of energy capacity available in the storage. This
	 * value, multiplied by {@link #getAvailableEnergy()}, represents the total
	 * energy capacity of the storage.
	 * 
	 * @return The available energy as a percentage of the total capacity of the
	 *         storage.
	 */
	Float getAvailableEnergyPercentage();

	/**
	 * Get the available energy of the storage system, in Wh.
	 * 
	 * @return The available energy of the storage.
	 */
	Long getAvailableEnergy();

}

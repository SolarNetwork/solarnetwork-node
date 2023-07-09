/* ==================================================================
 * MiniMeteorologicalModelAccessor.java - 10/07/2023 7:07:52 am
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

package net.solarnetwork.node.hw.sunspec.environmental;

import net.solarnetwork.node.hw.sunspec.ModelAccessor;

/**
 * API for accessing mini weather model data.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public interface MiniMeteorologicalModelAccessor extends ModelAccessor {

	/**
	 * Get the global horizontal irradiance, in W/m2.
	 * 
	 * @return the irradiance
	 */
	Integer getGlobalHorizontalIrradiance();

	/**
	 * Get the back-of-module temperature.
	 * 
	 * @return the temperature, in degrees Celsius
	 */
	Float getBackOfModuleTemperature();

	/**
	 * Get the ambient temperature.
	 * 
	 * @return the temperature, in degrees Celsius
	 */
	Float getAmbientTemperature();

	/**
	 * Get the wind speed.
	 * 
	 * @return the wind speed, in m/s
	 */
	Integer getWindSpeed();

}

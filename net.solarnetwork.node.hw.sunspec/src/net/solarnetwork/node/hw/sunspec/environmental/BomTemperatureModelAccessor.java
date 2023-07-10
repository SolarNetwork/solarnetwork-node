/* ==================================================================
 * BomTemperatureModelAccessor.java - 5/07/2023 10:26:12 am
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

import java.util.List;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;

/**
 * API for accessing back-of-module temperature data.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public interface BomTemperatureModelAccessor extends ModelAccessor {

	/**
	 * Get the list of available back-of-module temperatures.
	 * 
	 * @return the temperatures, each in degrees Celsius
	 */
	List<Float> getBackOfModuleTemperatures();

	/**
	 * Get the first available back-of-module temperature.
	 * 
	 * @return the first available temperature, or {@literal null}
	 */
	default Float getBackOfModuleTemperature() {
		List<Float> temps = getBackOfModuleTemperatures();
		return (temps != null && !temps.isEmpty() ? temps.get(0) : null);
	}

}

/* ==================================================================
 * SmaSunnySensorboxDataAccessor.java - 15/09/2020 6:54:19 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import net.solarnetwork.node.domain.DataAccessor;

/**
 * {@link DataAccessor} for SMA Sunny Sensorbox devices.
 * 
 * @author matt
 * @version 1.0
 */
public interface SmaSunnySensorboxDataAccessor extends SmaDeviceDataAccessor {

	@Override
	default boolean hasCommonDataAccessorSupport() {
		return false;
	}

	/**
	 * Get the device class.
	 * 
	 * @return the device class
	 */
	Long getDeviceClass();

	/**
	 * Get the operating duration.
	 * 
	 * @return the operating time, in seconds
	 */
	BigInteger getOperatingTime();

	/**
	 * Get the ambient temperature.
	 * 
	 * @return the temperature, in degrees Celsius
	 */
	BigDecimal getTemperature();

	/**
	 * Get the solar irradiance level, in watts / square meter.
	 * 
	 * @return irradiance level
	 */
	BigDecimal getIrradiance();

	/**
	 * Get the wind speed, in meters / second.
	 * 
	 * @return wind speed
	 */
	BigDecimal getWindSpeed();

	/**
	 * Get the PV module temperature.
	 * 
	 * @return the module temperature, in degrees Celsius
	 */
	BigDecimal getModuleTemperature();

	/**
	 * Get the solar irradiance level on the external sensor, in watts / square
	 * meter.
	 * 
	 * @return irradiance level
	 */
	BigDecimal getExternalIrradiance();

}

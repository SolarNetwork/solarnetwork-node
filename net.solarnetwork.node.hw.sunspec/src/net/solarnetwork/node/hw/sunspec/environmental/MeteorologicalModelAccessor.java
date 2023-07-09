/* ==================================================================
 * MeteorologicalModelAccessor.java - 10/07/2023 8:35:40 am
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
 * API for accessing meteorological model data.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public interface MeteorologicalModelAccessor extends ModelAccessor {

	/**
	 * Get the ambient temperature.
	 * 
	 * @return the temperature, in degrees Celsius
	 */
	Float getAmbientTemperature();

	/**
	 * Get the relative humidity.
	 * 
	 * @return the humidity, as an integer percentage
	 */
	Integer getRelativeHumidity();

	/**
	 * Get the atmospheric pressure, in pascals.
	 * 
	 * @return the atmospheric pressure, in pascals
	 */
	Integer getAtmosphericPressure();

	/**
	 * Get the wind speed.
	 * 
	 * @return the wind speed, in m/s
	 */
	Integer getWindSpeed();

	/**
	 * Get the wind direction.
	 * 
	 * @return the wind direction, in degrees
	 */
	Integer getWindDirection();

	/**
	 * Get the rain accumulation, since the last reading.
	 * 
	 * @return the rain accumulation, in mm
	 */
	Integer getRainAccumulation();

	/**
	 * Get the snow accumulation, since the last reading.
	 * 
	 * @return the snow accumulation, in mm
	 */
	Integer getSnowAccumulation();

	/**
	 * Get the precipitation type.
	 * 
	 * @return the precipitation type
	 */
	PrecipitationType getPrecipitationType();

	/**
	 * Get the electric field.
	 * 
	 * @return the electric field, in V/m
	 */
	Integer getElectricField();

	/**
	 * Get the surface wetness.
	 * 
	 * @return the surface wetness, in Ohm
	 */
	Integer getSurfaceWetness();

	/**
	 * Get the soil moisture.
	 * 
	 * @return the soil moisture, as an integer percentage
	 */
	Integer getSoilMoisture();

}

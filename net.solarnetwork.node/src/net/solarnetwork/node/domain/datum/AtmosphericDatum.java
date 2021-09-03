/* ==================================================================
 * AtmosphericDatum.java - Aug 26, 2014 1:52:01 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain.datum;

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import java.math.BigDecimal;

/**
 * Standardized API for atmospheric related datum to implement.
 * 
 * @author matt
 * @version 2.0
 */
public interface AtmosphericDatum
		extends net.solarnetwork.domain.datum.AtmosphericDatum, MutableNodeDatum {

	/**
	 * Set the instantaneous temperature, in degrees Celsius.
	 * 
	 * @param value
	 *        the temperature, in degrees Celsius
	 */
	default void setTemperature(BigDecimal value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, TEMPERATURE_KEY, value);
	}

	/**
	 * Set the instantaneous dew point, in degrees Celsius.
	 * 
	 * @param value
	 *        the dew point, in degrees Celsius
	 */
	default void setDewPoint(BigDecimal value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, DEW_POINT_KEY, value);
	}

	/**
	 * Set the instantaneous humidity, as an integer percentage (where 100
	 * represents 100%).
	 * 
	 * @param value
	 *        the humidity, as an integer percentage
	 */
	default void setHumidity(Integer value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, HUMIDITY_KEY, value);
	}

	/**
	 * Set the instantaneous atmospheric pressure, in pascals.
	 * 
	 * @param value
	 *        the atmospheric pressure, in pascals
	 */
	default void setAtmosphericPressure(Integer value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, ATMOSPHERIC_PRESSURE_KEY, value);
	}

	/**
	 * Set the instantaneous visibility, in meters.
	 * 
	 * @param value
	 *        visibility, in meters
	 */
	default void setVisibility(Integer value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, VISIBILITY_KEY, value);
	}

	/**
	 * Set a textual description of the sky conditions, e.g. "clear", "cloudy",
	 * etc.
	 * 
	 * @param value
	 *        general sky conditions
	 */
	default void setSkyConditions(String value) {
		asMutableSampleOperations().putSampleValue(Status, SKY_CONDITIONS_KEY, value);
	}

	/**
	 * Set the wind speed, in meters / second.
	 * 
	 * @param value
	 *        the wind speed
	 */
	default void setWindSpeed(BigDecimal value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, WIND_SPEED_KEY, value);
	}

	/**
	 * Set the wind direction, in degrees.
	 * 
	 * @param value
	 *        the wind direction
	 */
	default void setWindDirection(Integer value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, WIND_DIRECTION_KEY, value);
	}

	/**
	 * Set the rain accumulation, in millimeters.
	 * 
	 * @param value
	 *        rain accumulation
	 */
	default void setRain(Integer value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, RAIN_KEY, value);
	}

	/**
	 * Set the snow accumulation, in millimeters.
	 * 
	 * @param value
	 *        snow accumulation
	 */
	default void setSnow(Integer value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, SNOW_KEY, value);
	}

	/**
	 * Set the solar irradiance level, in watts / square meter.
	 * 
	 * @param value
	 *        irradiance level
	 */
	default void setIrradiance(BigDecimal value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, IRRADIANCE_KEY, value);
	}

}

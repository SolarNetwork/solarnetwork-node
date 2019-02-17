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

package net.solarnetwork.node.domain;

import java.math.BigDecimal;

/**
 * Standardized API for atmospheric related datum to implement.
 * 
 * @author matt
 * @version 1.4
 */
public interface AtmosphericDatum extends Datum {

	/**
	 * A {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link AtmosphericDatum#getTemperature()} values.
	 */
	String TEMPERATURE_KEY = "temp";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link AtmosphericDatum#getHumidity()} values.
	 */
	String HUMIDITY_KEY = "humidity";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link AtmosphericDatum#getDewPoint()} values.
	 */
	String DEW_POINT_KEY = "dew";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link AtmosphericDatum#getAtmosphericPressure()} values.
	 */
	String ATMOSPHERIC_PRESSURE_KEY = "atm";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link AtmosphericDatum#getAtmosphericPressure()} values.
	 */
	String VISIBILITY_KEY = "visibility";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} status sample key
	 * for {@link AtmosphericDatum#getSkyConditions()} values.
	 */
	String SKY_CONDITIONS_KEY = "sky";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} status sample key
	 * for a bitmask of {@link net.solarnetwork.domain.SkyCondition#getCode()}
	 * values.
	 * 
	 * @since 1.4
	 */
	String SKY_CONDITION_CODES_KEY = "skies";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} status sample key
	 * for {@link AtmosphericDatum#getWindSpeed()} values.
	 * 
	 * @since 1.2
	 */
	String WIND_SPEED_KEY = "wspeed";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} status sample key
	 * for {@link AtmosphericDatum#getWindDirection()} values.
	 * 
	 * @since 1.2
	 */
	String WIND_DIRECTION_KEY = "wdir";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} status sample key
	 * for {@link AtmosphericDatum#getRain()} values.
	 * 
	 * @since 1.2
	 */
	String RAIN_KEY = "rain";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} status sample key
	 * for {@link AtmosphericDatum#getSnow()} values.
	 * 
	 * @since 1.2
	 */
	String SNOW_KEY = "snow";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} status sample key
	 * for {@link AtmosphericDatum#getIrradiance()} values.
	 * 
	 * @since 1.3
	 */
	String IRRADIANCE_KEY = "irradiance";

	/** A tag for an "indoor" atmosphere sample. */
	String TAG_ATMOSPHERE_INDOOR = "indoor";

	/** A tag for an "outdoor" atmosphere sample. */
	String TAG_ATMOSPHERE_OUTDOOR = "outdoor";

	/**
	 * A tag for a forecast atmosphere sample, as opposed to an actual
	 * measurement.
	 * 
	 * @since 1.2
	 */
	String TAG_FORECAST = "forecast";

	/**
	 * Get the instantaneous temperature, in degrees Celsius.
	 * 
	 * @return the temperature, in degrees Celsius
	 */
	BigDecimal getTemperature();

	/**
	 * Get the instantaneous dew point, in degrees Celsius.
	 * 
	 * @return the dew point, in degrees celsius
	 */
	BigDecimal getDewPoint();

	/**
	 * Get the instantaneous humidity, as an integer percentage (where 100
	 * represents 100%).
	 * 
	 * @return the humidity, as an integer percentage
	 */
	Integer getHumidity();

	/**
	 * Get the instantaneous atmospheric pressure, in pascals.
	 * 
	 * @return the atmospheric pressure, in pascals
	 */
	Integer getAtmosphericPressure();

	/**
	 * Get the instantaneous visibility, in meters.
	 * 
	 * @return visibility, in meters
	 */
	Integer getVisibility();

	/**
	 * Get a textual description of the sky conditions, e.g. "clear", "cloudy",
	 * etc.
	 * 
	 * @return general sky conditions
	 */
	String getSkyConditions();

	/**
	 * Get the wind speed, in meters / second.
	 * 
	 * @return wind speed
	 * @since 1.2
	 */
	BigDecimal getWindSpeed();

	/**
	 * Get the wind direction, in degrees.
	 * 
	 * @return wind direction
	 * @since 1.2
	 */
	Integer getWindDirection();

	/**
	 * Get the rain accumulation, in millimeters.
	 * 
	 * @return rain accumulation
	 * @since 1.2
	 */
	Integer getRain();

	/**
	 * Get the snow accumulation, in millimeters.
	 * 
	 * @return snow accumulation
	 * @since 1.2
	 */
	Integer getSnow();

	/**
	 * Get the solar irradiance level, in watts / square meter.
	 * 
	 * @return irradiance level
	 * @since 1.3
	 */
	BigDecimal getIrradiance();

}

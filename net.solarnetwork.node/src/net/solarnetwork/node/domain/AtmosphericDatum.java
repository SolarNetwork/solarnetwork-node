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
 * @version 1.2
 */
public interface AtmosphericDatum extends Datum {

	/**
	 * A {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link AtmosphericDatum#getTemperature()} values.
	 */
	static final String TEMPERATURE_KEY = "temp";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link AtmosphericDatum#getHumidity()} values.
	 */
	static final String HUMIDITY_KEY = "humidity";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link AtmosphericDatum#getDewPoint()} values.
	 */
	static final String DEW_POINT_KEY = "dew";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link AtmosphericDatum#getAtmosphericPressure()} values.
	 */
	static final String ATMOSPHERIC_PRESSURE_KEY = "atm";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} instantaneous
	 * sample key for {@link AtmosphericDatum#getAtmosphericPressure()} values.
	 */
	static final String VISIBILITY_KEY = "visibility";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} status sample key
	 * for {@link AtmosphericDatum#getSkyConditions()} values.
	 */
	static final String SKY_CONDITIONS_KEY = "sky";

	/** A tag for an "indoor" atmosphere sample. */
	static final String TAG_ATMOSPHERE_INDOOR = "indoor";

	/** A tag for an "outdoor" atmosphere sample. */
	static final String TAG_ATMOSPHERE_OUTDOOR = "outdoor";

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

}

/* ==================================================================
 * AtmosphereDatum.java - Aug 26, 2014 1:52:01 PM
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

/**
 * Standardized API for atmospheric related datum to implement.
 * 
 * @author matt
 * @version 1.0
 */
public interface AtmosphereDatum {

	/**
	 * A {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getTemperature()} values.
	 */
	public static final String TEMPERATURE_KEY = "temp";

	/**
	 * A {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getHumidity()} values.
	 */
	public static final String HUMIDITY_KEY = "hum";

	/** A tag for an "indoor" atmosphere sample. */
	public static final String TAG_ATMOSPHERE_INDOOR = "indoor";

	/** A tag for an "outdoor" atmosphere sample. */
	public static final String TAG_ATMOSPHERE_OUTDOOR = "outdoor";

	/**
	 * Get the instantaneous temperature, in degrees Celsius.
	 * 
	 * @return the temperature, in Celsius
	 */
	public Float getTemperature();

	/**
	 * Get the instantaneous humidity, as an integer percentage (where 100
	 * represents 100%).
	 * 
	 * @return the humidity, as an integer percentage
	 */
	public Integer getHumidity();

}

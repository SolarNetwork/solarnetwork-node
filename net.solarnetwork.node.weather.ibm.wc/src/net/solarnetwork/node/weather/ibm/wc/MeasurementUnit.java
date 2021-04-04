/* ==================================================================
 * MeasurementUnit.java - 23/11/2018 4:19:01 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.ibm.wc;

/**
 * Measurement units enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public enum MeasurementUnit {

	/**
	 * The English/Imperial measurement system, used by default for the
	 * {@literal en-US} locale.
	 */
	English("e", "English (US)"),

	/**
	 * The Hybrid-UK measurement system, used by default for the
	 * {@literal en-UK} locale.
	 */
	Hybrid("h", "English (UK)"),

	/**
	 * The Metric measurement system, used by default locales other than
	 * {@literal en-US}, {@literal en-UK}.
	 */
	Metric("m", "Metric");

	private final String key;
	private final String description;

	private MeasurementUnit(String key, String description) {
		this.key = key;
		this.description = description;
	}

	/**
	 * Get the unit key value.
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}
	
	/**
	 * Get the unit description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get an enumeration for a key value.
	 * 
	 * @param key
	 *        the key value to get the enumeration for
	 * @return the enumeration
	 * @throws IllegalArgumentException
	 *         if {@code key} is not supported
	 */
	public static MeasurementUnit forKey(String key) {
		for ( MeasurementUnit u : MeasurementUnit.values() ) {
			if ( u.key.equalsIgnoreCase(key) ) {
				return u;
			}
		}
		throw new IllegalArgumentException("MeasurementUnit key [" + key + "] is not supported");
	}
}

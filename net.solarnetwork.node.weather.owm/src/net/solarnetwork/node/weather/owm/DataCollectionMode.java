/* ==================================================================
 * DataCollectionMode.java - 25/09/2018 1:39:58 PM
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

package net.solarnetwork.node.weather.owm;

/**
 * Data collection mode enumeration.
 * 
 * @author matt
 * @version 1.0
 */
public enum DataCollectionMode {

	/** Collect both observation and forecast data. */
	Mixed(0),

	/** Collect only observation data. */
	Observation(1),

	/** Collect only forecast data. */
	Forecast(2);

	private final int key;

	private DataCollectionMode(int key) {
		this.key = key;
	}

	/**
	 * Get the key value.
	 * 
	 * @return the key
	 */
	public int getKey() {
		return key;
	}

	/**
	 * Get a friendly display version of the enumeration.
	 * 
	 * @return
	 */
	public String toDisplayString() {
		switch (this) {
			case Observation:
				return "Observation data only";

			case Forecast:
				return "Forecast data only";

			default:
				return "Mixed - both observation and forecast data";
		}
	}

	/**
	 * Get an enumeration for a key.
	 * 
	 * @param key
	 *        the key of the enumeration to get
	 * @return the enumeration
	 * @throws IllegalArgumentException
	 *         if {@code key} is not a valid value
	 */
	public static DataCollectionMode forKey(int key) {
		for ( DataCollectionMode mode : DataCollectionMode.values() ) {
			if ( key == mode.key ) {
				return mode;
			}
		}
		throw new IllegalArgumentException("[" + key + "] is not a valid DataCollectionMode key value");
	}

}

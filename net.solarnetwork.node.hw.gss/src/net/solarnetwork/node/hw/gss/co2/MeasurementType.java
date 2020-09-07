/* ==================================================================
 * MeasurementType.java - 28/08/2020 11:45:05 AM
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

package net.solarnetwork.node.hw.gss.co2;

import net.solarnetwork.domain.Bitmaskable;

/**
 * A bitmask of measurement types.
 * 
 * @author matt
 * @version 1.0
 */
public enum MeasurementType implements Bitmaskable {

	Humidity(12, "Humidity", "H"),

	Temperature(6, "Temperature", "T"),

	Co2Filtered(2, "CO2 (Filtered)", "Z"),

	Co2Unfiltered(1, "CO2 (Unfiltered)", "z");

	private final int bit;
	private final String description;
	private final String key;

	private MeasurementType(int bit, String description, String key) {
		this.bit = bit;
		this.description = description;
		this.key = key;
	}

	@Override
	public int bitmaskBitOffset() {
		return bit;
	}

	/**
	 * Get a description of the status.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get the key value.
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get a measurement type for a given key.
	 * 
	 * @param key
	 *        the key to get the type for
	 * @return the type, or {@literal null} if {@code key} is not a valid type
	 */
	public static MeasurementType forKey(String key) {
		for ( MeasurementType type : MeasurementType.values() ) {
			if ( type.key.equals(key) ) {
				return type;
			}
		}
		return null;
	}

}

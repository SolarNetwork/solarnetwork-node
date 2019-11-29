/* ==================================================================
 * Parity.java - 14/11/2019 3:02:45 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.gpsd.domain;

/**
 * Parity enumeration.
 * 
 * @author matt
 * @version 1.0
 */
public enum Parity {

	/** No parity. */
	None("N"),

	/** Odd parity. */
	Odd("O"),

	/** Even parity. */
	Even("E");

	private final String key;

	private Parity(String key) {
		this.key = key;
	}

	/**
	 * Get the key value.
	 * 
	 * @return the key the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get an enumeration instance from a key value.
	 * 
	 * @param key
	 *        the key value to get an enum for
	 * @return the mode, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if {@code key} is not a supported value
	 */
	public static Parity forKey(final String key) {
		for ( Parity e : Parity.values() ) {
			if ( e.getKey().equalsIgnoreCase(key) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Parity key [" + key + "] not supported.");
	}

}

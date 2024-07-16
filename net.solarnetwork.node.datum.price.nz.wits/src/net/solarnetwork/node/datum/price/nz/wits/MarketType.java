/* ==================================================================
 * MarketType.java - 16/07/2024 10:41:51 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.price.nz.wits;

/**
 * Market type enumeration.
 *
 * @author matt
 * @version 1.0
 */
public enum MarketType {

	/** The energy market. */
	Energy("E"),

	/** The reserve market. */
	Reserve("R"),

	;

	private final String key;

	private MarketType(String key) {
		this.key = key;
	}

	/**
	 * Get the type key.
	 *
	 * @return the key
	 */
	public final String getKey() {
		return key;
	}

	/**
	 * Get an enum instance for a key value.
	 *
	 * @param key
	 *        the key, either as returned by {@link MarketType#getKey()} or the
	 *        enumeration name itself
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code key} is not a valid value
	 */
	public static MarketType forKey(String key) {
		for ( MarketType e : MarketType.values() ) {
			if ( e.key.equals(key) || e.name().equalsIgnoreCase(key) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown MarketType key [" + key + "]");
	}

}

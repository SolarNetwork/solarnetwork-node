/* ==================================================================
 * TranspositionModel.java - 28/11/2024 11:09:23 am
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

package net.solarnetwork.node.datum.pvlib;

/**
 * Enumeration of supported transposition models to determine diffuse irradiance
 * from the sky on a tilted surface.
 *
 * @author matt
 * @version 1.0
 */
public enum TranspositionModel {

	/** Hay &amp; Davies model. */
	HayDavies("haydavies"),

	/** Perez-Driesse model. */
	PerezDriesse("perez-driesse"),

	;

	private final String key;

	private TranspositionModel(String key) {
		this.key = key;
	}

	/**
	 * Get the key value.
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
	 *        the key
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code key} is not a valid value
	 */
	public static TranspositionModel forKey(String key) {
		for ( TranspositionModel e : TranspositionModel.values() ) {
			if ( e.name().equals(key) || e.key.equalsIgnoreCase(key) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown TranspositionModel key [" + key + "]");
	}

}

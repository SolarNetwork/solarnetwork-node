/* ==================================================================
ddddddd * DatumPropertySampleType.java - 20/12/2017 1:44:58 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.modbus;

/**
 * The sample type for a datum property.
 * 
 * @author matt
 * @version 1.0
 */
public enum DatumPropertySampleType {

	Instantaneous('i'),

	Accumulating('a'),

	Status('s');

	private final char type;

	private DatumPropertySampleType(char type) {
		this.type = type;
	}

	/**
	 * Get a key value for this enum.
	 * 
	 * @return the key
	 */
	public char toKey() {
		return type;
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
	public static DatumPropertySampleType valueOf(char key) {
		for ( DatumPropertySampleType e : DatumPropertySampleType.values() ) {
			if ( key == e.type ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown DatumPropertySampleType key [" + key + "]");
	}

}

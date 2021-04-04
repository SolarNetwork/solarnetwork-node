/* ==================================================================
 * ModbusWordOrder.java - 5/05/2018 11:53:55 AM
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

package net.solarnetwork.node.io.modbus;

/**
 * A word-ordering for multi-register data types in Modbus.
 * 
 * @author matt
 * @version 1.0
 * @since 2.7
 */
public enum ModbusWordOrder {

	MostToLeastSignificant('m'),

	LeastToMostSignificant('l');

	private final char key;

	private ModbusWordOrder(char key) {
		this.key = key;
	}

	/**
	 * Get the key value for this enum.
	 * 
	 * @return the key
	 */
	public char getKey() {
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
	public static ModbusWordOrder forKey(char key) {
		for ( ModbusWordOrder e : ModbusWordOrder.values() ) {
			if ( key == e.key ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown ModbusWordOrder key [" + key + "]");
	}

	/**
	 * Get a friendly display string for this enum.
	 * 
	 * @return the display string
	 */
	public String toDisplayString() {
		switch (this) {
			case MostToLeastSignificant:
				return "Most significant word first";

			case LeastToMostSignificant:
				return "Least significant word first";

			default:
				return this.toString();
		}
	}

}

/* ==================================================================
 * InputUnit.java - 20/11/2018 2:48:40 PM
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

package net.solarnetwork.node.hw.advantech.adam;

/**
 * Input measurement units.
 * 
 * @author matt
 * @version 1.0
 */
public enum InputUnit {

	Amps("A", "Amps"),

	Volts("V", "Volts"),

	DegreeCelsius("℃", "℃"),

	Unknown("?", "Unknown");

	private final String key;
	private final String description;

	private InputUnit(String key, String description) {
		this.key = key;
		this.description = description;
	}

	/**
	 * Get the unit key.
	 * 
	 * @return the unit key
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

}

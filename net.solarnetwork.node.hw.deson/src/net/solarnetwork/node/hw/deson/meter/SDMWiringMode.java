/* ==================================================================
 * SDMWiringMode.java - Jul 12, 2016 12:09:17 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.deson.meter;

/**
 * Wiring mode.
 * 
 * @author matt
 * @version 2.0
 */
public enum SDMWiringMode {

	OnePhaseTwoWire(1, "1P-2W", "1 phase, 2 wire"),

	ThreePhaseThreeWire(2, "3P-3W", "3 phase, 3 wire"),

	ThreePhaseFourWire(3, "3P-4W", "3 phase, 4 wire");

	private int type;
	private final String displayName;
	private final String description;

	private SDMWiringMode(int type, String displayName, String description) {
		this.type = type;
		this.displayName = displayName;
		this.description = description;
	}

	/**
	 * Get the raw meter type value for this wiring mode.
	 * 
	 * @return The value reported by the meter.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Get a description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get the display value shown by the meter for this wiring mode.
	 * 
	 * @return The display value.
	 */
	public String displayName() {
		return displayName;
	}

	/**
	 * Get a wiring mode for a given meter raw type value.
	 * 
	 * @param type
	 *        The type.
	 * @return The mode.
	 * @throws IllegalArgumentException
	 *         if the {@code type} value is not supported.
	 */
	public static SDMWiringMode valueOf(int type) {
		switch (type) {
			case 1:
				return OnePhaseTwoWire;

			case 2:
				return ThreePhaseThreeWire;

			case 3:
				return ThreePhaseFourWire;

			default:
				throw new IllegalArgumentException("Wiring type " + type + " not supported");
		}
	}

}

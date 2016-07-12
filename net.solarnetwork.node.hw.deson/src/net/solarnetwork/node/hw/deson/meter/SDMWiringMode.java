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
 * @version 1.0
 */
public enum SDMWiringMode {

	OnePhaseTwoWire(1),

	ThreePhaseThreeWire(2),

	ThreePhaseFourWire(3);

	private int type;

	SDMWiringMode(int type) {
		this.type = type;
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

	/**
	 * Get the display value shown by the meter for this wiring mode.
	 * 
	 * @return The display value.
	 */
	public String displayString() {
		switch (this) {
			case OnePhaseTwoWire:
				return "1P2";

			case ThreePhaseThreeWire:
				return "1P3";

			case ThreePhaseFourWire:
				return "1P4";

			default:
				// shouldn't get here
				throw new RuntimeException("Unsupported wiring mode: " + this);
		}
	}
}

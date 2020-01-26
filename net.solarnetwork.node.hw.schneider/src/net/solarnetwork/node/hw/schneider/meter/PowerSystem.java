/* ==================================================================
 * PowerSystem.java - 17/05/2018 6:38:05 PM
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

package net.solarnetwork.node.hw.schneider.meter;

/**
 * Enumeration of the "power system" configuration on a PM5100 meter.
 * 
 * @author matt
 * @version 1.0
 * @since 2.4
 */
public enum PowerSystem {

	OnePhaseTwoWireLineNeutral(0, "1ph, 2w, LN"),

	OnePhaseTwoWireLineLine(1, "1ph, 2w, LL"),

	OnePhaseThreeWireLineLineWithNeutral(2, "1ph, 3w, LL with N (2phase)"),

	ThreePhaseThreeWireDeltaUngrounded(3, "3ph, 3w, Delta, Ungrounded"),

	ThreePhaseThreeWireDeltaUCornerGrounded(4, "3ph, 3w, Delta, Corner Grounded"),

	ThreePhaseThreeWireWyeUngrounded(5, "3ph, 3w, Wye, Ungrounded"),

	ThreePhaseThreeWireWyeGrounded(6, "3ph, 3w, Wye Grounded"),

	ThreePhaseThreeWireWyeResistanceGrounded(7, "3ph, 3w, Wye, Resistance Grounded"),

	ThreePhaseFourWireOpenDeltaCenterTapped(8, "3ph, 4w, Open Delta, Center-Tapped"),

	ThreePhaseFourWireDeltaCenterTapped(9, "3ph, 4w, Delta, Center-Tapped"),

	ThreePhaseFourWireWyeUngrounded(10, "3ph, 4w, Wye, Ungrounded"),

	ThreePhaseFourWireWyeGrounded(11, "3ph, 4w, Wye Grounded"),

	ThreePhaseFourWireWyeResistanceGrounded(12, "3ph, 4w, Wye, Resistance Grounded"),

	MultiCircuitThreeLineNeutral(13, "Multi-Circuit 3 circuit LN"),

	MultiCircuitTwoLineNeutral(14, "Multi-Circuit 2 Circuit LN"),

	MultiCircuitOneLineNeutral(15, "Multi-Circuit 1 Cicuit LN"),

	MultiCircuitThreeLineLine(16, "Multi-Circuit 3 Circuit LL"),

	MultiCircuitTwoLineLine(17, "Multi-Circuit 2 Circuit LL"),

	MultiCircuitOneLineLine(18, "Multi-Circuit 1 Circuit LL"),

	MultiCircuitOneLineLineOneLineNeutral(19, "Multi-Circuit 1 Circuit LL 1 Circuit LN"),

	MultiCircuitWye(20, "Multi-Circuit Wye");

	private final int code;
	private final String description;

	private PowerSystem(int code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get the code value.
	 * 
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get the description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get an enumeration for a code value.
	 * 
	 * @param code
	 *        the code to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static PowerSystem forCode(int code) {
		for ( PowerSystem e : PowerSystem.values() ) {
			if ( e.code == code ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Code [" + code + "] not supported");
	}

}

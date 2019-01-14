/* ==================================================================
 * InverterApparentPowerCalculationMethod.java - 15/10/2018 2:18:04 PM
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

package net.solarnetwork.node.hw.sunspec.inverter;

import net.solarnetwork.node.hw.sunspec.ApparentPowerCalculationMethod;
import net.solarnetwork.node.hw.sunspec.ModelData;

/**
 * Apparent power calculation method for inverters.
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public enum InverterApparentPowerCalculationMethod implements ApparentPowerCalculationMethod {

	Vector(1, "Switch VAR characterization"),

	Arithmetic(2, "Maintain VAR characterization");

	final private int code;
	final private String description;

	private InverterApparentPowerCalculationMethod(int index, String description) {
		this.code = index;
		this.description = description;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
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
	public static InverterApparentPowerCalculationMethod forCode(int code) {
		if ( (code & ModelData.NAN_ENUM16) == ModelData.NAN_ENUM16 ) {
			return null;
		}
		for ( InverterApparentPowerCalculationMethod e : InverterApparentPowerCalculationMethod
				.values() ) {
			if ( e.code == code ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Code [" + code + "] not supported");
	}

}

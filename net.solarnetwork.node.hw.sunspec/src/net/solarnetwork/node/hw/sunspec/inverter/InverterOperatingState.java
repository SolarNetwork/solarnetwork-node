/* ==================================================================
 * OperatingState.java - 5/10/2018 4:32:22 PM
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

import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.OperatingState;

/**
 * Operating state for inverters.
 * 
 * @author matt
 * @version 1.1
 */
public enum InverterOperatingState implements OperatingState {

	Normal(0, "Device operating normally"),

	Off(1, "Device is not operating"),

	Sleeping(2, "Device is sleeping / auto-shudown"),

	Starting(3, "Device is staring up"),

	Mppt(4, "Device is auto tracking maximum power point"),

	Throttled(5, "Device is operating at reduced power output"),

	ShuttingDown(6, "Device is shutting down"),

	Fault(7, "One or more faults exist"),

	Standby(8, "Device is in standby mode"),

	Test(9, "Device is in test mode");

	final private int code;
	final private String description;

	private InverterOperatingState(int index, String description) {
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
	 * Get an enumeration for an index value.
	 * 
	 * @param code
	 *        the code to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static InverterOperatingState forCode(int code) {
		if ( (code & ModelData.NAN_ENUM16) == ModelData.NAN_ENUM16 ) {
			return Normal;
		}
		for ( InverterOperatingState e : InverterOperatingState.values() ) {
			if ( e.code == code ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Code [" + code + "] not supported");
	}

}

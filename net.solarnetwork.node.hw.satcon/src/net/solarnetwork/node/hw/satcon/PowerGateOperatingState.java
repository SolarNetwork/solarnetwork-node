/* ==================================================================
 * PowerGateOperatingState.java - 11/09/2019 4:05:09 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.satcon;

import net.solarnetwork.domain.DeviceOperatingState;

/**
 * Enumeration of operating states.
 * 
 * @author matt
 * @version 1.0
 */
public enum PowerGateOperatingState {

	PowerUp(0, "Power up"),

	Shutdown(1, "Shutdown"),

	Stop(2, "Stop"),

	PrechargeClosed(3, "Precharge closed"),

	OutputClosed(4, "Output closed"),

	MatchDcVoltage(5, "Match DC voltage"),

	InputClosed(6, "Input closed"),

	Run(7, "Run"),

	Test1(8, "Factory test 1"),

	Test2(9, "Factory test 2"),

	Test3(10, "Factory test 3"),

	Test4(11, "Factory test 4"),

	Test5(12, "Factory test 5");

	private final int code;
	private final String description;

	private PowerGateOperatingState(int value, String description) {
		this.code = value;
		this.description = description;
	}

	/**
	 * Get the type value encoding.
	 * 
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get a description of the type.
	 * 
	 * @return a description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get a device operating state for this state.
	 * 
	 * @return the device operating state
	 */
	public DeviceOperatingState asDeviceOperatingState() {
		switch (this) {
			case PowerUp:
			case Test1:
			case Test2:
			case Test3:
			case Test4:
			case Test5:
				return DeviceOperatingState.Starting;

			case Shutdown:
				return DeviceOperatingState.Shutdown;

			case Stop:
				return DeviceOperatingState.Standby;

			case PrechargeClosed:
			case OutputClosed:
				return DeviceOperatingState.Fault;

			case MatchDcVoltage:
			case Run:
				return DeviceOperatingState.Normal;

			default:
				return DeviceOperatingState.Unknown;

		}
	}

	/**
	 * Get an enumeration for a given code value.
	 * 
	 * @param code
	 *        the code to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static PowerGateOperatingState forCode(int code) {
		for ( PowerGateOperatingState s : values() ) {
			if ( s.code == code ) {
				return s;
			}
		}
		throw new IllegalArgumentException("Unsupported operating state value: " + code);
	}

}

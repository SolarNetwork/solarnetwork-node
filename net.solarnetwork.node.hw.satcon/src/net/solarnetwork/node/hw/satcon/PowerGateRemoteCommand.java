/* ==================================================================
 * PowerGateRemoteCommand.java - 11/09/2019 4:27:44 pm
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

/**
 * Enumeration of remote commands.
 * 
 * @author matt
 * @version 1.0
 */
public enum PowerGateRemoteCommand {

	EnableRun(0x01, "Enable run"),

	DisableRun(0x02, "Disable run"),

	ResetFaults(0x04, "Reset faults"),

	Shutdown(0x08, "Shutdown"),

	SelectLocalDigitalPowerCommands(0x10, "Select local digital power commands"),

	SelectRemoteDigitalPowerCommands(0x20, "Select remote digital power commands"),

	SelectAnalogPowerCommands(0x40, "Select analog power commands");

	private final int code;
	private final String description;

	private PowerGateRemoteCommand(int value, String description) {
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
	 * Get an enumeration for a given code value.
	 * 
	 * @param code
	 *        the code to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static PowerGateRemoteCommand forCode(int value) {
		for ( PowerGateRemoteCommand s : values() ) {
			if ( s.code == value ) {
				return s;
			}
		}
		throw new IllegalArgumentException("Unsupported remote command value: " + value);
	}

}

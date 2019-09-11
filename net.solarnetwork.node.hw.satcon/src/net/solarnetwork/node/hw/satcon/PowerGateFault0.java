/* ==================================================================
 * PowerGateFault0.java - 11/09/2019 11:06:27 am
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
 * Bitmask enumeration of fault 0 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum PowerGateFault0 implements PowerGateFault {

	DcInputNotReady(
			0,
			"The DC input voltage has not been above DC Input Voltage Threshold for DC Input Voltage Delay."),

	LineNotReady(1, "The AC line voltage has not been above the required level for Reconnect Delay."),

	StopCommand(2, "Stopped because it has received a software stop command."),

	ShutdownCommand(3, "Stopped because it has received a software shutdown command."),

	Estop(4, "Stopped because the hardware estop switch is open."),

	LowPowerStop(
			5,
			"Stopped because the power output has remained below Low Power Trip for more than Low Power Delay."),

	LowCurrentStop(
			6,
			"Stopped because the power output has remained below Low Power Trip for more than Low Power Delay."),

	DoorOpen(8, "One of the doors is open."),

	DisconnectOpen(9, "The DC input disconnect DS1 is open."),

	BreakerOpen(10, "The AC line circuit breaker is open."),

	DpcbFault(11, "Digital Power Control Board fault."),

	HardwareFault(12, "Hardware fault."),

	InverterFault(13, "Inverter fault."),

	TemperatureFault(14, "Temperature fault.");

	private final int code;
	private final String description;

	private PowerGateFault0(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getFaultGroup() {
		return 0;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get the description for this condition.
	 * 
	 * @return the description
	 */
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int bitmaskBitOffset() {
		return code;
	}

	/**
	 * Get an enum for a code value.
	 * 
	 * @param code
	 *        the code to get an enum for
	 * @return the enum with the given {@code code}, or {@literal null} if
	 *         {@code code} is {@literal 0}
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static PowerGateFault0 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( PowerGateFault0 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("PowerGateFault0 code [" + code + "] not supported");
	}

}

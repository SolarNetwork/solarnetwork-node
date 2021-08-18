/* ==================================================================
 * AE250TxPvmStatus.java - 12/08/2021 9:28:24 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.ae.inverter.tx;

import net.solarnetwork.domain.CodedValue;

/**
 * AE250TX PV monitoring status.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum AE250TxPvmStatus implements CodedValue {

	Rebooting(0, "Rebooting."),

	InverterCommFault(1, "Unable to communicate with inverter."),

	WebPostFault(2, "Web post fault."),

	DnsServerFault(3, "DNS server fault."),

	ClockError(
			4,
			"Real time clock error, the battery is dead or cannot synchronize with network time server."),

	WrongFirmware(5, "Incompatible or incorrect revision of communications firmware."),

	ModbusAddressError(6, "Failed reading the Modbus address switches."),

	;

	private final int code;
	private final String description;

	private AE250TxPvmStatus(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get a description of the status.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}

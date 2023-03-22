/* ==================================================================
 * PVITLInverterState.java - 21/08/2018 2:24:24 PM
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

package net.solarnetwork.node.hw.yaskawa.mb.inverter;

import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.domain.DeviceOperatingState;

/**
 * Enumeration of inverter states.
 * 
 * @author matt
 * @version 1.1
 */
public enum PVITLInverterState implements CodedValue {

	/** Fault. */
	Fault(0x8000, "Fault"),

	/** Check. */
	Check(0x4000, "Check"),

	/** Standby. */
	Standby(0x2000, "Standby"),

	/** Running. */
	Running(0x1000, "Running"),

	/** Derate. */
	Derate(0x0800, "Derate"),

	;

	private final int code;
	private final String description;

	private PVITLInverterState(int value, String description) {
		this.code = value;
		this.description = description;
	}

	@Override
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
	 * Get a device operating state for this inverter state.
	 * 
	 * @return the device operating state
	 * @since 1.1
	 */
	public DeviceOperatingState asDeviceOperatingState() {
		switch (this) {
			case Check:
				return DeviceOperatingState.Starting;

			case Derate:
				return DeviceOperatingState.Override;

			case Fault:
				return DeviceOperatingState.Fault;

			case Standby:
				return DeviceOperatingState.Standby;

			default:
				return DeviceOperatingState.Normal;
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
	public static PVITLInverterState forCode(int code) {
		for ( PVITLInverterState s : values() ) {
			if ( s.code == code ) {
				return s;
			}
		}
		throw new IllegalArgumentException("Unsupported work mode value: " + code);
	}

}

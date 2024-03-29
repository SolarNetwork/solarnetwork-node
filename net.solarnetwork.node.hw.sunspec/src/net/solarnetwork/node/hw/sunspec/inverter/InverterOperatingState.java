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

import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.OperatingState;

/**
 * Operating state for inverters.
 * 
 * @author matt
 * @version 1.3
 */
public enum InverterOperatingState implements OperatingState {

	/** Device operating normally. */
	Normal(0, "Device operating normally"),

	/** Device is not operating. */
	Off(1, "Device is not operating"),

	/** Device is sleeping / auto-shudown. */
	Sleeping(2, "Device is sleeping / auto-shudown"),

	/** Device is starting up. */
	Starting(3, "Device is starting up"),

	/** Device is auto tracking maximum power point. */
	Mppt(4, "Device is auto tracking maximum power point"),

	/** Device is operating at reduced power output. */
	Throttled(5, "Device is operating at reduced power output"),

	/** Device is shutting down. */
	ShuttingDown(6, "Device is shutting down"),

	/** One or more faults exist. */
	Fault(7, "One or more faults exist"),

	/** Device is in standby mode. */
	Standby(8, "Device is in standby mode"),

	/** Device is in test mode. */
	Test(9, "Device is in test mode");

	private final int code;
	private final String description;

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
	 * Get a {@link DeviceOperatingState}.
	 * 
	 * @return the device operating state, never {@literal null}
	 * @since 1.2
	 */
	@Override
	public DeviceOperatingState asDeviceOperatingState() {
		switch (this) {
			case Normal:
			case Mppt:
				return DeviceOperatingState.Normal;

			case Off:
			case ShuttingDown:
				return DeviceOperatingState.Shutdown;

			case Sleeping:
			case Standby:
				return DeviceOperatingState.Standby;

			case Starting:
			case Test:
				return DeviceOperatingState.Starting;

			case Throttled:
				return DeviceOperatingState.Override;

			case Fault:
				return DeviceOperatingState.Fault;

			default:
				return DeviceOperatingState.Unknown;

		}
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

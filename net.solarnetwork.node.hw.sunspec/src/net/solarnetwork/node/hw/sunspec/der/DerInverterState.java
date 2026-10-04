/* ==================================================================
 * DerInverterState.java - 5/10/2026 8:27:22 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.der;

import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.node.hw.sunspec.inverter.InverterOperatingState;

/**
 * DER inverter state.
 *
 * <p>
 * Note the codes of this state are different from the codes of the
 * {@link InverterOperatingState} used by the inverter models. Use
 * {@link #asInverterOperatingState()} to get the equivalent inverter operating
 * state.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum DerInverterState implements CodedValue {

	/** Off. */
	Off(0, "Off"),

	/** Sleeping. */
	Sleeping(1, "Sleeping"),

	/** Starting. */
	Starting(2, "Starting"),

	/** Running. */
	Running(3, "Running"),

	/** Active power throttled. */
	Throttled(4, "Active power throttled"),

	/** Shutting down. */
	ShuttingDown(5, "Shutting down"),

	/** Fault. */
	Fault(6, "Fault"),

	/** Standby. */
	Standby(7, "Standby"),

	;

	private final int code;
	private final String description;

	private DerInverterState(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get a description of the state.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get the equivalent inverter operating state.
	 *
	 * @return the inverter operating state, never {@code null}
	 */
	public InverterOperatingState asInverterOperatingState() {
		switch (this) {
			case Off:
				return InverterOperatingState.Off;

			case Sleeping:
				return InverterOperatingState.Sleeping;

			case Starting:
				return InverterOperatingState.Starting;

			case Running:
				return InverterOperatingState.Normal;

			case Throttled:
				return InverterOperatingState.Throttled;

			case ShuttingDown:
				return InverterOperatingState.ShuttingDown;

			case Fault:
				return InverterOperatingState.Fault;

			default:
				return InverterOperatingState.Standby;
		}
	}

}

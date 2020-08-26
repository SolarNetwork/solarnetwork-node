/* ==================================================================
 * AE500NxFault.java - 22/04/2020 11:38:44 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.ae.inverter.nx;

import net.solarnetwork.domain.Bitmaskable;

/**
 * AE500NX system status bitmask enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public enum AE500NxSystemStatus implements Bitmaskable {

	Power(0, "Set if unit is on."),

	Fault(1, "Set if unit has one or more active faults."),

	Limit(2, "Set if unit operation has been affected by one or more operating limits."),

	Enabled(3, "Set if master control enabled."),

	Startup(4, "Set if unit is in startup mode."),

	Warning(5, "Set if unit has one or more active warnings."),

	Lockout(6, "Set if the unit has been locked out."),

	Mppt(8, "Set if MPPT is active."),

	Sleep(9, "Set for sleep."),

	Autostart(10, "Set if auto-start is on."),

	BadMov(11, "Set if a surge protection device has failed.");

	private final int bit;
	private final String description;

	private AE500NxSystemStatus(int bit, String description) {
		this.bit = bit;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return bit;
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

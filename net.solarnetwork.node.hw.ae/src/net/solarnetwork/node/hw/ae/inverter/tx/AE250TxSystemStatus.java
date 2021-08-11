/* ==================================================================
 * AE250TxSystemStatus.java - 12/08/2021 9:12:45 AM
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

import net.solarnetwork.domain.Bitmaskable;

/**
 * AE250TX system status bitmask enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum AE250TxSystemStatus implements Bitmaskable {

	Sleep(0, "Sleep state"),

	StartupDelay(1, "Startup delay"),

	AcPrecharge(2, "AC precharge"),

	DcPrecharge(3, "DC precharge"),

	Idle(4, "Idle"),

	PowerTrack(5, "Power track"),

	Fault(9, "Fault"),

	Initialization(10, "Initialization"),

	Disabled(11, "Disabled"),

	Latching(12, "Latching"),

	CoolDown(13, "Cool down");

	private final int bit;
	private final String description;

	private AE250TxSystemStatus(int bit, String description) {
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

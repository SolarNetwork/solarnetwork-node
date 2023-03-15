/* ==================================================================
 * AE250TxMainFault.java - 12/08/2021 9:28:24 AM
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

/**
 * AE250TX main faults.
 * 
 * @author matt
 * @version 1.1
 * @since 3.2
 */
public enum AE250TxMainFault implements AE250TxFault {

	/** Drive. */
	Drive(0, "A general Drive type fault has occurred"),

	/** Voltage. */
	Voltage(1, "A general Voltage type fault has occurred"),

	/** Grid. */
	Grid(2, "A general Grid type fault has occurred"),

	/** Temperature. */
	Temperature(3, "A general Temperature type fault has occurred"),

	/** System. */
	System(4, " A general System type fault has occurred"),

	/** Latching. */
	Latching(15, " A general Latching type fault has occurred"),

	;

	private final int bit;
	private final String description;

	private AE250TxMainFault(int bit, String description) {
		this.bit = bit;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return bit;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int getGroupIndex() {
		return 0;
	}

}

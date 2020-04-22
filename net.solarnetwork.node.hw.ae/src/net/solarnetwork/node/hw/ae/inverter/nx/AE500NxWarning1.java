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

/**
 * AE500NX warning bitmask enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public enum AE500NxWarning1 implements AE500NxWarning {

	Fan1(1, "Fan not operating normally."),

	Fan2(2, "Fan not operating normally."),

	Fan3(3, "Fan not operating normally."),

	Fan4(4, "Fan not operating normally."),

	Fan5(5, "Fan not operating normally."),

	Fan6(6, "Fan not operating normally."),

	Fan7(7, "Fan not operating normally."),

	Fan8(13, "Charge abatement option not operating correctly."),;

	private final int bit;
	private final String description;

	private AE500NxWarning1(int bit, String description) {
		this.bit = bit;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return bit;
	}

	@Override
	public int getWarningGroup() {
		return 0;
	}

	@Override
	public String getDescription() {
		return description;
	}

}

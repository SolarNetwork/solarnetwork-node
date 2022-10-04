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
 * AE250TX system warnings.
 * 
 * <p>
 * These are not documented in the manual provided by AE.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum AE250TxSystemWarning implements AE250TxWarning {

	/** Fan 1 warning. */
	Fan1(0, "Fan 1 warning"),

	/** Fan 2 warning. */
	Fan2(1, "Fan 2 warning"),

	/** Fan 3 warning. */
	Fan3(2, "Fan 3 warning"),

	/** Magnetics high temp warning. */
	MagHighTemp(3, "Magnetics high temp warning"),

	/** Power foldback warning. */
	HighTempPowerLimit(4, "Power foldback warning"),

	/** Heatsink delta temp warning. */
	DeltaTemp(5, "Heatsink delta temp warning"),

	// 6: missing

	/** GFDI current warning. */
	GfdiCurrent(7, "GFDI current warning"),

	/** AC surge warning. */
	AcSurge(8, "AC surge warning"),

	/** DC surge warning. */
	DcSurge(9, "DC surge warning"),

	/** DC current warning. */
	DcCurrent(10, "DC current warning"),

	/** IPM current warning. */
	IpmCurrent(11, "IPM current warning"),

	/** 24V power supply warning. */
	Ps24V(12, "24V power supply warning"),

	/** DC bleed circuit warning. */
	DcBleed(13, "DC bleed circuit warning"),

	;

	private final int bit;
	private final String description;

	private AE250TxSystemWarning(int bit, String description) {
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

}

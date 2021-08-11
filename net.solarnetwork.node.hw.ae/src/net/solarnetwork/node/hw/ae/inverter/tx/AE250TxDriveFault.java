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
 * AE250TX drive faults.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum AE250TxDriveFault implements AE250TxFault {

	DriveALow(0, "Drive protection fault, phase A low."),

	DriveAHigh(1, "Drive protection fault, phase A high."),

	DriveBLow(2, "Drive protection fault, phase B low."),

	DriveBHigh(3, "Drive protection fault, phase B high."),

	DriveCLow(4, "Drive protection fault, phase C low."),

	DriveCHigh(5, "Drive protection fault, phase C high."),

	HwOverCurrentA(6, "Peak over-current, phase A."),

	HwOverCurrentB(7, "Peak over-current, phase B."),

	HwOverCurrentC(8, "Peak over-current, phase C."),

	RmsOverCurrentA(6, "RMS over-current, phase A."),

	RmsOverCurrentB(7, "RMS over-current, phase B."),

	RmsOverCurrentC(8, "RMS over-current, phase C."),

	DcOverVoltage(7, "DC volts over range."),

	DcUnderVoltage(8, "DC volts under range."),

	;

	private final int bit;
	private final String description;

	private AE250TxDriveFault(int bit, String description) {
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

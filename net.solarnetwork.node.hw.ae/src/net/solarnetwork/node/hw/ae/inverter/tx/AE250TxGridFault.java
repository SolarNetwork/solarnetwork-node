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
 * AE250TX grid faults.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum AE250TxGridFault implements AE250TxFault {

	AcFastUnderVoltA(0, "Fast AC voltage low, phase A."),

	AcFastUnderVoltB(1, "Fast AC voltage low, phase B."),

	AcFastUnderVoltC(2, "Fast AC voltage low, phase C."),

	AcSlowUnderVoltA(3, "Slow AC voltage low, phase A."),

	AcSlowUnderVoltB(4, "Slow AC voltage low, phase B."),

	AcSlowUnderVoltC(5, "Slow AC voltage low, phase C."),

	AcFastOverVoltA(6, "Fast AC voltage high, phase A."),

	AcFastOverVoltB(7, "Fast AC voltage high, phase B."),

	AcFastOverVoltC(8, "Fast AC voltage high, phase C."),

	AcSlowOverVoltA(9, "Slow AC voltage high, phase A."),

	AcSlowOverVoltB(10, "Slow AC voltage high, phase B."),

	AcSlowOverVoltC(11, "Slow AC voltage high, phase C."),

	AcUnderFreq(12, "Low frequency fault."),

	AcOverFreq(13, "High frequency fault."),

	;

	private final int bit;
	private final String description;

	private AE250TxGridFault(int bit, String description) {
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

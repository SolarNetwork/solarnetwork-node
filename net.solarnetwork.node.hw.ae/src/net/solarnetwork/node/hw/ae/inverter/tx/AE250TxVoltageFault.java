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
 * AE250TX voltage faults.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum AE250TxVoltageFault implements AE250TxFault {

	VacOverPeakA(0, "Peak AC voltage high, phase A."),

	VacOverPeakB(1, "Peak AC voltage high, phase B."),

	VacOverPeakC(2, "Peak AC voltage high, phase C."),

	PllFault(3, "Control PLL fault."),

	AcUnbalancedFault(4, "AC voltages unbalanced."),

	DcOverVoltage(5, "DC voltage high."),

	PowerSupplyP5(6, "+5V power supply fault."),

	PowerSupply1P5(7, "+15V power supply fault."),

	PowerSupplyM15(8, "-15V power supply fault."),

	PowerSupply10(9, "10V power supply fault."),

	PowerSupply24(10, "24V power supply fault."),

	DcPrecharage(11, "DC precharge fault."),

	PvDcDelta(12, "PV input and DC bus voltage delta."),

	;

	private final int bit;
	private final String description;

	private AE250TxVoltageFault(int bit, String description) {
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
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
 * @version 1.1
 * @since 3.2
 */
public enum AE250TxVoltageFault implements AE250TxFault {

	/** Peak AC voltage high, phase A. */
	VacOverPeakA(0, "Peak AC voltage high, phase A."),

	/** Peak AC voltage high, phase B. */
	VacOverPeakB(1, "Peak AC voltage high, phase B."),

	/** Peak AC voltage high, phase C. */
	VacOverPeakC(2, "Peak AC voltage high, phase C."),

	/** Control PLL fault. */
	PllFault(3, "Control PLL fault."),

	/** AC voltages unbalanced. */
	AcUnbalancedFault(4, "AC voltages unbalanced."),

	/** DC voltage high. */
	DcVoltageHigh(5, "DC voltage high."),

	/** +5V power supply fault. */
	PowerSupplyP5(6, "+5V power supply fault."),

	/** +15V power supply fault. */
	PowerSupply1P5(7, "+15V power supply fault."),

	/** -15V power supply fault. */
	PowerSupplyM15(8, "-15V power supply fault."),

	/** 10V power supply fault. */
	PowerSupply10(9, "10V power supply fault."),

	/** 24V power supply fault. */
	PowerSupply24(10, "24V power supply fault."),

	/** DC precharge fault. */
	DcPrecharage(11, "DC precharge fault."),

	/** PV input and DC bus voltage delta. */
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

	@Override
	public int getGroupIndex() {
		return 2;
	}

}

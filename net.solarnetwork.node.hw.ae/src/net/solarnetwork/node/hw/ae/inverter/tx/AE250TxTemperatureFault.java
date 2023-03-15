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
 * AE250TX temperature faults.
 * 
 * @author matt
 * @version 1.1
 * @since 3.2
 */
public enum AE250TxTemperatureFault implements AE250TxFault {

	/** Module heat sink A1 temperature high. */
	HeatsinkTempA1(0, "Module heat sink A1 temperature high."),

	/** Module heat sink A2 temperature high. */
	HeatsinkTempA2(1, "Module heat sink A2 temperature high."),

	/** Module heat sink B1 temperature high. */
	HeatsinkTempB1(2, "Module heat sink B1 temperature high."),

	/** Module heat sink B2 temperature high. */
	HeatsinkTempB2(3, "Module heat sink B2 temperature high."),

	/** Module heat sink C1 temperature high. */
	HeatsinkTempC1(4, "Module heat sink C1 temperature high."),

	/** Module heat sink C2 temperature high. */
	HeatsinkTempC2(5, "Module heat sink C2 temperature high."),

	/** Control board temperature high. */
	BoardTempHigh(6, "Control board temperature high."),

	/** Drive temperature low. */
	DriveTempLow(7, "Drive temperature low."),

	/** Magnetics temperature high. */
	MagTempHigh(8, "Magnetics temperature high."),

	/** Ambient temperature low. */
	AmbientTempLow(9, "Ambient temperature low."),

	/** Magnetics temperature low. */
	MagTempLow(10, "Magnetics temperature low."),

	/** IPM temperature high. */
	IpmTempHigh(11, "IPM temperature high."),

	/** Inductor temperature high. */
	InductorTempHigh(12, "Inductor temperature high."),

	;

	private final int bit;
	private final String description;

	private AE250TxTemperatureFault(int bit, String description) {
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
		return 4;
	}

}

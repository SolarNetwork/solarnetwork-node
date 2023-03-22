/* ==================================================================
 * PVITLPermanentFault.java - 22/03/2023 4:12:23 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.yaskawa.mb.inverter;

/**
 * Fault1 enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum PVITLFault1 implements PVITLFault {

	/** Inverter voltage offset. */
	InverterVoltageOffset(0, "Inverter voltage offset"),

	/** DCI offset. */
	DciOffset(1, "DCI offset"),

	/** DCI high. */
	DciHigh(2, "DCI high"),

	/** Insulation resistance low. */
	InsulationResistanceLow(3, "Insulation resistance low"),

	/** Dynamic leakage current high. */
	DynamicLeakageCurrentHigh(4, "Dynamic leakage current high"),

	/** Frequency detection. */
	FrequencyDetection(5, "Frequency detection"),

	/** MCU protection (from another Snap). */
	McuProtection(7, "MCU protection"),

	/** Inverter hardware over current. */
	InverterHardwareOverCurrent(8, "Inverter hardware over current"),

	/** Grid voltage imbalance. */
	GridVoltageImbalance(9, "Grid voltage imbalance"),

	/** Inverter current imbalance. */
	InverterCurrentImbalance(11, "Inverter current imbalance"),

	/** Power module protection. */
	PowerModuleProtection(12, "Power module protection"),

	/** Leakage current sensor. */
	LeakageCurrentSensor(15, "Leakage current sensor"),

	;

	private final int code;
	private final String description;

	private PVITLFault1(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return code;
	}

	@Override
	public int getGroupIndex() {
		return 2;
	}

	@Override
	public String getDescription() {
		return description;
	}

}

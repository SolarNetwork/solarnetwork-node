/* ==================================================================
 * PvStatus.java - 17/03/2023 10:12:52 am
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

package net.solarnetwork.node.hw.yaskawa.ecb;

/**
 * AC status enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum AcStatus implements PVIStatus {

	/** The voltage is too critically high. */
	CriticalOverVoltage(0, "Critical over voltage"),

	/** The voltage is too high. */
	OverVoltage(1, "Over voltage"),

	/** Under voltage. */
	UnderVoltage(2, "Under voltage"),

	/** Critical under voltage. */
	CriticalUnderVoltage(3, "Critical under voltage"),

	/** High frequency. */
	HighFrequency(4, "High frequency"),

	/** Low frequency. */
	LowFrequency(5, "Low frequency"),

	/** DC injection. */
	DcInjection(6, "DC injection"),

	/** AC synchronisation. */
	AcSync(7, "AC synchronisation"),

	/** Islanding detected. */
	IslandingDetected(8, "Islanding detected"),

	/** Power reduction active. */
	PowerReductionActive(9, "Power reduction active"),

	/** Connection condition. */
	ConnectionCondition(10, "Connection condition"),

	/** Fault ride through active. */
	FaultRideThroughActive(11, "Fault ride through active"),

	/** Soft start. */
	SoftStart(12, "Soft start"),

	/** Hardware critical over voltage. */
	HardwareCriticalOverVoltage(13, "Hardware critical over voltage"),

	/** Active power limited to allow for reactive power. */
	ActivePowerLimitedForReactivePower(14, "Active power limited to allow for reactive power"),

	/** Long grid out. */
	LongGridOut(15, "Long grid out"),

	/** Grid synchronisation error. */
	GridSynchronisationError(16, "Grid synchronisation error"),

	;

	private final int code;
	private final String description;

	private AcStatus(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return code;
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

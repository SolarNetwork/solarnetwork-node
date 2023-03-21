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
 * PV status enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum PvStatus implements PVIStatus {

	/** The voltage is too low. */
	VoltageLow(0, "PV voltage too low"),

	/** The power is too low. */
	PowerLow(1, "PV power too low"),

	/** The voltage is almost too low. */
	VoltageLowWarning(2, "PV voltage too low warning"),

	/** The power is limited to Pn. */
	PowerLimitation(3, "Power limitation to Pn"),

	/** The power is reduced because of high temperature. */
	TemperatureDerating(4, "Temperature derating"),

	;

	private final int code;
	private final String description;

	private PvStatus(int code, String description) {
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
		return 0;
	}

}

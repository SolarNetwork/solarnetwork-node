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
 * PV ISO/GND status enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum PvIsoStatus implements PVIStatus {

	/** ISO startup failure. */
	IsoStartupFailure(0, "ISO startup failure"),

	/** ISO running failure. */
	IsoRunningFailure(1, "ISO running failure"),

	/** PV+ grounding failure. */
	PVPositiveGroundingFailure(2, "PV+ grounding failure"),

	/** PV- grounding failure. */
	PVNegativeGroundingFailure(3, "PV- grounding failure"),

	/** ISO startup warning. */
	IsoStartupWarning(4, "ISO startup warning"),

	/** ISO running warning. */
	IsoRunningWarning(5, "ISO running warning"),

	/** PV+ grounding warning. */
	PVPositiveGroundingWarning(6, "PV+ grounding warning"),

	/** PV- grounding warning. */
	PVNegativeGroundingWarning(7, "PV- grounding warning"),

	;

	private final int code;
	private final String description;

	private PvIsoStatus(int code, String description) {
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
		return 1;
	}

}

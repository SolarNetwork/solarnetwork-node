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
public enum PVITLFault4 implements PVITLFault {

	/** 15V control board low. */
	ControlBoard15VLow(0, "15V control board low"),

	/** Static GFCI high. */
	StaticGfciHigh(1, "Static GFCI high"),

	/** Arc board failure. */
	ArcBoard(2, "Arc board failure"),

	/** PV module configuration error. */
	PvModuleConfiguration(3, "PV module configuration error"),

	;

	private final int code;
	private final String description;

	private PVITLFault4(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return code;
	}

	@Override
	public int getGroupIndex() {
		return 5;
	}

	@Override
	public String getDescription() {
		return description;
	}

}

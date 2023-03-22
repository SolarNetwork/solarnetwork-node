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

import net.solarnetwork.domain.Bitmaskable;

/**
 * Permanent fault enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum PVITLWarning implements Bitmaskable {

	/** External fan error. */
	ExternalFan(0, "External fan error"),

	/** Internal fan error. */
	InternalFan(1, "Internal fan error"),

	/** Internal communication failed. */
	InternalComms(2, "Internal communication failed"),

	/** DSP EEPROM fault. */
	DspEeprom(3, "DSP EEPROM fault"),

	/** Temperature sensor fault. */
	TemperatureSensor(6, "Temperature sensor fault"),

	/** LCD EEPROM fault. */
	LcdEeprom(9, "LCD EEPROM fault"),

	;

	private final int code;
	private final String description;

	private PVITLWarning(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return code;
	}

	/**
	 * Get the description.
	 * 
	 * @return the description.
	 */
	public String getDescription() {
		return description;
	}

}

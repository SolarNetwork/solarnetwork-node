/* ==================================================================
 * UsbGpioCommand.java - 24/09/2021 2:07:53 PM
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

package net.solarnetwork.node.control.numato.usbgpio;

/**
 * A USB GPIO serial command verb.
 * 
 * @author matt
 * @version 1.0
 */
public enum UsbGpioCommand {

	/** Get the version firmware. */
	Version("ver"),

	/** Get the user-configurable 8-character ID. */
	IdGet("id get"),

	/** Set the user-configurable 8-character ID. */
	IdSet("id set"),

	/** Clear a GPIO. */
	GpioClear("gpio clear"),

	/** Clear a GPIO. */
	GpioRead("gpio read"),

	/** Clear a GPIO. */
	GpioSet("gpio set"),

	/**
	 * Set the GPIO mask for future {@literal writeall} or {@code iodir}
	 * commands.
	 */
	GpioIoMask("gpio iomask"),

	/**
	 * Set all GPIO directions as input (1) or output (0) via hex-encoded
	 * bitmask.
	 */
	GpioIoDirection("gpio iodir"),

	/**
	 * Read all GPIO values as a hex-encoded bitmask (the iodir command must be
	 * sent prior).
	 */
	GpioReadAll("gpio readall"),

	/**
	 * Write all GPIO values as a hex-encoded bitmask (the iodir command must be
	 * sent prior).
	 */
	GpioWriteAll("gpio writeall"),

	/** Read analog input value as value in the range {@literal 0 - 1023}. */
	AdcRead("adc read"),

	;

	private final String command;

	private UsbGpioCommand(String command) {
		this.command = command;
	}

	/**
	 * Get the command text value.
	 * 
	 * @return the command text value
	 */
	public String getCommand() {
		return command;
	}

}

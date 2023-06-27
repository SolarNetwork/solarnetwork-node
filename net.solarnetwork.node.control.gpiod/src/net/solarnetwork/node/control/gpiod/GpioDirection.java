/* ==================================================================
 * GpioDirection.java - 25/09/2021 2:56:00 PM
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

package net.solarnetwork.node.control.gpiod;

import net.solarnetwork.domain.CodedValue;

/**
 * GPIO direction enumeration.
 * 
 * @author matt
 * @version 1.0
 */
public enum GpioDirection implements CodedValue {

	/** Input (read) direction. */
	Input('i'),

	/** Output (write) direction. */
	Output('o');

	private final int code;

	private GpioDirection(char code) {
		this.code = code;
	}

	@Override
	public int getCode() {
		return code;
	}

}

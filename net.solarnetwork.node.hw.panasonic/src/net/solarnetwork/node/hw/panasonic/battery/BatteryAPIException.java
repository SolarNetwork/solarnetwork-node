/* ==================================================================
 * BatteryAPIException.java - 16/02/2016 7:28:59 am
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.panasonic.battery;

import net.solarnetwork.node.RemoteServiceException;

/**
 * Exception for Battery API client errors.
 * 
 * @author matt
 * @version 1.0
 */
public class BatteryAPIException extends RemoteServiceException {

	private static final long serialVersionUID = 8452520411500677197L;

	private int code;

	/**
	 * Construct with a code, message, and nested exception.
	 * 
	 * @param code
	 *        The code returned by the server.
	 * @param message
	 *        An optional message.
	 * @param cause
	 *        An optional nested exception.
	 */
	public BatteryAPIException(int code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * Construct with a code and message.
	 * 
	 * @param code
	 *        The code returned by the server.
	 * @param message
	 *        An optional message.
	 */
	public BatteryAPIException(int code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * Construct with a nested exception.
	 * 
	 * @param cause
	 *        An optional nested exception.
	 */
	public BatteryAPIException(Throwable cause) {
		super(cause);
	}

	/**
	 * Get the code returned by the server.
	 * 
	 * @return The code.
	 */
	public int getCode() {
		return code;
	}

}

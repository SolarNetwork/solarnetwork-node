/* ==================================================================
 * OCPPException.java - 10/06/2015 7:09:18 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp;

import ocpp.v15.AuthorizationStatus;

/**
 * Generic exception for OCPP specific conditions.
 * 
 * @author matt
 * @version 1.0
 */
public class OCPPException extends RuntimeException {

	private static final long serialVersionUID = 6185440591151893140L;

	private final AuthorizationStatus status;

	/**
	 * Construct with a message.
	 * 
	 * @param message
	 *        The message.
	 */
	public OCPPException(String message) {
		this(message, null, null);
	}

	/**
	 * Construct with cause.
	 * 
	 * @param cause
	 *        The original cause.
	 */
	public OCPPException(Throwable cause) {
		this(null, cause, null);
	}

	/**
	 * Construct with message and cause.
	 * 
	 * @param message
	 *        The message.
	 * @param cause
	 *        The original cause.
	 */
	public OCPPException(String message, Throwable cause) {
		this(message, cause, null);
	}

	/**
	 * Construct with values.
	 * 
	 * @param message
	 *        The message.
	 * @param cause
	 *        The original cause.
	 * @param status
	 *        A status.
	 */
	public OCPPException(String message, Throwable cause, AuthorizationStatus status) {
		super(message, cause);
		this.status = status;
	}

	/**
	 * Get a status associated with the exception condition.
	 * 
	 * @return The status.
	 */
	public AuthorizationStatus getStatus() {
		return status;
	}

}

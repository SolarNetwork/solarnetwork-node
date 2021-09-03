/* ==================================================================
 * RemoteServiceException.java - Oct 6, 2014 1:18:02 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service;

/**
 * Exception thrown when interacting with a remote service.
 * 
 * @author matt
 * @version 1.0
 */
public class RemoteServiceException extends RuntimeException {

	private static final long serialVersionUID = 6050177744319149194L;

	/**
	 * Construct with a message.
	 * 
	 * @param message
	 *        the message
	 */
	public RemoteServiceException(String message) {
		super(message);
	}

	/**
	 * Construct with a nested exception.
	 * 
	 * @param cause
	 *        the cause
	 */
	public RemoteServiceException(Throwable cause) {
		super(cause);
	}

	/**
	 * Construct with a message and nested exception.
	 * 
	 * @param message
	 *        the message
	 * @param cause
	 *        the cause
	 */
	public RemoteServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}

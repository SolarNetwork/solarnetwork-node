/* ==================================================================
 * InvalidVerificationCodeException.java - Sep 13, 2011 9:43:16 AM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.setup;

/**
 * Exception used to identify invalid verification codes, generally thrown when
 * an exception is encountered trying to decode a verification code.
 * 
 * @author maxieduncan
 * @version 1.0
 */
public class InvalidVerificationCodeException extends Exception {

	private static final long serialVersionUID = -3412491490707016756L;

	/**
	 * Construct with a message.
	 * 
	 * @param message
	 *        the message
	 */
	public InvalidVerificationCodeException(String message) {
		super(message);
	}

	/**
	 * Construct with a message and exception.
	 * 
	 * @param message
	 *        message
	 * @param t
	 *        the original exception
	 */
	public InvalidVerificationCodeException(String message, Throwable t) {
		super(message, t);
	}

}

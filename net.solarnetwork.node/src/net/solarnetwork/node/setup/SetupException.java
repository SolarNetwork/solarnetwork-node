/* ==================================================================
 * SetupException.java - Nov 28, 2012 5:27:13 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup;

/**
 * General setup runtime exception.
 * 
 * @author matt
 * @version 1.0
 */
public class SetupException extends RuntimeException {

	private static final long serialVersionUID = 547923961008965723L;

	/**
	 * Construct with a message.
	 * 
	 * @param message
	 *        the message
	 */
	public SetupException(String message) {
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
	public SetupException(String message, Throwable cause) {
		super(message, cause);
	}

}

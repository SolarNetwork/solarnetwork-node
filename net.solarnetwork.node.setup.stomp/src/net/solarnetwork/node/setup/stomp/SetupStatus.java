/* ==================================================================
 * SetupStatus.java - 19/08/2021 11:18:18 AM
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

package net.solarnetwork.node.setup.stomp;

/**
 * Standard setup status codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum SetupStatus {

	Ok(200),

	Accepted(202),

	NotFound(404),

	Unprocessable(422),

	InternalError(500),

	;

	private final int code;

	private SetupStatus(int code) {
		this.code = code;
	}

	/**
	 * Get the code value.
	 * 
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get an enum value from a code value.
	 * 
	 * @param code
	 *        the code to get an enum for
	 * @return the num
	 * @throws IllegalArgumentException
	 *         if {@literal code} is not supported
	 */
	public SetupStatus forCode(int code) {
		for ( SetupStatus s : SetupStatus.values() ) {
			if ( code == s.code ) {
				return s;
			}
		}
		throw new IllegalArgumentException("Unknown code: " + code);
	}

}

/* ==================================================================
 * StompHeader.java - 17/08/2021 4:25:28 PM
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
 * Common STOMP headers.
 * 
 * @author matt
 * @version 1.0
 */
public enum StompHeader {

	AcceptVersion("accept-version"),

	ContentLength("content-length"),

	ContentType("content-type"),

	Destination,

	Host,

	Id,

	Login,

	Message,

	;

	private final String value;

	private StompHeader() {
		this.value = this.name().toLowerCase();
	}

	private StompHeader(String value) {
		this.value = value;
	}

	/**
	 * Get the header value.
	 * 
	 * @return the value, never {@literal null}
	 */
	public String getValue() {
		return value;
	}

}

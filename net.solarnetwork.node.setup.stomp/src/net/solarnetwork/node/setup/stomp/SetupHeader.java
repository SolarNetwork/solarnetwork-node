/* ==================================================================
 * SetupHeader.java - 16/08/2021 8:44:24 AM
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
 * SolarNode Setup specific STOMP header names.
 * 
 * @author matt
 * @version 1.0
 */
public enum SetupHeader {

	Authenticate("Server request for client to authenticate, similar to HTTP WWW-Authenticate header."),

	Authorization("Client authorization, in HTTP Authorization header format."),

	AuthHash("auth-hash", "The digest algorithm to use when authenticating."),

	/**
	 * The message date.
	 * 
	 * @see net.solarnetwork.security.AuthorizationUtils#AUTHORIZATION_DATE_HEADER_FORMATTER
	 */
	Date("The message date, in HTTP Date header format, similar to similar to RFC 1123."),

	RequestId("request-id", "A unique ID included with a send/receive frame pair."),

	Status("A status code, like HTTP status values."),

	;

	private final String value;
	private final String description;

	private SetupHeader(String description) {
		this.value = this.name().toLowerCase();
		this.description = description;
	}

	private SetupHeader(String value, String description) {
		this.value = value;
		this.description = description;
	}

	/**
	 * Get the header value.
	 * 
	 * @return the value, never {@literal null}
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Get the description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get an enum instance for a value.
	 * 
	 * @param value
	 *        the value to get the enum instance for
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	public static SetupHeader forValue(String value) {
		if ( value != null ) {
			for ( SetupHeader h : SetupHeader.values() ) {
				if ( h.value.equals(value) ) {
					return h;
				}
			}
		}
		throw new IllegalArgumentException("Invalid SetupHeader value: " + value);
	}

}

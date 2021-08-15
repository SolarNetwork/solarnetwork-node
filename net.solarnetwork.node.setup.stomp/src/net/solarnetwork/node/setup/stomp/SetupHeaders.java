/* ==================================================================
 * SetupHeaders.java - 16/08/2021 8:44:24 AM
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
 * Setup STOMP header names.
 * 
 * @author matt
 * @version 1.0
 */
public enum SetupHeaders {

	Authenticate("Server request for client to authenticate, similar to HTTP WWW-Authenticate header."),

	Authorization("Client authorization, in HTTP Authorization header format."),

	/**
	 * The message date.
	 * 
	 * @see net.solarnetwork.security.AuthorizationUtils#AUTHORIZATION_DATE_HEADER_FORMATTER
	 */
	Date("The message date, in HTTP Date header format, similar to similar to RFC 1123."),

	;

	private final String value;
	private final String description;

	private SetupHeaders(String description) {
		this.value = this.name().toLowerCase();
		this.description = description;
	}

	private SetupHeaders(String value, String description) {
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

}

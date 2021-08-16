/* ==================================================================
 * StompUtils.java - 16/08/2021 7:50:52 PM
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
 * Utilities for STOMP.
 * 
 * @author matt
 * @version 1.0
 */
public final class StompUtils {

	private StompUtils() {
		// can't construct me
	}

	/**
	 * Encode a STOMP header value.
	 * 
	 * @param v
	 *        the header value
	 * @return the encoded header value
	 */
	public static String encodeStompHeaderValue(String v) {
		if ( v == null || v.isEmpty() ) {
			return v;
		}
		// @formatter:off
	    return v.replaceAll("\\\\", "\\\\\\\\")
	        .replaceAll("\r", "\\\\r")
	        .replaceAll("\n", "\\\\n")
	        .replaceAll(":", "\\\\c");
	    // @formatter:on
	}

	/**
	 * Decode a STOMP header value.
	 * 
	 * @param v
	 *        the header value
	 * @return the decoded header value
	 */
	public static String decodeStompHeaderValue(String v) {
		if ( v == null || v.isEmpty() ) {
			return v;
		}
		// @formatter:off
	    return v.replaceAll("\\\\\\\\", "\\\\")
	        .replaceAll("\\\\r", "\r")
	        .replaceAll("\\\\n", "\n")
	        .replaceAll("\\\\c", ":");
	    // @formatter:on
	}

}

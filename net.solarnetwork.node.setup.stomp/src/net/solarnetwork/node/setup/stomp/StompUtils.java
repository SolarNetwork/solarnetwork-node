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

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for STOMP.
 * 
 * @author matt
 * @version 1.0
 */
public final class StompUtils {

	/** The UTF-8 charset. */
	public static final Charset UTF8 = Charset.forName("UTF-8");

	/** The JSON content type with UTF-8 encoding. */
	public static final String JSON_UTF8_CONTENT_TYPE = "application/json;charset=utf-8";

	/**
	 * Pattern that matches characters that are reserved in STOMP headers and
	 * must be backlash-escaped.
	 */
	public static final Pattern HEADER_RESERVED_REGEX = Pattern
			.compile("(" + Pattern.quote("\\") + "|\r|\n|:)");

	/**
	 * Pattern that matches characters that are backslash-escaped in STOMP
	 * headers and must be un-escaped.
	 */
	public static final Pattern HEADER_ESCAPED_REGEX = Pattern
			.compile("(" + Pattern.quote("\\") + "[" + Pattern.quote("\\") + "rnc])");

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
		Matcher m = HEADER_RESERVED_REGEX.matcher(v);
		if ( !m.find() ) {
			return v;
		}
		m.reset();
		int idx = 0;
		StringBuilder buf = new StringBuilder();
		while ( m.find() ) {
			buf.append(v.substring(idx, m.start()));
			buf.append('\\');
			char reserved = m.group(1).charAt(0);
			switch (reserved) {
				case '\\':
					buf.append('\\');
					break;
				case '\r':
					buf.append('r');
					break;
				case '\n':
					buf.append('n');
					break;
				case ':':
					buf.append('c');
					break;
				default:
					// should never be here
			}
			idx = m.end();
		}
		if ( idx < v.length() ) {
			buf.append(v.substring(idx));
		}
		return buf.toString();
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
		Matcher m = HEADER_ESCAPED_REGEX.matcher(v);
		if ( !m.find() ) {
			return v;
		}
		m.reset();
		int idx = 0;
		StringBuilder buf = new StringBuilder();
		while ( m.find() ) {
			buf.append(v.substring(idx, m.start()));
			char reserved = m.group(1).charAt(1);
			switch (reserved) {
				case '\\':
					buf.append('\\');
					break;
				case 'r':
					buf.append('\r');
					break;
				case 'n':
					buf.append('\n');
					break;
				case 'c':
					buf.append(':');
					break;
				default:
					// should never be here
			}
			idx = m.end();
		}
		if ( idx < v.length() ) {
			buf.append(v.substring(idx));
		}
		return buf.toString();
	}

}

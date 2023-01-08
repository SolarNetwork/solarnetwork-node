/* ==================================================================
 * BacnetUtils.java - 8/11/2022 9:25:13 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.bacnet;

/**
 * Utilities for BACnet.
 * 
 * @author matt
 * @version 1.0
 */
public final class BacnetUtils {

	private BacnetUtils() {
		// not available
	}

	/**
	 * Convert a CamelCaseString into a kebab-case-string.
	 * 
	 * @param value
	 *        the value to convert
	 * @return the converted value
	 */
	public static String camelToKebabCase(String value) {
		if ( value == null || value.isEmpty() ) {
			return value;
		}
		StringBuilder buf = new StringBuilder();
		for ( int i = 0, len = value.length(); i < len; i++ ) {
			char c = value.charAt(i);
			if ( Character.isUpperCase(c) ) {
				if ( i > 0 ) {
					buf.append('-');
				}
				buf.append(Character.toLowerCase(c));
			} else {
				buf.append(c);
			}

		}
		return buf.toString();
	}

	/**
	 * Convert a kebab-case-string into a CamelCaseString.
	 * 
	 * @param value
	 *        the value to convert
	 * @return the converted value
	 */
	public static String kebabToCamelCase(String value) {
		if ( value == null || value.isEmpty() ) {
			return value;
		}
		final int len = value.length();
		int idx = value.indexOf('-');
		if ( idx < 0 ) {
			char c = value.charAt(0);
			if ( Character.isUpperCase(c) ) {
				return value;
			}
			StringBuilder buf = new StringBuilder(len);
			buf.append(Character.toUpperCase(c));
			if ( len > 1 ) {
				buf.append(value.substring(1));
			}
			return buf.toString();
		}
		StringBuilder buf = new StringBuilder(len);
		int prev = 0;
		do {
			if ( prev < len && idx > 0 ) {
				buf.append(Character.toUpperCase(value.charAt(prev)));
				prev++;
			}
			if ( prev < idx ) {
				buf.append(value.substring(prev, idx));
				prev = idx + 1;
			} else if ( idx == prev ) {
				prev++;
			}
			if ( prev < len ) {
				idx = value.indexOf('-', prev);
				if ( idx < 0 ) {
					idx = len;
				}
			}
		} while ( prev < len );
		return buf.toString();
	}

}

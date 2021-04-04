/* ==================================================================
 * CozIrUtils.java - 27/08/2020 4:48:32 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.gss.co2;

import static net.solarnetwork.node.hw.gss.co2.CozIrMessageType.SerialNumber;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utilities for working with CozIR sensors.
 * 
 * @author matt
 * @version 1.0
 */
public final class CozIrUtils {

	private CozIrUtils() {
		// don't construct me
	}

	/**
	 * Parse a line of key/value pairs where keys are strings and values are
	 * integers, all separated by whitespace.
	 * 
	 * <p>
	 * For example, an input line might look like this:
	 * </p>
	 * 
	 * <pre>
	 * <code>H 00408 T 01218 Z 01294 z 01297</code>
	 * </pre>
	 * 
	 * <p>
	 * which would return a Map with 4 key/value pairs.
	 * </p>
	 * 
	 * @param line
	 *        the line to parse
	 * @param radix
	 *        the radix to parse the integers as
	 * @return the parsed map, never {@literal null}
	 */
	public static Map<String, Integer> parseKeyValueIntegerLine(String line, int radix) {
		if ( line == null || line.isEmpty() ) {
			return Collections.emptyMap();
		}
		Map<String, Integer> data = new LinkedHashMap<>(4);
		String[] components = line.trim().split("\\s+");
		if ( components.length > 1 ) {
			// iterate to length - 1 to omit any trailing (non-pair) value
			for ( int i = 0, len = components.length - 1; i < len; i += 2 ) {
				String k = components[i];
				try {
					Integer n = Integer.parseInt(components[i + 1], radix);
					data.put(k, n);
				} catch ( NumberFormatException e ) {
					// silently ignore this bad boy
				}
			}
		}
		return data;
	}

	/**
	 * Parse a serial number message.
	 * 
	 * <p>
	 * Messages take this form: <code>B 412755 00000</code>
	 * </p>
	 * 
	 * @param message
	 *        the message to parse
	 * @return the serial number, or {@literal null} if one cannot be determined
	 */
	public static String parseSerialNumberMessage(String message) {
		if ( message == null || message.isEmpty() ) {
			return null;
		}
		String[] components = message.trim().split(" ");
		if ( !(components.length == 3 && SerialNumber.getKey().equals(components[0])) ) {
			return null;
		}
		return String.format("%d.%d", Integer.parseInt(components[1]), Integer.parseInt(components[2]));
	}

	private static final BigDecimal FEET_PER_METER = new BigDecimal("3.28084");
	private static final BigDecimal ALT_COMP_OFFSET = new BigDecimal("8192");
	private static final BigDecimal ALT_COMP_ERR = new BigDecimal("0.14");
	private static final BigDecimal ALT_COMP_POLY1 = new BigDecimal("-4.818e-7");
	private static final BigDecimal ALT_COMP_POLY2 = new BigDecimal("0.0364");
	private static final BigDecimal ALT_COMP_POLY3 = new BigDecimal("0.0324");

	/**
	 * Calculate an altitude compensation value from a given altitude.
	 * 
	 * @param altitude
	 *        the altitude, in feet
	 * @return the compensation value to use
	 */
	public static int altitudeCompensationValueForAltitudeInFeet(BigDecimal altitude) {

		// @formatter:off
		BigDecimal sealLevelDifference = ALT_COMP_POLY1
				.multiply(altitude.multiply(altitude))
				.add(ALT_COMP_POLY2.multiply(altitude))
				.add(ALT_COMP_POLY3);
		// @formatter:on

		return ALT_COMP_OFFSET.add(
				sealLevelDifference.multiply(ALT_COMP_ERR).movePointLeft(2).multiply(ALT_COMP_OFFSET))
				.setScale(0, RoundingMode.HALF_UP).intValue();
	}

	/**
	 * Calculate an altitude compensation value from a given altitude.
	 * 
	 * @param altitude
	 *        the altitude, in feet
	 * @return the compensation value to use
	 */
	public static int altitudeCompensationValueForAltitudeInFeet(int altitude) {
		return altitudeCompensationValueForAltitudeInFeet(new BigDecimal(altitude));
	}

	/**
	 * Calculate an altitude compensation value from a given altitude.
	 * 
	 * @param altitude
	 *        the altitude, in meters
	 * @return the compensation value to use
	 */
	public static int altitudeCompensationValueForAltitudeInMeters(BigDecimal altitude) {
		return altitudeCompensationValueForAltitudeInFeet(altitude.multiply(FEET_PER_METER));
	}

	/**
	 * Calculate an altitude compensation value from a given altitude.
	 * 
	 * @param altitude
	 *        the altitude, in meters
	 * @return the compensation value to use
	 */
	public static int altitudeCompensationValueForAltitudeInMeters(int altitude) {
		return altitudeCompensationValueForAltitudeInMeters(new BigDecimal(altitude));
	}

}

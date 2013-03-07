/* ==================================================================
 * DataUtils.java - Apr 25, 2010 12:38:53 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.util;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for dealing with raw data.
 * 
 * @author matt
 * @version 1.0
 */
public final class DataUtils {

	private static final Logger LOG = LoggerFactory.getLogger(DataUtils.class);

	private DataUtils() {
		// do not construct me
	}

	/**
	 * Convert a signed byte into an unsigned short value.
	 * 
	 * <p>
	 * The returned short will have a value between 0 and 255.
	 * </p>
	 * 
	 * @param data
	 *        the byte
	 * @return the unsigned value
	 */
	public static short unsigned(byte data) {
		return (short) (data & 0xFF);
	}

	/**
	 * Convert signed bytes into unsigned short values.
	 * 
	 * <p>
	 * The returned shorts will have values between 0 and 255.
	 * </p>
	 * 
	 * @param data
	 *        the bytes
	 * @return the unsigned values
	 */
	public static short[] getUnsignedValues(byte[] data) {
		// convert bytes into "unsigned" integer values, i.e. 0..255
		short[] unsigned = new short[data.length];
		for ( int i = 0; i < data.length; i++ ) {
			unsigned[i] = (short) (data[i] & 0xFF);
		}
		if ( LOG.isTraceEnabled() ) {
			LOG.trace("Unsigned data: " + Arrays.toString(unsigned));
		}
		return unsigned;
	}

}

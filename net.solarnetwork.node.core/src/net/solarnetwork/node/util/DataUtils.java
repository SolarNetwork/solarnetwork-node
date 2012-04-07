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
 * $Revision$
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
 * @version $Revision$
 */
public final class DataUtils {

	private static final Logger LOG = LoggerFactory.getLogger(DataUtils.class);
	
	private DataUtils() {
		// do not construct me
	}
	
	/**
	 * Convert signed bytes into unsigned short values.
	 * 
	 * <p>This is used 
	 * 
	 * @param data
	 * @return
	 */
	public static short[] getUnsignedValues(byte[] data) {
		// convert bytes into "unsigned" integer values, i.e. 0..255
		short[] unsigned = new short[data.length];
		for ( int i = 0; i < data.length; i++ ) {
			unsigned[i] = (short)(data[i] & 0xFF);
		}
		if ( LOG.isTraceEnabled() ) {
			LOG.trace("Unsigned data: " +Arrays.toString(unsigned));
		}
		return unsigned;
	}

}

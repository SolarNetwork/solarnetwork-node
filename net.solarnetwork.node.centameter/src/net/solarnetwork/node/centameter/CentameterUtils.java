/* ==================================================================
 * CentameterUtils.java - Apr 25, 2010 12:34:34 PM
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

package net.solarnetwork.node.centameter;


/**
 * Utility methods for Centameter support.
 * 
 * @author matt
 * @version $Revision$
 */
public final class CentameterUtils {
	
	private CentameterUtils() {
		// do not construct me
	}
	
	/**
	 * Extract the amp value for a specific Centameter sensor.
	 * 
	 * @param unsigned array of unsigned data read from Centameter.
	 * @param ampIndex the Centameter sensor to read, either 1, 2, or 3
	 * @return
	 */
	public static double getAmpReading(short[] unsigned, int ampIndex) {
		switch ( ampIndex ) {
			case 1:
				return (unsigned[4] 
				     	+ ((unsigned[5] &  0x3) * 256)) / 10.0;
				
			case 2:
				return (((unsigned[5] >> 2) & 0x3F) 
						+ ((unsigned[6] &  0xF) * 64)) / 10.0;
				
			case 3:
				return (((unsigned[6] >> 4) & 0xF) 
						+ ((unsigned[7] &  0x3F) * 16)) / 10.0;
		}
		return 0;
	}

}

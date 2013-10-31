/* ==================================================================
 * SmaDataFrame.java - Oct 31, 2013 4:49:01 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.protocol;

/**
 * API for SMA data frame protocol implementations.
 * 
 * @author matt
 * @version 1.0
 */
public interface SmaDataFrame {

	/**
	 * Get the frame data, as a raw byte array.
	 * 
	 * @return the frame data
	 */
	byte[] getFrame();

	/**
	 * Test if the packet appears to be valid, e.g. from CRC checking.
	 * 
	 * @return boolean
	 */
	boolean isValid();

	/**
	 * Get the {@link SmaPacket} encoded in this frame.
	 * 
	 * @return the packet
	 */
	SmaPacket getPacket();

}

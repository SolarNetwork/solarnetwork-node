/* ==================================================================
 * CanbusFrame.java - 23/09/2019 8:55:08 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.canbus;

/**
 * General API for a CAN bus message, or frame.
 * 
 * @author matt
 * @version 1.0
 */
public interface CanbusFrame {

	/** The maximum value a "standard" message address can have (11 bits). */
	int MAX_STANDARD_ADDRESS = 0x7FF;

	/** The maximum value an "extended" message address can have (29 bits). */
	int MAX_EXTENDED_ADDRESS = 0x1FFFFFFF;

	/**
	 * Get the address.
	 * 
	 * @return the address
	 */
	int getAddress();

	/**
	 * Test if this the address represents a CAN 2.0B "extended" address or not.
	 * 
	 * @return {@literal true} if the address is a CAN 2.0B 29-bit address,
	 *         otherwise a CAN 2.0A 11-bit address is assumed
	 */
	default boolean isExtendedFrame() {
		return (getAddress() > MAX_STANDARD_ADDRESS);
	}

	/**
	 * Get the count of data bytes.
	 * 
	 * @return the count of data bytes
	 */
	int getDataLength();

	/**
	 * Get the data.
	 * 
	 * @return the data, never {@literal null}
	 */
	byte[] getData();

}

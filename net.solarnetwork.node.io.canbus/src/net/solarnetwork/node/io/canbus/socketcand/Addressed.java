/* ==================================================================
 * Addressed.java - 20/09/2019 7:27:29 am
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

package net.solarnetwork.node.io.canbus.socketcand;

import net.solarnetwork.node.io.canbus.CanbusFrame;

/**
 * API for something that has an address within the socketcand bus.
 * 
 * @author matt
 * @version 1.0
 */
public interface Addressed {

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
	default boolean isExtendedAddress() {
		return (getAddress() > CanbusFrame.MAX_STANDARD_ADDRESS);
	}

	/**
	 * Get a hex-formatted address value.
	 * 
	 * @param address
	 *        the address
	 * @param forceExtendedAddress
	 *        {@literal true} if the address should always be treated as an
	 *        extended address
	 * @return the hex-formatted address
	 */
	static String hexAddress(int address, boolean forceExtendedAddress) {
		final boolean extended = forceExtendedAddress || address > CanbusFrame.MAX_STANDARD_ADDRESS;
		return String.format(extended ? "%08X" : "%X", address);
	}

}

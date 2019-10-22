/* ==================================================================
 * CanbusSignalReference.java - 9/10/2019 10:59:02 am
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

import net.solarnetwork.domain.BitDataType;
import net.solarnetwork.domain.ByteOrdering;

/**
 * A reference to a CAN bus frame message signal.
 * 
 * <p>
 * A <i>signal</i> is some portion of a full CAN bus <i>frame message</i> value.
 * Some message frames include multiple signals.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface CanbusSignalReference {

	/**
	 * Get the message address.
	 * 
	 * @return the address
	 */
	int getAddress();

	/**
	 * Get the data type.
	 * 
	 * @return the data type
	 */
	BitDataType getDataType();

	/**
	 * Get the byte ordering.
	 * 
	 * @return the byte ordering
	 */
	ByteOrdering getByteOrdering();

	/**
	 * Get the bit offset within the frame message.
	 * 
	 * @return the bit offset
	 */
	int getBitOffset();

	/**
	 * Get the number of bits to include.
	 * 
	 * @return the bit length
	 */
	int getBitLength();

}

/* ==================================================================
 * SimpleCanbusSignalReference.java - 9/10/2019 11:42:24 am
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

package net.solarnetwork.node.io.canbus.support;

import net.solarnetwork.domain.BitDataType;
import net.solarnetwork.domain.ByteOrdering;
import net.solarnetwork.node.io.canbus.CanbusSignalReference;

/**
 * Simple immutable implementation of {@link CanbusSignalReference}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleCanbusSignalReference implements CanbusSignalReference {

	private final int address;
	private final BitDataType dataType;
	private final ByteOrdering byteOrdering;
	private final int bitOffset;
	private final int bitLength;

	/**
	 * Constructor.
	 * 
	 * @param address
	 *        the address
	 * @param dataType
	 *        the data type
	 * @param byteOrdering
	 *        the byte ordering
	 * @param bitOffset
	 *        the bit offset
	 * @param bitLength
	 *        the bit length
	 */
	public SimpleCanbusSignalReference(int address, BitDataType dataType, ByteOrdering byteOrdering,
			int bitOffset, int bitLength) {
		super();
		this.address = address;
		this.dataType = dataType;
		this.byteOrdering = byteOrdering;
		this.bitOffset = bitOffset;
		this.bitLength = bitLength;
	}

	@Override
	public int getAddress() {
		return address;
	}

	@Override
	public BitDataType getDataType() {
		return dataType;
	}

	@Override
	public ByteOrdering getByteOrdering() {
		return byteOrdering;
	}

	@Override
	public int getBitOffset() {
		return bitOffset;
	}

	@Override
	public int getBitLength() {
		return bitLength;
	}

}

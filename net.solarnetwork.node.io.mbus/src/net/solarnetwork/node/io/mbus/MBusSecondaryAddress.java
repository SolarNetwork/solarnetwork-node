/* ==================================================================
 * MBusSecondaryAddress.java - 29/06/2020 12:02:19 PM
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

package net.solarnetwork.node.io.mbus;

import java.util.Arrays;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * 
 * An object that represents an MBus Secondary Address
 * 
 * @author alex
 * @version 1.1
 */
public class MBusSecondaryAddress {

	final private byte[] INVALID_ADDRESS = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
	final private int SIZE = INVALID_ADDRESS.length;

	private byte[] address = INVALID_ADDRESS;

	/**
	 * Construct from a hex address.
	 * 
	 * @param hexAddress
	 *        A hex string representing 8 bytes
	 */
	public MBusSecondaryAddress(String hexAddress) {
		try {
			final byte[] bytes = Hex.decodeHex(hexAddress);
			if ( bytes.length == SIZE ) {
				address = bytes;
			}
		} catch ( DecoderException e ) {
		}
	}

	/**
	 * Construct from raw bytes.
	 * 
	 * @param bytes
	 *        the raw MBus secondary address data
	 */
	public MBusSecondaryAddress(byte[] bytes) {
		if ( bytes != null && bytes.length == SIZE ) {
			address = bytes;
		}
	}

	/**
	 * Get the raw bytes.
	 * 
	 * @return the bytes
	 */
	public byte[] getBytes() {
		return address;
	}

	/**
	 * Test if the address is valid.
	 * 
	 * @return {@literal true} if the address is not invalid
	 */
	public boolean isValid() {
		return !Arrays.equals(address, INVALID_ADDRESS);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(address);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof MBusSecondaryAddress) ) {
			return false;
		}
		MBusSecondaryAddress other = (MBusSecondaryAddress) obj;
		return Arrays.equals(address, other.address);
	}

	/**
	 * Return the hex-encoded address value.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return (address != null ? Hex.encodeHexString(address, false) : "");
	}

}

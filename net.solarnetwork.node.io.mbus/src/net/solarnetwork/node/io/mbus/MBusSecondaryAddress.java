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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * 
 * An object that represents an MBus Secondary Address
 * 
 * @author alex
 * @version 1.0
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
		if ( bytes.length == SIZE ) {
			address = bytes;
		}
	}

	public byte[] getBytes() {
		return address;
	}

	public boolean isValid() {
		return !address.equals(INVALID_ADDRESS);
	}

	@Override
	public boolean equals(Object o) {
		if ( o == this ) {
			return true;
		}

		if ( !(o instanceof MBusSecondaryAddress) ) {
			return false;
		}

		MBusSecondaryAddress mbsa = (MBusSecondaryAddress) o;

		return address.equals(mbsa.address);
	}
}

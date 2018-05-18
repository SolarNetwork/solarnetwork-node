/* ==================================================================
 * PacketHeader.java - 14/05/2018 9:24:30 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.yaskawa.ecb;

/**
 * The start of a packet, up to but not including any command, sub-command, or
 * packet data.
 * 
 * @author matt
 * @version 1.0
 */
public class PacketHeader {

	/** The number of bytes in a packet header. */
	public static final int BYTE_LENGTH = 4;

	private final byte[] data;
	private final int offset;

	/**
	 * Construct from packet header data.
	 * 
	 * @param data
	 *        the data
	 * @throws IllegalArgumentException
	 *         if the header data is not long enough for a packet header
	 */
	public PacketHeader(byte[] data) {
		this(data, 0);
	}

	/**
	 * Construct from packet header data.
	 * 
	 * @param data
	 *        the data
	 * @param offset
	 *        the offset within the data
	 * @throws IllegalArgumentException
	 *         if the header data is not long enough for a packet header
	 */
	public PacketHeader(byte[] data, int offset) {
		super();
		if ( data == null || data.length < offset + BYTE_LENGTH ) {
			throw new IllegalArgumentException("Not enough data available.");
		}
		this.data = data;
		this.offset = offset;
	}

	/**
	 * Get the packet header envelope.
	 * 
	 * <p>
	 * This should always return {@link PacketEnvelope#Start} for a valid packet
	 * header.
	 * </p>
	 * 
	 * @return the packet header envelope
	 * @throws IllegalArgumentException
	 *         if no envelope is available
	 */
	public PacketEnvelope getEnvelope() {
		return PacketEnvelope.forCode(data[offset]);
	}

	/**
	 * Get the packet header type.
	 * 
	 * @return the packet header type
	 * @throws IllegalArgumentException
	 *         if no type is available
	 */
	public PacketType getType() {
		return PacketType.forCode(data[offset + 1]);
	}

	/**
	 * Get the slave address.
	 * 
	 * @return the address of the slave the packet is associated with
	 */
	public short getAddress() {
		return (short) (data[offset + 2] & (short) 0xFF);
	}

	/**
	 * Get the number of data bytes included in the packet associated with this
	 * header.
	 * 
	 * @return the number of data bytes
	 */
	public int getDataLength() {
		return (data[offset + 3] & 0xFF);
	}

	/**
	 * Get a debug string for this header.
	 * 
	 * @return a hex-encoded string of the header bytes, delimited by a space
	 */
	public String toDebugString() {
		StringBuilder buf = new StringBuilder();
		for ( int i = offset; i < offset + BYTE_LENGTH && i < data.length; i++ ) {
			if ( i > offset ) {
				buf.append(' ');
			}
			buf.append(String.format("%02x", data[i]));
		}
		return buf.toString();
	}

	/**
	 * Test if the header data appears to be a properly encoded packet header.
	 * 
	 * @return {@literal true} if the header data represents a valid packet
	 *         header
	 */
	public boolean isValid() {
		try {
			return (data != null && (offset + 3) < data.length && getEnvelope() == PacketEnvelope.Start
					&& getType() != null);
		} catch ( IllegalArgumentException e ) {
			return false;
		}
	}

}

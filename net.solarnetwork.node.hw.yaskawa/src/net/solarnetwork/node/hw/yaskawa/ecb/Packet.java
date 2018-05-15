/* ==================================================================
 * Packet.java - 14/05/2018 9:22:32 AM
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
 * An ECB message packet.
 * 
 * @author matt
 * @version 1.0
 */
public class Packet {

	private final byte[] data;
	private final int offset;

	private final PacketHeader header;

	/**
	 * Construct from packet data.
	 * 
	 * @param data
	 *        the data
	 * @throws IllegalArgumentException
	 *         if the data is not long enough for a packet
	 */
	public Packet(byte[] data) {
		this(data, 0);
	}

	/**
	 * Construct from packet data.
	 * 
	 * @param data
	 *        the data
	 * @param offset
	 *        the offset within the data
	 * @throws IllegalArgumentException
	 *         if the data is not long enough for a packet
	 */
	public Packet(byte[] data, int offset) {
		super();
		this.data = data;
		this.offset = offset;
		this.header = new PacketHeader(data, offset);
	}

	/**
	 * Get the packet header.
	 * 
	 * @return the header
	 */
	public PacketHeader getHeader() {
		return header;
	}

	/**
	 * Get the CRC calculated from the data itself.
	 * 
	 * @return the CRC
	 */
	public int getCalculatedCrc() {
		int dataLength = this.header.getDataLength();
		return DataUtils.crc16(this.data, offset + 1, dataLength + 5);
	}

	/**
	 * Get the CRC encoded in the packet.
	 * 
	 * @return the encoded CRC
	 */
	public int getCrc() {
		int dataLength = this.header.getDataLength();
		int crcOffset = offset + 6 + dataLength;
		if ( this.data.length < crcOffset + 1 ) {
			return 0;
		}
		int result = ((this.data[crcOffset + 1] & 0xFF) << 8) | (this.data[crcOffset] & 0xFF);
		return result;
	}

	/**
	 * Test if the data appears to be a properly encoded packet.
	 * 
	 * @return {@literal true} if the data represents a valid packet
	 */
	public boolean isValid() {
		return (data != null && data.length > 8 && header != null && header.isValid()
				&& getCalculatedCrc() == getCrc());
	}
}

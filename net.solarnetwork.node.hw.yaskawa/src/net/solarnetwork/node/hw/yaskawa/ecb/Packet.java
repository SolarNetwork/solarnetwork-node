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

import java.util.Arrays;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

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
	 * Construct from hex-encoded packet data.
	 * 
	 * @param hexData
	 *        the hex-encoded data; whitespace is allowed (it will be removed)
	 * @throws DecoderException
	 *         if there is a problem decoding the hex data
	 */
	public Packet(String hexData) throws DecoderException {
		this(decodeHex(hexData));
	}

	@Override
	public String toString() {
		return "Packet{" + Arrays.toString(data) + "}";
	}

	private static byte[] decodeHex(String hexData) throws DecoderException {
		if ( hexData == null ) {
			return null;
		}
		return Hex.decodeHex(hexData.replaceAll("\\s+", "").toCharArray());
	}

	/**
	 * Construct a packet from components and a hex-encoded body.
	 * 
	 * @param address
	 *        the address to send the packet to
	 * @param cmd
	 *        the command
	 * @param subCommand
	 *        the sub-command
	 * @param hexBody
	 *        the body of the packet, as a Hex-encoded string (whitespace
	 *        allowed) or {@literal null} for no body
	 * @return the packet
	 * @throws DecoderException
	 *         if there is a problem decoding the {@code hexBody}
	 */
	public static Packet forCommand(int address, int cmd, int subCommand, String hexBody)
			throws DecoderException {
		return forCommand(address, cmd, subCommand, decodeHex(hexBody));
	}

	/**
	 * Construct a packet from components.
	 * 
	 * @param address
	 *        the address to send the packet to
	 * @param cmd
	 *        the command
	 * @param subCommand
	 *        the sub-command
	 * @param body
	 *        the body of the packet
	 * @return the packet
	 */
	public static Packet forCommand(int address, int cmd, int subCommand, byte[] body) {
		int bodyLen = 2 + (body != null ? body.length : 0);
		byte[] data = new byte[bodyLen + 7];
		data[0] = PacketEnvelope.Start.getCode();
		data[1] = PacketType.MasterLinkRequest.getCode();
		data[2] = (byte) (address & 0xFF);
		data[3] = (byte) (bodyLen & 0xFF);
		data[4] = (byte) (cmd & 0xFF);
		data[5] = (byte) (subCommand & 0xFF);
		if ( body != null ) {
			System.arraycopy(body, 0, data, 6, bodyLen - 2);
		}
		data[bodyLen + 6] = PacketEnvelope.End.getCode();
		Packet p = new Packet(data);
		p.setCrc();
		return p;
	}

	/**
	 * Construct a packet from data components.
	 * 
	 * @param header
	 *        the first 4 bytes of data, starting with
	 *        {@link PacketEnvelope#Start} and ending with the data length byte
	 * @param headerOffset
	 *        the offset within {@code header} to start at
	 * @param data
	 *        the remaining bytes of data for the entire packet, starting with
	 *        the command byte through to the {@link PacketEnvelope#End} byte
	 * @param dataOffset
	 *        the offset within {@code data} to start at
	 * @return the packet
	 */
	public static Packet forData(byte[] header, int headerOffset, byte[] data, int dataOffset) {
		if ( header == null || header.length < headerOffset + 4 ) {
			throw new IllegalArgumentException("Not enough data in header");
		}
		int dataLen = header[headerOffset + 3];
		if ( data == null || data.length < dataOffset + dataLen + 3 ) {
			throw new IllegalArgumentException("Not enough data");
		}
		byte[] packetData = new byte[dataLen + 7];
		System.arraycopy(header, headerOffset, packetData, 0, 4);
		System.arraycopy(data, dataOffset, packetData, 4, dataLen + 3);
		return new Packet(packetData);
	}

	/**
	 * Construct from header and body hex data.
	 * 
	 * @param headerHex
	 *        the header data, in hex
	 * @param dataHex
	 *        the rest of the packet data, in hex
	 * @return the packet
	 * @throws DecoderException
	 *         if there is a problem decoding the {@code headerHex} or
	 *         {@code dataHex}
	 */
	public static Packet forData(String headerHex, String dataHex) throws DecoderException {
		return new Packet(headerHex + dataHex);
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
	 * Get the command byte.
	 * 
	 * @return the command
	 */
	public byte getCommand() {
		return data[offset + 4];
	}

	/**
	 * Get the sub-command byte.
	 * 
	 * @return the sub-command
	 */
	public byte getSubCommand() {
		return data[offset + 5];
	}

	/**
	 * Get the body of the packet.
	 * 
	 * <p>
	 * This is the "data" portion of the packet, i.e. the packet without the
	 * header, command, sub-command, or envelope bytes.
	 * </p>
	 * 
	 * @return the body data
	 * @throws IllegalArgumentException
	 *         if there aren't enough actual data bytes to fulfill the
	 *         {@literal dataLength} encoded in the packet
	 */
	public byte[] getBody() {
		int dataLength = this.header.getDataLength();
		byte[] body = new byte[Math.max(dataLength - 2, 0)];
		if ( offset + PacketHeader.BYTE_LENGTH + dataLength > this.data.length ) {
			throw new IllegalArgumentException("Not enough data.");
		}
		if ( dataLength > 0 ) {
			System.arraycopy(this.data, offset + PacketHeader.BYTE_LENGTH + 2, body, 0, dataLength - 2);
		}
		return body;
	}

	/**
	 * Get the CRC calculated from the data itself.
	 * 
	 * @return the CRC
	 */
	public int getCalculatedCrc() {
		int dataLength = this.header.getDataLength();
		return DataUtils.crc16(this.data, offset + 1, dataLength + 3);
	}

	/**
	 * Get the CRC encoded in the packet.
	 * 
	 * @return the encoded CRC
	 */
	public int getCrc() {
		int dataLength = this.header.getDataLength();
		int crcOffset = offset + PacketHeader.BYTE_LENGTH + dataLength;
		if ( this.data.length < crcOffset + 1 ) {
			return 0;
		}
		int result = ((this.data[crcOffset + 1] & 0xFF) << 8) | (this.data[crcOffset] & 0xFF);
		return result;
	}

	/**
	 * Set the CRC bytes in the packet according to the calculated CRC from the
	 * packet data.
	 */
	private void setCrc() {
		int dataLength = this.header.getDataLength();
		int crcOffset = offset + PacketHeader.BYTE_LENGTH + dataLength;
		if ( this.data.length < crcOffset + 1 ) {
			return;
		}
		int calcCrc = getCalculatedCrc();
		this.data[crcOffset] = (byte) (calcCrc & 0xFF);
		this.data[crcOffset + 1] = (byte) ((calcCrc >> 8) & 0xFF);
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

	/**
	 * Get the entire packet encoded as bytes.
	 * 
	 * @return the packet bytes
	 */
	public byte[] getBytes() {
		return data;
	}

}

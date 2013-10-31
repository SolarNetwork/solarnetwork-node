/* ==================================================================
 * SmaSunnyNetFrame.java - Oct 31, 2013 4:34:33 PM
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
 * A communication frame for the {@link SmaPacket} packet, using the SunnyNet
 * protocol.
 * 
 * <p>
 * A SunnyNet frame is composed of a fixed-length header, a {@link SmaPacket},
 * and a fixed-length footer, in the following form:
 * </p>
 * 
 * <pre>
 *   # 2 optional sync bytes
 *   # 1 telegram start byte (0x68)
 *   # 1 user length byte
 *   # 1 user length byte repeated
 *   # 1 telegram start byte (0x68)
 *   ### the checksum data block starts here
 *   # SmaPacket; 7 or more bytes
 *   ### the checksum data block ends here
 *   # 2 checksum bytes
 *   # 1 end character byte (0x16)
 * </pre>
 * 
 * @author matt
 * @version 1.0
 */
public class SmaSunnyNetFrame implements SmaDataFrame {

	/** A "wakeup" packet byte. */
	public static final byte WAKEUP = (byte) 0xAA;

	/** A "telegram" packet byte. */
	public static final byte TELEGRAM = (byte) 0x68;

	/** An "end" packet byte. */
	public static final byte END = (byte) 0x16;

	private SmaPacket packet;
	private byte[] frame;
	private int computedCRC;

	/**
	 * Construct a SmaSunnyNetFrame from a SmaPacket.
	 * 
	 * @param packet
	 *        the packet
	 */
	public SmaSunnyNetFrame(SmaPacket packet) {
		super();
		this.packet = packet;
		encodeFrame(packet.getPacket());
	}

	/**
	 * Decode a SmaSunnyNetFrame from a byte array.
	 * 
	 * <p>
	 * After construction, the {@link #getFrame()} will return the raw bytes
	 * used to decode this frame object. You can thus tell by the length of that
	 * array where the next frame should begin if more data bytes are available
	 * in the {@code data} array.
	 * </p>
	 * 
	 * @param data
	 *        the data to decode
	 * @param offset
	 *        the offset within the data to start decoding at
	 */
	public SmaSunnyNetFrame(byte[] data, int offset) {
		super();
		decodeFrame(data, offset);
	}

	@Override
	public byte[] getFrame() {
		return frame;
	}

	@Override
	public boolean isValid() {
		int crc = getCRC();
		return (crc > 0 && computedCRC == crc && packet != null && packet.getCommand() != null && packet
				.getCommand() != SmaCommand.Unknown);
	}

	@Override
	public SmaPacket getPacket() {
		return packet;
	}

	/**
	 * Get the CRC as encoded in the frame data.
	 * 
	 * <p>
	 * The CRC is derived from the 3rd and 2nd to last bytes in the frame.
	 * </p>
	 * 
	 * @return the CRC
	 */
	public int getCRC() {
		if ( frame == null || frame.length < 9 ) {
			return -1;
		}
		return ((0xFF & frame[frame.length - 3]) | ((0xFF & frame[frame.length - 2]) << 8));
	}

	/**
	 * Get the CRC as computed from the raw packet data.
	 * 
	 * <p>
	 * When decoding a frame from bytes, this value can be compared to
	 * {@link #getCRC()} to tell if the packet is valid or not.
	 * </p>
	 * 
	 * @return the computed CRC
	 */
	public int getComputedCRC() {
		return computedCRC;
	}

	private void encodeFrame(byte[] packet) {
		byte[] req = new byte[6 + packet.length + 3];
		req[0] = WAKEUP; // wakeup
		req[1] = WAKEUP;
		req[2] = TELEGRAM; // telegram
		req[3] = (byte) packet.length; // length of data, twice
		req[4] = req[3];
		req[5] = TELEGRAM; // telegram
		System.arraycopy(packet, 0, req, 6, packet.length);

		computedCRC = 0;
		for ( int i = 6; i < packet.length; i++ ) {
			computedCRC += (0xFF & req[i]);
		}
		req[req.length - 3] = (byte) (computedCRC & 0xFF); // computedCrc low byte
		req[req.length - 2] = (byte) ((computedCRC >> 8) & 0xFF); // computedCrc high byte
		req[req.length - 1] = END;
		this.frame = req;
	}

	private void decodeFrame(byte[] data, int offset) {
		if ( data == null || data.length + offset < 7 ) {
			return;
		}

		int idx = offset;
		// skip (optional) WAKEUP bytes
		while ( data[idx] == WAKEUP ) {
			idx++;
		}

		// next is TELEGRAM, user data length, user data length, TELEGRAM
		idx++;
		int userDataLength = 0xFF & data[idx];
		idx += 3;

		byte[] packetData = new byte[7 + userDataLength];
		if ( idx + packetData.length + 1 > data.length ) {
			// not enough data for packet
			return;
		}
		System.arraycopy(data, idx, packetData, 0, packetData.length);

		// CRC data starts now
		computedCRC = 0;
		for ( int i = 0; i < packetData.length; i++, idx++ ) {
			int b = 0xFF & data[idx];
			computedCRC += b;
		}

		this.packet = new SmaPacket(packetData);

		// skip the 2 CRC bytes and the final 0x16 byte 
		idx += 3;

		byte[] req = new byte[idx];
		System.arraycopy(data, offset, req, 0, req.length);
		this.frame = req;
	}

}

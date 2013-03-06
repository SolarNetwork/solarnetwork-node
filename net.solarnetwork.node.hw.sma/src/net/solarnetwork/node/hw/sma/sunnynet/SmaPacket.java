
package net.solarnetwork.node.hw.sma.sunnynet;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMA communication packet.
 * 
 * <p>
 * This is the basic unit of communication for requests to and responses from
 * the SMA controller. The data is encoded into/decoded from a byte array (the
 * {@code packet} field) from/to other fields on this class.
 * </p>
 * 
 * <p>
 * The packet structure consists of a fixed-length header, a variable-length
 * {@code userData} section, and a fixed-length footer. The header contains
 * information like the length of the user data section, the source and
 * destination addresses, and the command type. The user data section contains
 * any data required to pass with the command, or sent back from the controller.
 * The footer contains a checksum value calculated from parts of the header and
 * the complete user data section.
 * </p>
 * 
 * <p>
 * The special {@code userDataFields} Map will contain data decoded from the
 * user data of response packets, such as the value returned by GetData
 * commands.
 * </p>
 * 
 * <p>
 * The SMA controller uses little endian encoding in all communication.
 * </p>
 * 
 * <pre>
 * # 2 optional sync bytes
 *   # 1 telegram start byte (0x68)
 *   # 1 user length byte
 *   # 1 user length byte repeated
 *   # 1 telegram start byte (0x68)
 *   ### the checksum data block starts here
 *   # 2 source address bytes
 *   # 2 destination address bytes
 *   # 1 control byte (0x40)
 *   # 1 packet counter byte
 *   # 1 command type byte
 *   # X user data bytes
 *   ### the checksum data block ends here
 *   # 2 checksum bytes
 *   # 1 end character byte (0x16)
 * </pre>
 */
public final class SmaPacket {

	/** A "wakeup" packet byte. */
	public static final byte WAKEUP = (byte) 0xAA;

	/** A "telegram" packet byte. */
	public static final byte TELEGRAM = (byte) 0x68;

	/** An "end" packet byte. */
	public static final byte END = (byte) 0x16;

	/** An empty byte array, useful for specifying empty user data. */
	public static final byte[] EMPTY_DATA = new byte[0];

	private static final Logger LOG = LoggerFactory.getLogger(SmaPacket.class);

	private byte[] packet;
	private byte[] userData = null;
	private int srcAddress = 0;
	private int destAddress = 0;
	private SmaControl control = null;
	private int packetCounter = 0;
	private SmaCommand command = null;
	private int crc = 0;
	private int computedCrc = 0;
	private Map<SmaUserDataField, Object> userDataFields = null;

	/**
	 * Construct a request packet.
	 * 
	 * <p>
	 * This constructor can be used for any type of packet, but generally will
	 * be used to construct a request packet to send to the controller.
	 * </p>
	 * 
	 * @param src
	 *        the source address (usually 0)
	 * @param dest
	 *        the destination address (use 0 for a RequestGroup control type)
	 * @param packetCounter
	 *        the packet counter (usually 0)
	 * @param control
	 *        the request type (usually RequestSingle or RequestGroup)
	 * @param command
	 *        the command to issue
	 * @param userData
	 *        the data to send with the command
	 */
	public SmaPacket(int src, int dest, int packetCounter, SmaControl control, SmaCommand command,
			byte[] userData) {
		this.srcAddress = src;
		this.destAddress = dest;
		this.control = control;
		this.command = command;
		this.userData = userData;
		this.packetCounter = packetCounter;
		encodePacket();
	}

	/**
	 * Construct a response packet.
	 * 
	 * <p>
	 * This constructor will decode a raw packet byte array into the individual
	 * fields on this class.
	 * </p>
	 * 
	 * @param packet
	 *        the raw packet data to decode
	 */
	public SmaPacket(byte[] packet) {
		this.packet = packet;
		decodePacket();
	}

	/**
	 * Get a specific user data field value.
	 * 
	 * @param key
	 *        the key of the field to get
	 * @return the associated value, or <em>null</em> if not available
	 */
	public Object getUserDataField(SmaUserDataField key) {
		if ( userDataFields == null ) {
			return null;
		}
		return userDataFields.get(key);
	}

	/**
	 * Return <em>true</em> if this packet is valid according to the packet's
	 * CRC check.
	 * 
	 * @return boolean
	 */
	public boolean isValid() {
		return crc != 0 && computedCrc == crc && command != null && command != SmaCommand.Unknown;
	}

	@Override
	public String toString() {
		return "SmaPacket{command=" + command + ",packetCounter=" + packetCounter + ",crc=" + crc
				+ ",control=" + control + ",valid=" + isValid() + '}';
	}

	private void encodePacket() {
		byte[] req = new byte[13 + this.userData.length + 3];
		req[0] = WAKEUP; // wakeup
		req[1] = WAKEUP;
		req[2] = TELEGRAM; // telegram
		req[3] = (byte) this.userData.length; // length of data, twice
		req[4] = req[3];
		req[5] = TELEGRAM; // telegram
		req[6] = (byte) 0x00; // source address
		req[7] = req[6];
		req[8] = (byte) (this.destAddress & 0xFF); // destination (low byte)
		req[9] = (byte) ((this.destAddress >> 8) & 0xFF);// destination (high byte)
		req[10] = (byte) this.control.getCode();
		req[11] = (byte) this.packetCounter;
		req[12] = (byte) this.command.getCode();
		System.arraycopy(this.userData, 0, req, 13, this.userData.length);

		this.crc = 0;
		for ( int i = 6; i < 13 + this.userData.length; i++ ) {
			this.crc += (0xFF & req[i]);
		}
		req[req.length - 3] = (byte) (this.crc & 0xFF); // computedCrc low byte
		req[req.length - 2] = (byte) ((this.crc >> 8) & 0xFF); // computedCrc high byte
		req[req.length - 1] = END;
		this.computedCrc = this.crc;
		this.packet = req;
	}

	private void decodePacket() {
		if ( packet == null || packet.length < 1 ) {
			return;
		}
		int idx = 0;
		// skip (optional) WAKEUP bytes
		while ( packet[idx] == WAKEUP ) {
			idx++;
		}

		// next is TELEGRAM, user data length, user data length, TELEGRAM
		idx++;
		int dataLength = 0xFF & packet[idx];
		userData = new byte[dataLength];
		idx += 3;

		// CRC data starts now
		for ( int i = 0; i < (7 + dataLength); i++, idx++ ) {
			int b = 0xFF & packet[idx];
			switch (i) {
				case 0:
					srcAddress = b;
					break;

				case 1:
					srcAddress |= (b << 8);
					break;

				case 2:
					destAddress = b;
					break;

				case 3:
					destAddress |= (b << 8);
					break;

				case 4:
					control = SmaControl.forCode(b);
					break;

				case 5:
					packetCounter = b;
					break;

				case 6:
					command = SmaCommand.forCode(b);
					break;

				default:
					userData[i - 7] = packet[idx];

			}
			computedCrc += b;
		}

		// CRC two bytes
		crc = (0xFF & packet[idx++]) | ((0xFF & packet[idx++]) << 8);

		// final byte should be 0x16
		idx++;

		// we may have more data collected that a single packet, so if more
		// adjust our packet to the correct length, so calling code knows how
		// many bytes we consumed constructing this packet
		if ( idx < packet.length ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace("Adjusting packet length from " + packet.length + " to " + idx);
			}
			byte[] adjustedPacket = new byte[idx];
			System.arraycopy(packet, 0, adjustedPacket, 0, idx);
			this.packet = adjustedPacket;
		}
	}

	/**
	 * Decode the user data into a Map of {@link SmaUserDataField} keys and
	 * associated values.
	 * 
	 * <p>
	 * After calling this method, use
	 * {@link #getUserDataField(SmaUserDataField)} to obtain values for specific
	 * fields.
	 * </p>
	 */
	public void decodeUserDataFields() {
		Map<SmaUserDataField, Object> results = new EnumMap<SmaUserDataField, Object>(
				SmaUserDataField.class);
		if ( command != null ) {
			switch (command) {
				case NetStart:
					decodeNetUserDataFields(results);
					break;

				case GetChannelInfo:
					decodeChannelInfoUserDataFields(results);
					break;

				case GetData:
					decodeGetDataUserDataFields(results);
					break;

				case SetData:
					decodeSetDataUserDataFields(results);
					break;

				default:
					// nothing to do
					break;
			}
		}
		this.userDataFields = results;
	}

	private void decodeGetDataUserDataFields(Map<SmaUserDataField, Object> results) {
		//  # 1  request type1
		//  # 1  request type2
		//  # 1  channel number (index)
		//  # 2  number of data sets
		//  # 4  seconds since
		//  # 4  time basis
		//  # other stuff
		if ( userData.length < 13 ) {
			return;
		}
		SmaChannelType type1 = decodeGetSetUserDataFieldsHeader(results);
		results.put(SmaUserDataField.SecondsSince, (0xFF & userData[5]) | ((0xFF & userData[6]) << 8)
				| ((0xFF & userData[7]) << 16) | ((0xFF & userData[8]) << 24));
		results.put(SmaUserDataField.TimeBasis, (0xFF & userData[9]) | ((0xFF & userData[10]) << 8)
				| ((0xFF & userData[11]) << 16) | ((0xFF & userData[12]) << 24));

		switch (type1) {

			case Analog:
				// # 2 byte integer
				results.put(SmaUserDataField.Value, (0xFF & userData[13]) | ((0xFF & userData[14]) << 8));
				break;

			case Digital:
				// # 16 byte char
				// # 16 byte char
				results.put(SmaUserDataField.TextLow, SmaUtils.parseString(userData, 13, 16));
				results.put(SmaUserDataField.TextHigh, SmaUtils.parseString(userData, 29, 16));
				break;

			case Counter:
				// # 4 byte integer
				results.put(SmaUserDataField.Value, (0xFF & userData[13]) | ((0xFF & userData[14]) << 8)
						| ((0xFF & userData[15]) << 16) | ((0xFF & userData[16]) << 24));
				break;

			case Status:
				// # 4-byte char
				results.put(SmaUserDataField.Value, SmaUtils.parseString(userData, 13, 4));
				break;

			default:
				results.put(SmaUserDataField.Error, "Unknown user data type");
				break;
		}

		if ( LOG.isTraceEnabled() ) {
			LOG.trace("Decoded GetData userDataFields: " + results);
		}
	}

	private void decodeSetDataUserDataFields(Map<SmaUserDataField, Object> results) {
		//  # 1  request type1
		//  # 1  request type2
		//  # 1  channel number (index)
		//  # 2  number of data sets
		if ( userData.length < 5 ) {
			return;
		}
		decodeGetSetUserDataFieldsHeader(results);
		if ( LOG.isTraceEnabled() ) {
			LOG.trace("Decoded SetData userDataFields: " + results);
		}
	}

	private SmaChannelType decodeGetSetUserDataFieldsHeader(Map<SmaUserDataField, Object> results) {
		//  # 1  request type1
		//  # 1  request type2
		//  # 1  channel number (index)
		//  # 2  number of data sets
		SmaChannelType type1 = SmaChannelType.forCode(0xFF & userData[0]);

		results.put(SmaUserDataField.ChannelType1, type1);
		results.put(SmaUserDataField.ChannelType2, 0xFF & userData[1]);
		results.put(SmaUserDataField.ChannelIndex, 0xFF & userData[2]);
		results.put(SmaUserDataField.DataSets, (0xFF & userData[3]) | ((0xFF & userData[4]) << 8));
		results.put(SmaUserDataField.SecondsSince, (0xFF & userData[5]) | ((0xFF & userData[6]) << 8)
				| ((0xFF & userData[7]) << 16) | ((0xFF & userData[8]) << 24));
		results.put(SmaUserDataField.TimeBasis, (0xFF & userData[9]) | ((0xFF & userData[10]) << 8)
				| ((0xFF & userData[11]) << 16) | ((0xFF & userData[12]) << 24));

		return type1;
	}

	private void decodeNetUserDataFields(Map<SmaUserDataField, Object> results) {
		if ( userData.length < 12 ) {
			return;
		}
		//  # 4 bytes of serial (number)
		//  # 8 bytes of device type (characters)
		long serial = (0xFF & userData[0]) | ((0xFF & userData[1]) << 8) | ((0xFF & userData[2]) << 16)
				| ((0xFF & userData[3]) << 24);
		results.put(SmaUserDataField.DeviceSerialNumber, Long.valueOf(serial));

		try {
			String type = new String(userData, 4, 8, "ASCII");
			results.put(SmaUserDataField.DeviceType, type);
		} catch ( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}

	private void decodeChannelInfoUserDataFields(Map<SmaUserDataField, Object> results) {
		List<SmaChannel> channels = new ArrayList<SmaChannel>();
		int offset = 0;
		do {
			SmaChannel channel = new SmaChannel(this.userData, offset);
			if ( LOG.isTraceEnabled() ) {
				LOG.trace("Found channel " + channel);
			}
			channels.add(channel);
			offset += channel.getDataLength();
		} while ( offset < userData.length );
		results.put(SmaUserDataField.Channels, channels);
	}

	/**
	 * @return the userData
	 */
	public byte[] getUserData() {
		return userData;
	}

	/**
	 * @param userData
	 *        the userData to set
	 */
	public void setUserData(byte[] userData) {
		this.userData = userData;
	}

	/**
	 * @return the srcAddress
	 */
	public int getSrcAddress() {
		return srcAddress;
	}

	/**
	 * @return the destAddress
	 */
	public int getDestAddress() {
		return destAddress;
	}

	/**
	 * @return the control
	 */
	public SmaControl getControl() {
		return control;
	}

	/**
	 * @return the packetCounter
	 */
	public int getPacketCounter() {
		return packetCounter;
	}

	/**
	 * @return the command
	 */
	public SmaCommand getCommand() {
		return command;
	}

	/**
	 * @return the crc
	 */
	public int getCrc() {
		return crc;
	}

	/**
	 * @return the computedCrc
	 */
	public int getComputedCrc() {
		return computedCrc;
	}

	/**
	 * @return the packet
	 */
	public byte[] getPacket() {
		return packet;
	}

}

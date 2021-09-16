/* ==================================================================
 * CommandMessage.java - Jul 7, 2012 3:20:20 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.rfxcom;

/**
 * A "command" to sent to, or command response received from, the 
 * RFXCOM transceiver.
 * 
 * @author matt
 * @version 1.0
 */
public class CommandMessage extends BaseDataMessage {

	private static final short PACKET_SIZE = 13;

	private final Command command;

	/**
	 * Construct a command message.
	 * 
	 * <p>The sequence number is set to 0.</p>
	 * 
	 * @param command the command
	 */
	public CommandMessage(Command command) {
		this(command, (short)0, null);
	}
	
	/**
	 * Construct a command message with a sequence number.
	 * 
	 * @param command the command
	 * @param sequenceNumber the sequence number
	 */
	public CommandMessage(Command command, short sequenceNumber) {
		this(command, sequenceNumber, null);
	}
	
	/**
	 * Construct a command message with data.
	 * 
	 * <p>The first byte in {@code data} (if provided) is ignored, as the
	 * {@code command} parameter is used in preference.</p>
	 * 
	 * @param command the command
	 * @param sequenceNumber the sequence number
	 * @param data the command data (may be {@literal null})
	 */
	public CommandMessage(Command command, short sequenceNumber, byte[] data) {
		super(PACKET_SIZE, MessageType.Command, (short)0, sequenceNumber, data);
		this.command = command;
	}

	/**
	 * Construct a response command message.
	 * 
	 * @param sequenceNumber the sequence number
	 * @param data the response message data (may <b>not</b> be {@literal null})
	 */
	public CommandMessage(short sequenceNumber, byte[] data) {
		super(PACKET_SIZE, MessageType.CommandResponse, (short)0, sequenceNumber, data);
		if ( data != null && data.length > 0 ) {
			this.command = Command.valueOf(data[0]);
		} else {
			this.command = null;
		}
	}

	@Override
	public byte[] getMessagePacket() {
		byte[] packet = new byte[] {
				PACKET_SIZE,
				getType().getMessageValue(),
				(byte)getSubType(),
				(byte)getSequenceNumber(),
				(command == null ? 0 : command.getMessageValue()),
				0, 0, 0, 0, 0, 0, 0, 0, 0
		};
		if ( getData() != null ) {
			// copy  msg1-9 bytes (data contains command at byte 0)
			System.arraycopy(getData(), 1, packet, 5, (getData().length - 1));
		}
		return packet;
	}

	public Command getCommand() {
		return command;
	}

}

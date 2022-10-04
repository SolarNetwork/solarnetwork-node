/* ==================================================================
 * PVI3800Command.java - 15/05/2018 4:29:41 PM
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
 * Enumeration of command + subcommands.
 * 
 * @author matt
 * @version 1.0
 */
public enum PVI3800Command implements Command {

	/** Read the inverter identification information. */
	InfoReadIdentification(0, 0, -1),

	/** Read the device serial number. */
	InfoReadSerialNumber(0, 1, 10),

	/** Read the lifetime total energy export, as UInt64 Wh. */
	MeterReadLifetimeTotalEnergy(24, 1, 8),

	/** Read the AC combined active power, as UInt16 W. */
	MeterReadAcCombinedActivePower(71, 4, 2),

	/** No operation. */
	NOOP(0, 0, 0),

	;

	private static final byte[] NULL_BODY = new byte[] { 0 };

	private final byte command;
	private final byte subCommand;
	private final int bodyLength;

	private PVI3800Command(int command, int subCommand, int bodyLength) {
		this.command = (byte) (command & 0xFF);
		this.subCommand = (byte) (subCommand & 0xFF);
		this.bodyLength = bodyLength;
	}

	@Override
	public byte getCommand() {
		return command;
	}

	@Override
	public byte getSubCommand() {
		return subCommand;
	}

	@Override
	public int getBodyLength() {
		return bodyLength;
	}

	/**
	 * Get a request packet for this command.
	 * 
	 * @param addr
	 *        the address of the inverter to address the packet to
	 * @return the packet
	 */
	public Packet request(int addr) {
		return request(addr, null);
	}

	/**
	 * Get a request packet for this command.
	 * 
	 * @param addr
	 *        the address of the inverter to address the packet to
	 * @param body
	 *        the request body, or {@literal null} to insert a "null" body of
	 *        one {@literal 0} byte
	 * @return the packet
	 */
	public Packet request(int addr, byte[] body) {
		return Packet.forCommand(addr, command, subCommand, (body == null ? NULL_BODY : body));
	}

	/**
	 * Get an enum value from command and sub-command values in a byte array.
	 * 
	 * @param data
	 *        the command data, which must have at least {@code offset + 2}
	 *        length
	 * @param offset
	 *        the offset in {@code data} to read from
	 * @return the enum value
	 * @throws IllegalArgumentException
	 *         if the command is not supported
	 */
	public static PVI3800Command forCommandData(byte[] data, int offset) {
		if ( data == null || data.length < offset + 2 ) {
			throw new IllegalArgumentException("Not enough data");
		}
		return forCommand(data[offset], data[offset + 1]);
	}

	/**
	 * Get an enum value from command and sub-command values.
	 * 
	 * @param command
	 *        the command value
	 * @param subCommand
	 *        the sub-command value
	 * @return the enum value
	 * @throws IllegalArgumentException
	 *         if the command is not supported
	 */
	public static PVI3800Command forCommand(int command, int subCommand) {
		final byte c = (byte) (command & 0xFF);
		final byte s = (byte) (subCommand & 0xFF);
		return forCommand(c, s);
	}

	/**
	 * Get an enum value from command and sub-command values.
	 * 
	 * @param command
	 *        the command value
	 * @param subCommand
	 *        the sub-command value
	 * @return the enum value
	 * @throws IllegalArgumentException
	 *         if the command is not supported
	 */
	public static PVI3800Command forCommand(byte command, byte subCommand) {
		for ( PVI3800Command e : PVI3800Command.values() ) {
			if ( command == e.command && subCommand == e.subCommand ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Command [" + command + "/" + subCommand + "] not supported");
	}

}

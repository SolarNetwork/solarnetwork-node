/* ==================================================================
 * SendMessageImpl.java - 21/09/2019 7:27:33 am
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

package net.solarnetwork.node.io.canbus.socketcand.msg;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.io.canbus.socketcand.Addressed;
import net.solarnetwork.node.io.canbus.socketcand.MessageType;
import net.solarnetwork.node.io.canbus.socketcand.SendMessage;
import net.solarnetwork.node.io.canbus.socketcand.SocketcandUtils;

/**
 * Implementation of {@link SendMessage}.
 * 
 * @author matt
 * @version 1.0
 */
public class SendMessageImpl extends AddressedDataMessage implements SendMessage {

	private static final int DATA_OFFSET = 2;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the message type, or {@literal null} if not known
	 * @param command
	 *        the raw command, if {@code type} is {@literal null}
	 * @param arguments
	 *        the raw command arguments
	 * @throws IllegalArgumentException
	 *         if the arguments are inappropriate for a send message
	 */
	public SendMessageImpl(List<String> arguments) {
		super(MessageType.Send, null, arguments, 0, DATA_OFFSET);

	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The address will not be forced into extended form.
	 * </p>
	 * 
	 * @param address
	 *        the address to send the message to
	 * @param data
	 *        the data to include in the message, or {@literal null} for no data
	 */
	public SendMessageImpl(int address, byte[] data) {
		this(address, false, data);
	}

	/**
	 * Constructor.
	 * 
	 * @param address
	 *        the address to send the message to
	 * @param forceExtendedAddress
	 *        {@literal true} to force {@code address} to be treated as an
	 *        extended address, even it if would otherwise fit
	 * @param data
	 *        the data to include in the message, or {@literal null} for no data
	 */
	public SendMessageImpl(int address, boolean forceExtendedAddress, byte[] data) {
		super(MessageType.Send, null, generateArguments(address, forceExtendedAddress, data), 0,
				DATA_OFFSET, forceExtendedAddress);
	}

	private static List<String> generateArguments(int address, boolean forceExtendedAddress,
			byte[] data) {
		final int dataLen = (data != null ? data.length : 0);
		final boolean extended = forceExtendedAddress || address > Addressed.MAX_STANDARD_ADDRESS;
		List<String> args = new ArrayList<>(2 + dataLen);
		args.add(String.format(extended ? "%08X" : "%X", address));
		args.add(String.valueOf(dataLen));
		List<String> hexData = SocketcandUtils.encodeHexStrings(data, 0, dataLen);
		if ( hexData != null ) {
			args.addAll(hexData);
		}
		return args;
	}

}

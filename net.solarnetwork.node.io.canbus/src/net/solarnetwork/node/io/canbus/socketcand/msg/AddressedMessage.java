/* ==================================================================
 * AddressedMessage.java - 20/09/2019 2:44:58 pm
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

import java.util.List;
import net.solarnetwork.node.io.canbus.socketcand.Addressed;
import net.solarnetwork.node.io.canbus.socketcand.MessageType;

/**
 * Implementation of an addressed message.
 * 
 * @author matt
 * @version 1.0
 */
public class AddressedMessage extends BasicMessage implements Addressed {

	private final int address;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The address will be assumed to be the first argument.
	 * </p>
	 * 
	 * @param type
	 *        the message type, or {@literal null} if not known
	 * @param command
	 *        the raw command, if {@code type} is {@literal null}
	 * @param arguments
	 *        the raw command arguments
	 * @throws IllegalArgumentException
	 *         if both {@code type} and {@code command} are {@literal null}
	 */
	public AddressedMessage(MessageType type, String command, List<String> arguments) {
		this(type, command, arguments, 0);
	}

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
	 *         if both {@code type} and {@code command} are {@literal null}, or
	 *         {@code arguments} does not have an {@code addressIndex} element,
	 *         or the address argument cannot be parsed as a base-16 number
	 */
	public AddressedMessage(MessageType type, String command, List<String> arguments, int addressIndex) {
		super(type, command, arguments);
		if ( arguments == null || arguments.size() <= addressIndex ) {
			throw new IllegalArgumentException(
					"The frame bus address argument " + addressIndex + " is missing. ");
		}
		try {
			this.address = Integer.parseInt(arguments.get(addressIndex), 16);
		} catch ( NumberFormatException e ) {
			throw new IllegalArgumentException("The frame bus address argument ["
					+ arguments.get(addressIndex) + "] cannot be parsed as a base-16 number.", e);
		}
	}

	@Override
	public final int getAddress() {
		return address;
	}

}

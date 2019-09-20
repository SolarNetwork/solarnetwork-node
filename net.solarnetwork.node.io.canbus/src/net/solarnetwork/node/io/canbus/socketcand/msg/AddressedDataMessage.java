/* ==================================================================
 * AddressedDataMessage.java - 21/09/2019 8:11:11 am
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
import net.solarnetwork.node.io.canbus.socketcand.DataContainer;
import net.solarnetwork.node.io.canbus.socketcand.MessageType;
import net.solarnetwork.node.io.canbus.socketcand.SocketcandUtils;

/**
 * A message that is addressed and contains data.
 * 
 * @author matt
 * @version 1.0
 */
public class AddressedDataMessage extends AddressedMessage implements DataContainer {

	private final int dataIndex;

	/**
	 * @param type
	 *        the message type, or {@literal null} if not known
	 * @param command
	 *        the raw command, if {@code type} is {@literal null}
	 * @param arguments
	 *        the raw command arguments
	 * @param addressIndex
	 *        the index within {@code arguments} that contains the base-16
	 *        address value
	 * @param dataIndex
	 *        the index within {@code arguments} that is the start of the data
	 * @throws IllegalArgumentException
	 *         if both {@code type} and {@code command} are {@literal null}, or
	 *         the arguments are inappropriate for the message
	 */
	public AddressedDataMessage(MessageType type, String command, List<String> arguments,
			int addressIndex, int dataIndex) {
		super(type, command, arguments, addressIndex);
		if ( dataIndex < 0 ) {
			throw new IllegalArgumentException("The data index must be >= 0.");
		}
		this.dataIndex = dataIndex;
	}

	@Override
	public int getDataLength() {
		List<String> arguments = getArguments();
		return (arguments != null && arguments.size() > dataIndex ? arguments.size() - dataIndex : 0);
	}

	@Override
	public byte[] getData() {
		List<String> arguments = getArguments();
		if ( arguments == null || arguments.size() <= dataIndex ) {
			return new byte[0];
		}
		return SocketcandUtils.decodeHexStrings(arguments, dataIndex, arguments.size());
	}

}

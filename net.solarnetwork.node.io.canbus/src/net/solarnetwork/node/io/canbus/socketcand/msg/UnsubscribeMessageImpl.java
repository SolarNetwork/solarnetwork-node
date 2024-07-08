/* ==================================================================
 * UnsubscribeMessageImpl.java - 23/09/2019 4:56:55 pm
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

import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.io.canbus.Addressed;
import net.solarnetwork.node.io.canbus.socketcand.MessageType;
import net.solarnetwork.node.io.canbus.socketcand.UnsubscribeMessage;

/**
 * Implementation of {@link UnsubscribeMessage}.
 *
 * @author matt
 * @version 1.0
 */
public class UnsubscribeMessageImpl extends AddressedMessage implements UnsubscribeMessage {

	private static final int ADDRESS_OFFSET = 0;

	/**
	 * Constructor.
	 *
	 * @param arguments
	 *        the raw command arguments
	 * @throws IllegalArgumentException
	 *         if the arguments are inappropriate for a muxfilter message
	 */
	public UnsubscribeMessageImpl(List<String> arguments) {
		super(MessageType.Unsubscribe, null, arguments, ADDRESS_OFFSET);
	}

	/**
	 * Constructor.
	 *
	 * @param address
	 *        the address to send the message to
	 * @param forceExtendedAddress
	 *        {@literal true} to force {@code address} to be treated as an
	 *        extended address, even it if would otherwise fit
	 */
	public UnsubscribeMessageImpl(int address, boolean forceExtendedAddress) {
		super(MessageType.Unsubscribe, null, generateArguments(address, forceExtendedAddress),
				ADDRESS_OFFSET);
	}

	private static List<String> generateArguments(int address, boolean forceExtendedAddress) {
		return Collections.singletonList(Addressed.hexAddress(address, forceExtendedAddress));
	}

}

/* ==================================================================
 * SubscribeMessageImpl.java - 20/09/2019 2:57:04 pm
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
import net.solarnetwork.node.io.canbus.socketcand.SubscribeMessage;

/**
 * Implementation of {@link SubscribeMessage}.
 * 
 * @author matt
 * @version 1.0
 */
public class SubscribeMessageImpl extends AddressedMessage implements SubscribeMessage {

	private static final int SECONDS_OFFSET = 0;
	private static final int MICROSECONDS_OFFSET = 1;
	private static final int ADDRESS_OFFSET = 2;

	private final int seconds;
	private final int microseconds;

	/**
	 * Constructor.
	 * 
	 * @param arguments
	 *        the raw command arguments
	 * @throws IllegalArgumentException
	 *         if the arguments are inappropriate for a subscribe message
	 */
	public SubscribeMessageImpl(List<String> arguments) {
		super(MessageType.Subscribe, null, arguments, ADDRESS_OFFSET);
		try {
			this.seconds = Integer.parseInt(arguments.get(SECONDS_OFFSET));
			this.microseconds = Integer.parseInt(arguments.get(MICROSECONDS_OFFSET));
		} catch ( NumberFormatException e ) {
			throw new IllegalArgumentException("The seconds [" + arguments.get(SECONDS_OFFSET)
					+ "] and/or microseconds [" + arguments.get(MICROSECONDS_OFFSET)
					+ "] arguments could not be parsed as numbers.", e);
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param address
	 *        the address to send the message to
	 * @param forceExtendedAddress
	 *        {@literal true} to force {@code address} to be treated as an
	 *        extended address, even it if would otherwise fit
	 * @param limitSeconds
	 *        the limit seconds
	 * @param limitMicroseconds
	 *        the limit microseconds
	 */
	public SubscribeMessageImpl(int address, boolean forceExtendedAddress, int limitSeconds,
			int limitMicroseconds) {
		super(MessageType.Subscribe, null,
				generateArguments(address, forceExtendedAddress, limitSeconds, limitMicroseconds),
				ADDRESS_OFFSET, forceExtendedAddress);
		this.seconds = limitSeconds;
		this.microseconds = limitMicroseconds;
	}

	private static List<String> generateArguments(int address, boolean forceExtendedAddress,
			int limitSeconds, int limitMicroseconds) {
		List<String> args = new ArrayList<>(3);
		args.add(String.valueOf(limitSeconds));
		args.add(String.valueOf(limitMicroseconds));
		args.add(Addressed.hexAddress(address, forceExtendedAddress));
		return args;
	}

	@Override
	public int getSeconds() {
		return seconds;
	}

	@Override
	public int getMicroseconds() {
		return microseconds;
	}

}

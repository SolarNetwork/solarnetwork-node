/* ==================================================================
 * FrameMessageImpl.java - 20/09/2019 7:18:05 am
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import net.solarnetwork.node.io.canbus.socketcand.FrameMessage;
import net.solarnetwork.node.io.canbus.socketcand.MessageType;
import net.solarnetwork.node.io.canbus.socketcand.SocketcandUtils;

/**
 * A specialized {@link Message} for a frame.
 * 
 * @author matt
 * @version 1.0
 */
public class FrameMessageImpl extends BasicMessage implements FrameMessage {

	private final int address;
	private final int seconds;
	private final int microseconds;

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
	 *         if {@code type} is not {@link MessageType#Frame} or the arguments
	 *         are inappropriate for a frame message
	 */
	public FrameMessageImpl(MessageType type, String command, List<String> arguments) {
		super(type, command, arguments);
		if ( type != MessageType.Frame ) {
			throw new IllegalArgumentException("A Frame message type must be used, not " + type);
		}
		if ( arguments == null || arguments.size() < 2 ) {
			throw new IllegalArgumentException("The frame bus address is missing.");
		}
		this.address = Integer.parseInt(arguments.get(0), 16);

		String time = arguments.get(1);
		int dotIdx = time.indexOf(".");
		if ( dotIdx < 0 ) {
			this.seconds = Integer.parseInt(time);
			this.microseconds = 0;
		} else {
			this.seconds = Integer.parseInt(time.substring(0, dotIdx));
			this.microseconds = (dotIdx + 1 < time.length()
					? Integer.parseInt(time.substring(dotIdx + 1))
					: 0);
		}
	}

	@Override
	public int getAddress() {
		return address;
	}

	@Override
	public int getSeconds() {
		return seconds;
	}

	@Override
	public int getMicroseconds() {
		return microseconds;
	}

	@Override
	public byte[] getData() {
		List<String> hexData = getArguments();
		if ( hexData == null || hexData.size() < 3 ) {
			return new byte[0];
		}
		try {
			ByteArrayOutputStream byos = new ByteArrayOutputStream(hexData.size() * 2);
			for ( String hex : hexData.subList(2, hexData.size()) ) {
				byos.write(SocketcandUtils.decodeHexPadStart(hex.toCharArray()));
			}
			return byos.toByteArray();
		} catch ( IOException e ) {
			// drat
		}
		return new byte[0];
	}

}

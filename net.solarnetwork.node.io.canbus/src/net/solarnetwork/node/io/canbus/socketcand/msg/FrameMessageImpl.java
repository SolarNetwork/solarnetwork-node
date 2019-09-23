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

import java.util.List;
import net.solarnetwork.node.io.canbus.socketcand.FrameMessage;
import net.solarnetwork.node.io.canbus.socketcand.MessageType;

/**
 * A specialized {@link Message} for a frame.
 * 
 * @author matt
 * @version 1.0
 */
public class FrameMessageImpl extends AddressedDataMessage implements FrameMessage {

	private static final int DATA_OFFSET = 2;

	private final int seconds;
	private final int microseconds;

	/**
	 * Constructor.
	 * 
	 * @param arguments
	 *        the raw command arguments
	 * @throws IllegalArgumentException
	 *         if the arguments are inappropriate for a frame message
	 */
	public FrameMessageImpl(List<String> arguments) {
		super(MessageType.Frame, null, arguments, 0, DATA_OFFSET);
		String time = arguments.get(1);
		int dotIdx = time.indexOf(".");
		try {
			if ( dotIdx < 0 ) {
				this.seconds = Integer.parseInt(time);
				this.microseconds = 0;
			} else {
				this.seconds = Integer.parseInt(time.substring(0, dotIdx));
				this.microseconds = (dotIdx + 1 < time.length()
						? Integer.parseInt(time.substring(dotIdx + 1))
						: 0);
			}
		} catch ( NumberFormatException e ) {
			throw new IllegalArgumentException(
					"The time argument [" + time + "] could not be parsed as a fractional number.", e);
		}
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

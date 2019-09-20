/* ==================================================================
 * SocketcandUtils.java - 20/09/2019 6:24:55 am
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

package net.solarnetwork.node.io.canbus.socketcand;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.io.canbus.socketcand.msg.AddressedMessage;
import net.solarnetwork.node.io.canbus.socketcand.msg.BasicMessage;
import net.solarnetwork.node.io.canbus.socketcand.msg.FrameMessageImpl;
import net.solarnetwork.node.io.canbus.socketcand.msg.SubscribeMessageImpl;

/**
 * Utilities for dealing with the socketcand protocol.
 * 
 * @author matt
 * @version 1.0
 */
public final class SocketcandUtils {

	private static enum ReadState {
		SEEK,
		START_SKIP_SPACE,
		PARSE,
	}

	/**
	 * Read a message from a character stream.
	 * 
	 * @param in
	 *        the character stream to read from
	 * @param buffer
	 *        a character buffer to use, which will be overwritten with data and
	 *        must be large enough to hold the complete message
	 * @return the message, never {@literal null}
	 * @throws IOException
	 *         if any IO error occurs
	 * @throws IndexOutOfBoundsException
	 *         if {@code buffer} is not large enough to hold a complete message
	 */
	public static Message readMessage(Reader in, char[] buffer) throws IOException {
		int pos = 0;
		int mark = 0;
		ReadState state = ReadState.SEEK;
		String command = null;
		List<String> arguments = null;

		LOOP: while ( true ) {
			char c = (char) in.read();
			// find opening <
			switch (state) {
				case SEEK:
					if ( c == '<' ) {
						state = ReadState.START_SKIP_SPACE;
					}
					break;

				case START_SKIP_SPACE:
					if ( c != ' ' ) {
						buffer[pos] = c;
						pos++;
						state = ReadState.PARSE;
					}
					break;

				case PARSE:
					if ( c == ' ' ) {
						String s = new String(buffer, mark, pos - mark);
						if ( command == null ) {
							command = s;
						} else {
							if ( arguments == null ) {
								arguments = new ArrayList<String>(8);
							}
							arguments.add(s);
						}
						mark = pos + 1;
					} else if ( c == '>' ) {
						break LOOP;
					}
					buffer[pos] = c;
					pos++;
					break;
			}
		}

		MessageType type = MessageType.forCommand(command);
		if ( type == MessageType.Frame ) {
			return new FrameMessageImpl(type, command, arguments);
		} else if ( type == MessageType.Subscribe ) {
			return new SubscribeMessageImpl(type, command, arguments);
		} else if ( type == MessageType.Unsubscribe ) {
			return new AddressedMessage(type, command, arguments);
		} else {
			return new BasicMessage(type, command, arguments);
		}
	}

	/**
	 * Convert a hex-encoded string to a byte array.
	 * 
	 * <p>
	 * If the string does not have an even number of characters, a {@literal 0}
	 * will be inserted at the start of the string.
	 * </p>
	 * 
	 * @param s
	 *        the string to decode
	 * @return the bytes, never {@literal null}
	 * @see #decodeHexStringPadStart(String)
	 */
	public static byte[] decodeHexString(String s) {
		if ( s == null ) {
			return new byte[0];
		}
		return decodeHexPadStart(s.toCharArray());
	}

	/**
	 * Convert a hex-encoded string to a byte array.
	 * 
	 * <p>
	 * If the string does not have an even number of characters, a {@literal 0}
	 * will be inserted at the start of the string.
	 * </p>
	 * 
	 * @param chars
	 *        the characters to decode
	 * @return the bytes, never {@literal null}
	 */
	public static byte[] decodeHexPadStart(final char[] chars) {
		if ( chars == null || chars.length < 1 ) {
			return new byte[0];
		}
		final int len = chars.length;
		final boolean even = (len & 0x01) == 0;
		final byte[] data = new byte[(even ? len : len + 1) / 2];
		int i = 0;
		int j = 0;
		if ( !even ) {
			data[i] = (byte) (Character.digit(chars[j], 16) & 0xFF);
			i++;
			j++;
		}
		for ( ; j < len; i++ ) {
			int n = Character.digit(chars[j], 16) << 4;
			j++;
			n |= Character.digit(chars[j], 16);
			j++;
			data[i] = (byte) (n & 0xFF);
		}
		return data;
	}

}

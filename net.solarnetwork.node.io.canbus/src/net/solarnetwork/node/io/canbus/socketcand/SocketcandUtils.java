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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.io.canbus.socketcand.msg.BasicMessage;
import net.solarnetwork.node.io.canbus.socketcand.msg.FilterMessageImpl;
import net.solarnetwork.node.io.canbus.socketcand.msg.FrameMessageImpl;
import net.solarnetwork.node.io.canbus.socketcand.msg.MuxFilterMessageImpl;
import net.solarnetwork.node.io.canbus.socketcand.msg.SubscribeMessageImpl;
import net.solarnetwork.node.io.canbus.socketcand.msg.UnsubscribeMessageImpl;
import net.solarnetwork.util.ByteUtils;

/**
 * Utilities for dealing with the socketcand protocol.
 *
 * @author matt
 * @version 1.0
 */
public final class SocketcandUtils {

	private SocketcandUtils() {
		// not available
	}

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
	 * @return the message, or {@literal null} if the end of the stream is
	 *         reached
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
			int b = in.read();
			if ( b < 0 ) {
				break;
			}
			char c = (char) b;
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
		if ( command == null ) {
			return null;
		}
		MessageType type = MessageType.forCommand(command);
		if ( type != null ) {
			switch (type) {
				case Frame:
					return new FrameMessageImpl(arguments);

				case Subscribe:
					return new SubscribeMessageImpl(arguments);

				case Unsubscribe:
					return new UnsubscribeMessageImpl(arguments);

				case Filter:
					return new FilterMessageImpl(arguments);

				case Muxfilter:
					return new MuxFilterMessageImpl(arguments);

				default:
					// ignore here
			}
		}
		return new BasicMessage(type, command, arguments);
	}

	/**
	 * Decode a list of hex-encoded strings into a byte array.
	 *
	 * @param hexData
	 *        the hex-encoded strings
	 * @param fromIndex
	 *        an offset within {@code hexData} to start from (inclusive)
	 * @param toIndex
	 *        an offset within {@code hexData} to stop at (exclusive)
	 * @return the bytes, never {@literal null}
	 */
	public static byte[] decodeHexStrings(final List<String> hexData, final int fromIndex,
			final int toIndex) {
		final int hexDataSize = (hexData != null ? hexData.size() : 0);
		if ( hexDataSize < 1 || hexDataSize <= fromIndex || (toIndex - fromIndex) < 1 ) {
			return new byte[0];
		}
		final int maxIndex = (toIndex > hexDataSize ? hexDataSize : toIndex);
		try {
			ByteArrayOutputStream byos = new ByteArrayOutputStream(maxIndex - fromIndex);
			for ( String hex : (fromIndex == 0 && maxIndex == hexDataSize ? hexData
					: hexData.subList(fromIndex, maxIndex)) ) {
				byos.write(ByteUtils.decodeHexPadStart(hex.toCharArray()));
			}
			return byos.toByteArray();
		} catch ( IOException e ) {
			// drat; we ignore this
		}
		return new byte[0];
	}

	// adapted from Apache Commons Codec Hex.java

	/**
	 * Encode a byte array into a list of hex-encoded strings, one string for
	 * each individual byte.
	 *
	 * @param data
	 *        the data to encode as hex strings
	 * @param fromIndex
	 *        the starting index within {@code data} to encode (inclusive)
	 * @param toIndex
	 *        the ending index within {@code data} to encode (exclusive)
	 * @return the list of strings, or {@literal null} if no data is encoded
	 */
	public static List<String> encodeHexStrings(final byte[] data, final int fromIndex,
			final int toIndex) {
		if ( data == null || data.length < 1 || fromIndex < 0 || fromIndex >= data.length || toIndex < 0
				|| toIndex <= fromIndex ) {
			return null;
		}
		List<String> hexData = new ArrayList<>(toIndex - fromIndex);
		char[] buffer = new char[2];
		for ( int i = fromIndex; i < toIndex; i++ ) {
			hexData.add(new String(ByteUtils.encodeHexUpperCase(data[i], buffer, 0)));
		}
		return hexData;
	}

	/**
	 * Convert up to 8 bytes into a long value.
	 *
	 * @param data
	 *        the bytes to convert
	 * @param offset
	 *        the index within {@code data} to start reading from
	 * @return the extracted long value
	 */
	public static long longForBytes(byte[] data, int offset) {
		if ( data == null || data.length < 1 || offset >= data.length ) {
			return 0L;
		}
		long f = 0L;
		for ( int i = offset, j = Long.BYTES - 1; i < data.length && j >= 0; i++, j-- ) {
			f |= ((long) (data[i] & 0xFF) << (j * 8));
		}
		return f;
	}

	/**
	 * Convert a long into a list of hex strings.
	 *
	 * @param number
	 *        the long to convert
	 * @param trimEnd
	 *        {@literal true} to trim trailing zero values from the result
	 * @return the list of hex-encoded bytes
	 */
	public static List<String> encodeAsHexStrings(long number, boolean trimEnd) {
		List<String> result = new ArrayList<>(Long.BYTES);
		int lastNonZeroIndex = 0;
		char[] buffer = new char[2];
		for ( int i = 0; i < Long.BYTES; i++ ) {
			byte b = (byte) ((number >>> ((Long.BYTES - i - 1) * 8)) & 0xFF);
			if ( b != 0 ) {
				lastNonZeroIndex = i + 1;
				result.add(new String(ByteUtils.encodeHexUpperCase(b, buffer, 0)));
			} else {
				result.add("00");
			}
		}
		if ( trimEnd && lastNonZeroIndex > 0 && lastNonZeroIndex < Long.BYTES ) {
			result = result.subList(0, lastNonZeroIndex);
		}
		return result;
	}

}

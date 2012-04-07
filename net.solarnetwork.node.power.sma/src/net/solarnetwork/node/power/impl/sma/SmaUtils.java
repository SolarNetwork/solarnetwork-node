/* ===================================================================
 * SmaUtils.java
 * 
 * Created Sep 7, 2009 10:28:12 AM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.power.impl.sma;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Constants for SMA.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public final class SmaUtils {

	private static final Pattern STRING_TRIM_PAT = Pattern.compile("\\s+\\u0000?$");
	
	private SmaUtils() {
		// this class should not be constructed
	}
	
	/**
	 * Encode the user data value for a {@code SmaCommand.GetData} request packet.
	 * 
	 * @param channel the channel to get the data for
	 * @return the user data value
	 */
	public static byte[] encodeGetDataRequestUserData(SmaChannel channel) {
		byte[] result = new byte[3];
		result[0] = (byte)channel.getType1().getCode();
		result[1] = (byte)channel.getType2();
		result[2] = (byte)channel.getIndex();
		return result;
	}
	
	/**
	 * Encode the user data value for a {@code SmaCommand.SetData} request packet
	 * for a specific channel and a integer value.
	 * 
	 * <p>For {@code Analog} channel types, the value will be encoded as a 2-byte
	 * value. Otherwise a 4-byte value will be used.</p>
	 * 
	 * @param channel the channel to set the data for
	 * @param value the data to encode
	 * @return the user data value (7 or 9 bytes long)
	 */
	public static byte[] encodeSetDataRequestUserData(SmaChannel channel, int value) {
		byte[] result = new byte[channel.getType1() == SmaChannelType.Analog ? 7 : 9];
		result[0] = (byte)channel.getType1().getCode();
		result[1] = (byte)channel.getType2();
		result[2] = (byte)channel.getIndex();
		result[3] = (byte)(channel.getType1() == SmaChannelType.Analog ? 1 : 2);
		result[4] = (byte)0;
		result[5] = (byte)(0xFF & value);
		result[6] = (byte)(0xFF & (value >> 8));
		if ( channel.getType1() != SmaChannelType.Analog ) {
			result[7] = (byte)(0xFF & (value >> 16));
			result[8] = (byte)(0xFF & (value >> 24));
		}
		return result;
	}
	
	/**
	 * Parse an IEEE-754, little endian encoded float into a Float.
	 * 
	 * <p>This method expects to read 4 bytes starting at the provided {@code offset}.</p>
	 * 
	 * @param data the bytes
	 * @param offset the offset to read the float from
	 * @return a Float
	 */
	public static Float parseFloat(byte[] data, int offset) {
		int bits = (0xFF & data[offset]) 
			| ((0xFF & data[offset+1]) << 8)
			| ((0xFF & data[offset+2]) << 16)
			| ((0xFF & data[offset+3]) << 24);
		return Float.intBitsToFloat(bits);
	}

	/**
	 * Parse an ASCII encoded String from bytes, removing trailing spaces and 
	 * null character.
	 * 
	 * @param data the byte data
	 * @param offset the offset to start reading the String from
	 * @param length the length to read
	 * @return a new String
	 */
	public static String parseString(byte[] data, int offset, int length) {
		// create ASCII string and remove trailing spaces and null character
		String s = null;
		try {
			s = new String(data, offset, length, "ASCII");
		} catch ( UnsupportedEncodingException e ) {
			// should not get here
		}
		if ( s != null ) {
			Matcher m = STRING_TRIM_PAT.matcher(s);
			s = m.replaceFirst("");
		}
		return s;
	}
	
	/**
	 * Turn an integer into a little-endian encoded byte array.
	 * 
	 * @param value the integer to encode
	 * @return the byte array (4 bytes long)
	 */
	public static byte[] littleEndianBytes(int value) {
		byte[] result = new byte[4];
		result[0] = (byte)(0xFF & value);
		result[1] = (byte)(0xFF & (value >> 8));
		result[2] = (byte)(0xFF & (value >> 16));
		result[3] = (byte)(0xFF & (value >> 24));
		return result;
	}
	
}

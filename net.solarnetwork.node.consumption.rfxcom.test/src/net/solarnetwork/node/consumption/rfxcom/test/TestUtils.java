/* ==================================================================
 * TestUtils.java - Jul 7, 2012 8:10:40 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.consumption.rfxcom.test;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * Helper methods for unit tests.
 * 
 * @author matt
 * @version $Revision$
 */
public final class TestUtils {

	/**
	 * Convert a hex-encoded string into bytes.
	 * 
	 * <p>Non hexadecimal characters are stripped from the input string,
	 * for example to remove spaces.</p>
	 * 
	 * @param str the string
	 * @return the bytes
	 */
	public static byte[] bytesFromHexString(String str) {
		try {
			return Hex.decodeHex(str.replaceAll("[^0-9a-fA-F]", "").toCharArray());
		} catch (DecoderException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Remove the message header from a packet and return just the message data.
	 * 
	 * @param packet the packet
	 * @return the data
	 */
	public static byte[] extractMessageBytes(byte[] packet) {
		byte[] msg = new byte[packet.length - 4];
		System.arraycopy(packet, 4, msg, 0, msg.length);
		return msg;
	}

}

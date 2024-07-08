/* ==================================================================
 * CanbusUtils.java - 20/11/2019 4:02:18 pm
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

package net.solarnetwork.node.io.canbus.util;

import net.solarnetwork.node.io.canbus.Addressed;
import net.solarnetwork.node.io.canbus.CanbusFrame;
import net.solarnetwork.node.io.canbus.Temporal;
import net.solarnetwork.util.ByteUtils;

/**
 * General CAN bus utilities.
 *
 * @author matt
 * @version 1.0
 */
public final class CanbusUtils {

	private CanbusUtils() {
		// not available
	}

	/**
	 * Encode a {@link CanbusFrame} in a form that matches the output of the
	 * {@literal candump} log file output.
	 *
	 * @param message
	 *        the message to encode; it must also implement {@link Temporal}
	 * @param busName
	 *        the associated CAN bus name
	 * @return the log message, or {@literal null} if {@code message} is
	 *         {@literal null}
	 */
	public static String encodeCandumpLog(CanbusFrame message, String busName) {
		if ( !(message instanceof Temporal) ) {
			return null;
		}
		StringBuilder buf = new StringBuilder();
		buf.append("(");
		buf.append(((Temporal) message).toFractionalSecondsString());
		buf.append(") ");
		buf.append(busName);
		buf.append(" ");
		buf.append(Addressed.hexAddress(message.getAddress(), false));
		buf.append("#");
		byte[] data = message.getData();
		if ( data != null && data.length > 0 ) {
			char[] buffer = new char[2];
			for ( int i = 0, len = data.length; i < len; i++ ) {
				ByteUtils.encodeHexUpperCase(data[i], buffer, 0);
				buf.append(buffer);
			}
		}
		return buf.toString();
	}

}

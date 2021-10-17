/* ==================================================================
 * UsbRelayUtils.java - 18/06/2019 9:18:50 am
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

package net.solarnetwork.node.hw.lctech.relay;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.serial.SerialConnection;

/**
 * Helper class for the LC USB Relay hardware range.
 * 
 * <p>
 * See http://www.chinalctech.com/cpzx/Programmer/Relay_Module/131.html for
 * hardware reference.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class UsbRelayUtils {

	/** The default identity value. */
	public static final int DEFAULT_IDENTITY = 0xA0;

	private static final Logger log = LoggerFactory.getLogger(UsbRelayUtils.class);

	/**
	 * Set the state of a relay.
	 * 
	 * @param conn
	 *        the serial connection to use
	 * @param identity
	 *        the relay identity; use {@link #DEFAULT_IDENTITY} for the default
	 *        value
	 * @param address
	 *        the relay address; starts at 1 and increases linearly for
	 *        multi-relay devices
	 * @param open
	 *        {@literal true} to open the relay, {@literal false} to close it
	 * @throws IOException
	 *         if a communication error occurs
	 */
	public static void setRelayState(SerialConnection conn, int identity, int address, boolean open)
			throws IOException {
		if ( log.isDebugEnabled() ) {
			log.debug("Setting USB relay {} @ {}.{} state to {}", conn.getPortName(),
					Integer.toHexString(identity & 0xFF).toUpperCase(), address,
					(open ? "OPEN" : "CLOSED"));
		}
		byte state = (byte) (open ? 1 : 0);
		// @formatter:off
		byte[] msg = new byte[] {
				(byte)(identity & 0xFF),
				(byte)(address & 0xFF),
				state,
				(byte)((identity + address + state) & 0xFF)
		};
		// @formatter:on
		conn.writeMessage(msg);
	}

}

/* ==================================================================
 * PacketUtils.java - 18/05/2018 4:21:25 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.yaskawa.ecb;

import java.io.IOException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.serial.SerialConnection;

/**
 * Utility methods for sending/receiving packets.
 * 
 * @author matt
 * @version 1.0
 */
public final class PacketUtils {

	private static final Logger log = LoggerFactory.getLogger(PacketUtils.class);

	private PacketUtils() {
		// don't construct me
	}

	/**
	 * Send a packet to one inverter and get the response.
	 * 
	 * @param conn
	 *        the connection
	 * @param p
	 *        the packet to send
	 * @return the response packet
	 * @throws IOException
	 *         if an IO error occurs
	 */
	public static Packet sendPacket(SerialConnection conn, Packet p) throws IOException {
		if ( log.isDebugEnabled() ) {
			log.trace("Sending packet to {}: {}", p.getHeader().getAddress(), p.toDebugString());
		}
		conn.writeMessage(p.getBytes());
		byte[] head = conn.readMarkedMessage(new byte[] { PacketEnvelope.Start.getCode() }, 4);
		if ( log.isTraceEnabled() ) {
			log.trace("Initial repsonse from {}: {}", (head.length > 2 ? head[2] & 0xFF : -1),
					Hex.encodeHexString(head));
		}
		if ( head == null || head.length < 4 ) {
			return null;
		}
		PacketHeader header = new PacketHeader(head);
		if ( log.isTraceEnabled() ) {
			log.trace("Got header from {}: {}", header.getAddress(), header.toDebugString());
		}
		int dataLen = header.getDataLength();
		byte[] resp = conn.readMarkedMessage(new byte[] { p.getCommand(), p.getSubCommand() },
				dataLen + 3);
		if ( log.isTraceEnabled() ) {
			log.trace("Got remaining from {}: {}", (head.length > 2 ? head[2] & 0xFF : -1),
					Hex.encodeHexString(resp));
		}
		Packet respMsg = Packet.forData(head, 0, resp, 0);
		if ( log.isDebugEnabled() ) {
			log.debug("Got response from {}: {}", respMsg.getHeader().getAddress(),
					respMsg.toDebugString());
		}
		return respMsg;
	}

}

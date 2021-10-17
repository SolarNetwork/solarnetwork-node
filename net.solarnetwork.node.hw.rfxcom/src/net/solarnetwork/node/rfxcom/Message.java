/* ==================================================================
 * Message.java - Jul 6, 2012 9:26:00 AM
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
 */

package net.solarnetwork.node.rfxcom;

/**
 * A RFXCOM message packet.
 * 
 * <p>
 * RFXCOM messages follow the following format (all fields represent a single
 * byte, unless otherwise noted):
 * </p>
 * 
 * <ol>
 * <li>packet length - number of bytes in the message</li>
 * <li>type - a {@link MessageType}</li>
 * <li>sub-type - a sub categorization, depends on type</li>
 * <li>sequence number - a counter used to correlate messages</li>
 * <li>message data - specific to the message type, number of bytes equal to
 * (<em>packet length</em> - 4)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public interface Message {

	/**
	 * Get the number of bytes in the packet, without the size header byte.
	 * 
	 * @return the number of bytes in the message
	 */
	short getPacketSize();

	/**
	 * Get the message type.
	 * 
	 * @return the type
	 */
	MessageType getType();

	/**
	 * Get the message sub-type.
	 * 
	 * @return the message sub-type
	 */
	short getSubType();

	/**
	 * Get the sequence number.
	 * 
	 * @return the sequence number
	 */
	short getSequenceNumber();

	/**
	 * Get the message data, not including the message header information.
	 * 
	 * @return the message data
	 */
	byte[] getData();

	/**
	 * Get the entire message as a packet, including the 4-byte message header.
	 * 
	 * @return the message packet
	 */
	byte[] getMessagePacket();

}

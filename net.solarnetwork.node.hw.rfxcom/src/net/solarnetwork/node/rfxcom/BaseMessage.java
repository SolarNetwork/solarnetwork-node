/* ==================================================================
 * BaseMessage.java - Jul 7, 2012 3:21:01 PM
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

package net.solarnetwork.node.rfxcom;

/**
 * Abstract base {@link Message} implementation.
 * 
 * @author matt
 * @version $Revision$
 */
public abstract class BaseMessage implements Message {
	
	private final short dataSize;
	private final MessageType type;
	private final short subType;
	private final short sequenceNumber;
	
	/**
	 * Construct with values.
	 * 
	 * @param dataSize the data size
	 * @param type the message type
	 * @param subType the sub type
	 * @param sequenceNumber the sequence number
	 */
	public BaseMessage(short dataSize, MessageType type,
			short subType, short sequenceNumber) {
		super();
		this.dataSize = dataSize;
		this.type = type;
		this.subType = subType;
		this.sequenceNumber = sequenceNumber;
	}

	@Override
	public short getPacketSize() {
		return dataSize;
	}

	@Override
	public MessageType getType() {
		return type;
	}

	@Override
	public short getSubType() {
		return subType;
	}

	@Override
	public short getSequenceNumber() {
		return sequenceNumber;
	}

}

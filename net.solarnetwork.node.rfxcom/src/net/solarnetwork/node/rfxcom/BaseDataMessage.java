/* ==================================================================
 * BaseDataMessage.java - Jul 7, 2012 5:07:20 PM
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

import java.util.Arrays;

/**
 * FIXME
 * 
 * <p>TODO</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt></dt>
 *   <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class BaseDataMessage extends BaseMessage {

	private final byte[] data;

	public BaseDataMessage(short dataSize, MessageType type, short subType, 
			short sequenceNumber, byte[] data) {
		super(dataSize, type, subType, sequenceNumber);
		this.data = data;
	}

	@Override
	public byte[] getData() {
		return data;
	}

	@Override
	public byte[] getMessagePacket() {
		byte[] msg = new byte[getPacketSize()+1];
		Arrays.fill(msg, (byte)0);
		msg[0] = (byte)getPacketSize();
		msg[1] = getType().getMessageValue();
		msg[2] = (byte)getSubType();
		msg[3] = (byte)getSequenceNumber();
		if ( getData() != null ) {
			System.arraycopy(getData(), 0, msg, 4, getData().length);
		}
		return msg;
	}

}

/* ==================================================================
 * MessageType.java - Jul 6, 2012 7:07:02 PM
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
 * Message type enumeration.
 * 
 * @author matt
 * @version $Revision$
 */
public enum MessageType {

	Command(0x0),
	
	CommandResponse(0x1),
	
	CurrentMeter(0x59),
	
	Energy(0x5a);
	
	private final byte value;
	
	private MessageType(int value) {
		this.value = (byte)value;
	}
	
	/**
	 * Get the value of this value, suitable for a message packet.
	 * 
	 * @return the message value
	 */
	public byte getMessageValue() {
		return value;
	}
}

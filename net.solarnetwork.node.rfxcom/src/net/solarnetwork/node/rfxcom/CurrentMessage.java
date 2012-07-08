/* ==================================================================
 * CurrentMessage.java - Jul 6, 2012 7:06:04 PM
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

import static net.solarnetwork.node.util.DataUtils.unsigned;

/**
 * A current meter message, e.g. Owl CM113, Electrisave, Cent-a-meter.
 * 
 * @author matt
 * @version $Revision$
 */
public class CurrentMessage extends BaseDataMessage {

	private static final short PACKET_SIZE = (short)13;
	@SuppressWarnings("unused")
	private static final byte SUB_TYPE_ELEC1 = 0x1;
	private static final int IDX_BATTERY = 9; // bits 3-0
	private static final int IDX_SIGNAL = 9; // bits 7-4
	
	public CurrentMessage(short subType, short sequenceNumber, byte[] data) {
		super(PACKET_SIZE, MessageType.CurrentMeter, subType, sequenceNumber, data);
	}
	
	public boolean isBatteryLow() {
		return (getData()[IDX_BATTERY] & 0xF) == 0;
	}
	
	public int getSignalLevel() {
		return (getData()[IDX_SIGNAL] >> 4);
	}
	
	public String getAddress() {
		return String.format("%X", unsigned(getData()[0]) << 8 | unsigned(getData()[1]));
	}
	
	public int getCount() {
		return unsigned(getData()[2]);
	}
	
	public double getAmpReading1() {
		return (unsigned(getData()[3]) << 8 | unsigned(getData()[4])) / 10.0;
	}
	
	public double getAmpReading2() {
		return (unsigned(getData()[5]) << 8 | unsigned(getData()[6])) / 10.0;
	}
	
	public double getAmpReading3() {
		return (unsigned(getData()[7]) << 8 | unsigned(getData()[8])) / 10.0;
	}
		
}

/* ==================================================================
 * EnergyMessage.java - Jul 8, 2012 2:56:03 PM
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

import static net.solarnetwork.util.NumberUtils.unsigned;

/**
 * An energy message, e.g. Owl CM119, CM160.
 * 
 * @author matt
 * @version 1.0
 */
public class EnergyMessage extends BaseAddressSourceDataMessage {

	public static final byte SUB_TYPE_ELEC2 = 0x1;

	private static final short PACKET_SIZE = (short) 17;

	private static final int IDX_COUNT = 2;
	private static final int IDX_INSTANT1 = 3;
	private static final int IDX_INSTANT2 = 4;
	private static final int IDX_INSTANT3 = 5;
	private static final int IDX_INSTANT4 = 6;
	private static final int IDX_TOTAL1 = 7;
	private static final int IDX_TOTAL2 = 8;
	private static final int IDX_TOTAL3 = 9;
	private static final int IDX_TOTAL4 = 10;
	private static final int IDX_TOTAL5 = 11;
	private static final int IDX_TOTAL6 = 12;
	private static final int IDX_BATTERY = 13; // bits 3-0
	private static final int IDX_SIGNAL = 13; // bits 7-4

	public EnergyMessage(short subType, short sequenceNumber, byte[] data) {
		super(PACKET_SIZE, MessageType.Energy, subType, sequenceNumber, data);
	}

	public int getCount() {
		return unsigned(getData()[IDX_COUNT]);
	}

	public double getInstantWatts() {
		return (unsigned(getData()[IDX_INSTANT1]) * 0x1000000)
				+ (unsigned(getData()[IDX_INSTANT2]) * 0x10000)
				+ (unsigned(getData()[IDX_INSTANT3]) * 0x100) + unsigned(getData()[IDX_INSTANT4]);
	}

	public double getUsageWattHours() {
		return ((unsigned(getData()[IDX_TOTAL1]) * 0x10000000000L)
				+ (unsigned(getData()[IDX_TOTAL2]) * 0x100000000L)
				+ (unsigned(getData()[IDX_TOTAL3]) * 0x1000000)
				+ (unsigned(getData()[IDX_TOTAL4]) * 0x10000) + (unsigned(getData()[IDX_TOTAL5]) * 0x100)
				+ unsigned(getData()[IDX_TOTAL6])) / 223.666;
	}

	public boolean isBatteryLow() {
		return (unsigned(getData()[IDX_BATTERY]) & 0xF) == 0;
	}

	public int getSignalLevel() {
		return (unsigned(getData()[IDX_SIGNAL]) >> 4);
	}

	@Override
	public String toString() {
		return String.format("EnergyMessage[%s; i = %f, u = %f, s = %d]", getAddress(),
				getInstantWatts(), getUsageWattHours(), getSignalLevel());
	}

}

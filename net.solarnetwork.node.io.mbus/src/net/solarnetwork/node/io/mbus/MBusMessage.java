/* ==================================================================
 * MBusMessage.java - 01/07/2020 13:34:05 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.mbus;

import java.time.Instant;

/**
 * 
 * A class representing an MBus Message
 * 
 * @author alex
 * @version 2.0
 */
public class MBusMessage extends MBusData {

	public boolean moreRecordsFollow = false;

	public MBusMessage(MBusData data) {
		super(data);
	}

	public MBusMessage(Instant receivedTime) {
		super(receivedTime);
	}

	@Override
	public boolean equals(Object o) {
		if ( o == this ) {
			return true;
		}

		if ( !(o instanceof MBusData) ) {
			return false;
		}

		MBusMessage m = (MBusMessage) o;

		return super.equals(this) && m.moreRecordsFollow == this.moreRecordsFollow;
	}
}

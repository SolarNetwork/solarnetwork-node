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
import java.util.Objects;

/**
 *
 * A class representing an MBus Message
 *
 * @author alex
 * @version 2.0
 */
public class MBusMessage extends MBusData {

	/** Flag indicating if more message data is to follow. */
	public boolean moreRecordsFollow = false;

	/**
	 * Constructor.
	 *
	 * @param data
	 *        the data of the message
	 */
	public MBusMessage(MBusData data) {
		super(data);
	}

	/**
	 * Constructor.
	 *
	 * @param receivedTime
	 *        the received time
	 */
	public MBusMessage(Instant receivedTime) {
		super(receivedTime);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof MBusMessage other) ) {
			return false;
		}
		return moreRecordsFollow == other.moreRecordsFollow;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(moreRecordsFollow);
		return result;
	}

}

/* ==================================================================
 * BaseAddressSourceDataMessage.java - Mar 23, 2013 3:25:43 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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
 * Base message type for {@link AddressSource} data messages.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseAddressSourceDataMessage extends BaseDataMessage implements AddressSource {

	private static final int IDX_ID1 = 0;
	private static final int IDX_ID2 = 1;

	/**
	 * Constructor.
	 * 
	 * @param dataSize
	 *        the data size
	 * @param type
	 *        the message type
	 * @param subType
	 *        the sub type
	 * @param sequenceNumber
	 *        the sequence number
	 * @param data
	 *        the data
	 */
	public BaseAddressSourceDataMessage(short dataSize, MessageType type, short subType,
			short sequenceNumber, byte[] data) {
		super(dataSize, type, subType, sequenceNumber, data);
	}

	@Override
	public String getAddress() {
		return String.format("%X", unsigned(getData()[IDX_ID1]) << 8 | unsigned(getData()[IDX_ID2]));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		final String address = getAddress();
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		CurrentMessage other = (CurrentMessage) obj;
		final String myAddress = getAddress();
		final String otherAddress = other.getAddress();
		if ( myAddress == null ) {
			if ( otherAddress != null )
				return false;
		} else if ( !myAddress.equals(otherAddress) )
			return false;
		return true;
	}

	@Override
	public int compareTo(AddressSource o) {
		final String address = getAddress();
		if ( address == null ) {
			return -1;
		}
		return getAddress().compareTo(o.getAddress());
	}

}

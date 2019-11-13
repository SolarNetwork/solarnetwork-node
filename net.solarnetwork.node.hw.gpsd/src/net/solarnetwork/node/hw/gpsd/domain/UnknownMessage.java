/* ==================================================================
 * UnknownMessage.java - 12/11/2019 7:10:35 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.gpsd.domain;

import java.util.Objects;

/**
 * An unknown GPSd message.
 * 
 * @author matt
 * @version 1.0
 */
public class UnknownMessage extends AbstractGpsdMessage {

	private final Object data;

	/**
	 * Constructor.
	 * 
	 * @param messageName
	 *        the message name
	 * @param data
	 *        an optional message data object
	 */
	public UnknownMessage(String messageName, Object data) {
		super(GpsdMessageType.Unknown, messageName);
		this.data = data;
	}

	/**
	 * Constructor.
	 * 
	 * @param messageType
	 *        the message type
	 * @param data
	 *        an optional message data object
	 */
	public UnknownMessage(GpsdMessageType messageType, Object data) {
		super(messageType, null);
		this.data = data;
	}

	/**
	 * Get the message data.
	 * 
	 * @return the data, or {@literal null}
	 */
	public Object getData() {
		return data;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("UnknownMessage{");
		buf.append(getMessageName());
		buf.append("}");
		return buf.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(data);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof UnknownMessage) ) {
			return false;
		}
		UnknownMessage other = (UnknownMessage) obj;
		return Objects.equals(data, other.data);
	}

}

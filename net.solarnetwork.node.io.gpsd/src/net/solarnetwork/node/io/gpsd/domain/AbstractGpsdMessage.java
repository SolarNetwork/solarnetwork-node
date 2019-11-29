/* ==================================================================
 * AbstractGpsdMessage.java - 11/11/2019 9:31:24 pm
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

package net.solarnetwork.node.io.gpsd.domain;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base {@link GpsdMessage} implementation.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class AbstractGpsdMessage implements GpsdMessage {

	private final GpsdMessageType messageType;
	private final String messageName;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the GPSd message type, or {@literal null} if not known
	 */
	public AbstractGpsdMessage(GpsdMessageType type) {
		this(type, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the GPSd message type, or {@literal null} if not known
	 * @param name
	 *        the GPSd message name
	 */
	public AbstractGpsdMessage(GpsdMessageType type, String name) {
		super();
		this.messageType = type;
		this.messageName = name;
	}

	@JsonIgnore
	@Override
	public final GpsdMessageType getMessageType() {
		return messageType;
	}

	@JsonGetter("class")
	@Override
	public final String getMessageName() {
		if ( messageName == null && messageType != null ) {
			return messageType.getName();
		}
		return messageName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(messageName, messageType);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof AbstractGpsdMessage) ) {
			return false;
		}
		AbstractGpsdMessage other = (AbstractGpsdMessage) obj;
		return Objects.equals(messageName, other.messageName) && messageType == other.messageType;
	}

}

/* ==================================================================
 * AbstractGpsdReportMessage.java - 16/11/2019 11:52:05 am
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

import java.time.Instant;
import java.util.Objects;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public abstract class AbstractGpsdReportMessage extends AbstractGpsdMessage
		implements GpsdReportMessage {

	private final Instant timestamp;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the GPSd message type, or {@literal null} if not known
	 */
	public AbstractGpsdReportMessage(GpsdMessageType type) {
		this(type, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the GPSd message type, or {@literal null} if not known
	 * @param name
	 *        the GPSd message name
	 * @param timestamp
	 *        the timestamp
	 */
	public AbstractGpsdReportMessage(GpsdMessageType type, Instant timestamp) {
		this(type, null, timestamp);
	}

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the GPSd message type, or {@literal null} if not known
	 * @param name
	 *        the GPSd message name
	 */
	public AbstractGpsdReportMessage(GpsdMessageType type, String name) {
		this(type, name, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the GPSd message type, or {@literal null} if not known
	 * @param name
	 *        the GPSd message name
	 * @param timestamp
	 *        the timestamp
	 */
	public AbstractGpsdReportMessage(GpsdMessageType type, String name, Instant timestamp) {
		super(type, name);
		this.timestamp = timestamp;
	}

	/**
	 * Get the time/date stamp.
	 * 
	 * <p>
	 * May have a fractional part of up to .001sec precision.
	 * </p>
	 * 
	 * @return the timestamp
	 */
	@Override
	public Instant getTimestamp() {
		return timestamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(timestamp);
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
		if ( !(obj instanceof AbstractGpsdReportMessage) ) {
			return false;
		}
		AbstractGpsdReportMessage other = (AbstractGpsdReportMessage) obj;
		return Objects.equals(timestamp, other.timestamp);
	}

}

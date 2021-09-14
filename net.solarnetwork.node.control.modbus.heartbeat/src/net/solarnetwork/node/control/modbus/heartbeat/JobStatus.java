/* ==================================================================
 * JobStatus.java - Mar 22, 2014 8:09:12 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.modbus.heartbeat;

import java.time.Instant;

/**
 * Heartbeat status info bean.
 * 
 * @author matt
 * @version 2.0
 */
public class JobStatus {

	private final Instant date;
	private final boolean successful;
	private final String message;

	/**
	 * Construct with values.
	 * 
	 * @param date
	 *        the date the heartbeat was executed
	 * @param successful
	 *        {@literal true} if the heartbeat was executed successfully
	 * @param message
	 *        a message
	 */
	public JobStatus(Instant heartbeatDate, boolean successful, String heartbeatMessage) {
		super();
		this.date = heartbeatDate;
		this.successful = successful;
		this.message = heartbeatMessage;
	}

	public Instant getDate() {
		return date;
	}

	public String getMessage() {
		return message;
	}

	public boolean isSuccessful() {
		return successful;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JobStatus{");
		if ( date != null ) {
			builder.append("date=");
			builder.append(date);
			builder.append(", ");
		}
		builder.append("successful=");
		builder.append(successful);
		if ( message != null ) {
			builder.append(", message=");
			builder.append(message);
		}
		builder.append("}");
		return builder.toString();
	}

}

/* ==================================================================
 * GpsdReportMessage.java - 16/11/2019 11:43:16 am
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

/**
 * Marker interface for status report messages.
 * 
 * @author matt
 * @version 1.0
 */
public interface GpsdReportMessage extends GpsdMessage {

	/**
	 * Get the report timestamp.
	 * 
	 * @return the timestamp, if available
	 */
	Instant getTimestamp();

	/**
	 * Create a copy of this report with a new timestamp.
	 * 
	 * @param <T>
	 *        the message type
	 * @param timestamp
	 *        the timestamp
	 * @return the new report message
	 */
	GpsdReportMessage withTimestamp(Instant timestamp);

}

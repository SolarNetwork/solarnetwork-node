/* ==================================================================
 * GpsdMessage.java - 11/11/2019 9:33:21 pm
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

/**
 * API for a GPSd message.
 * 
 * @author matt
 * @version 1.0
 */
public interface GpsdMessage {

	/**
	 * Get the message type.
	 * 
	 * @return the message type
	 */
	GpsdMessageType getMessageType();

	/**
	 * Get the unique message name.
	 * 
	 * @return the message name
	 */
	String getMessageName();

}

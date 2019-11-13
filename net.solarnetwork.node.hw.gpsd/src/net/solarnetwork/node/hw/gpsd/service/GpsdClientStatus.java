/* ==================================================================
 * GpsdClientStatus.java - 13/11/2019 10:02:36 am
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

package net.solarnetwork.node.hw.gpsd.service;

/**
 * Status for a GPSd client.
 * 
 * @author matt
 * @version 1.0
 */
public enum GpsdClientStatus {

	/**
	 * The connection to the GPSd server is closed and the client will not
	 * reconnect.
	 */
	Closed,

	/** The client is connected to the GPSd server. */
	Connected,

	/**
	 * The client is not connected but will attempt to connect in the future.
	 */
	ConnectionScheduled;

}

/* ==================================================================
 * GpsdClientConnection.java - 14/11/2019 6:40:46 am
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

package net.solarnetwork.node.io.gpsd.service;

import java.util.concurrent.Future;
import net.solarnetwork.node.io.gpsd.domain.VersionMessage;
import net.solarnetwork.node.io.gpsd.domain.WatchMessage;

/**
 * API for a GPSd client.
 * 
 * @author matt
 * @version 1.0
 */
public interface GpsdClientConnection extends GpsdMessageBroker {

	/**
	 * Get the status of the client connection.
	 * 
	 * @return the status
	 */
	GpsdClientStatus getClientStatus();

	/**
	 * Get the GPSd version information.
	 * 
	 * @return the version
	 */
	Future<VersionMessage> requestGpsdVersion();

	/**
	 * Configure and enable/disable "watch" mode to receive future
	 * {@literal SKY} and {@literal TPV} messages on any configured message
	 * listeners.
	 * </p>
	 * 
	 * @param config
	 *        the watch mode configuration to set
	 * @return the watch configuration response
	 */
	Future<WatchMessage> configureWatchMode(WatchMessage config);

}

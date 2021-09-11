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
import net.solarnetwork.node.io.gpsd.domain.GpsdMessage;
import net.solarnetwork.node.io.gpsd.domain.GpsdReportMessage;
import net.solarnetwork.node.io.gpsd.domain.VersionMessage;
import net.solarnetwork.node.io.gpsd.domain.WatchMessage;
import net.solarnetwork.service.Identifiable;

/**
 * API for a GPSd client.
 * 
 * @author matt
 * @version 2.0
 */
public interface GpsdClientConnection extends GpsdMessageBroker, Identifiable {

	/**
	 * An {@link org.osgi.service.event.Event} topic for when a
	 * {@link GpsdClientStatus} changes.
	 * 
	 * <p>
	 * The following event properties will be available:
	 * </p>
	 * 
	 * <ul>
	 * <li>{@link net.solarnetwork.domain.Identifiable#UID_PROPERTY}</li>
	 * <li>{@link net.solarnetwork.domain.Identifiable#GROUP_UID_PROPERTY} - if
	 * available</li>
	 * <li>{@link #STATUS_PROPERTY}</li>
	 * </ul>
	 */
	String EVENT_TOPIC_CLIENT_STATUS_CHANGE = "net/solarnetwork/node/io/gpsd/CLIENT_STATUS_CHANGE";

	/**
	 * The event topic for when a {@link GpsdReportMessage} is received.
	 * 
	 * <p>
	 * The following event properties will be available:
	 * </p>
	 * 
	 * <ul>
	 * <li>{@link net.solarnetwork.domain.Identifiable#UID_PROPERTY}</li>
	 * <li>{@link net.solarnetwork.domain.Identifiable#GROUP_UID_PROPERTY} - if
	 * available</li>
	 * <li>{@link #MESSAGE_PROPERTY}</li>
	 * </ul>
	 */
	String EVENT_TOPIC_REPORT_MESSAGE_CAPTURED = "net/solarnetwork/node/io/gpsd/REPORT_MESSAGE_CAPTURED";

	/** The event property for a {@link GpsdClientStatus} instance. */
	String STATUS_PROPERTY = "status";

	/** The event property for a {@link GpsdMessage} instance. */
	String MESSAGE_PROPERTY = "message";

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

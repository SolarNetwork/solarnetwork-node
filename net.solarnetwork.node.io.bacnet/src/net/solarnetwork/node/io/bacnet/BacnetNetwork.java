/* ==================================================================
 * BacnetNetwork.java - 1/11/2022 5:33:34 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.bacnet;

import net.solarnetwork.service.Identifiable;

/**
 * High level BACnet entry point API.
 * 
 * <p>
 * This API aims to simplify accessing BACnet devices without having any direct
 * dependency on any specific BACnet implementation. A {@code BacnetNetwork} is
 * a service that provides the entry point into the underlying BACnet network.
 * It is expected that implementations of this API are configured with
 * appropriate {@link Identifiable} properties and registered at runtime so that
 * clients wishing to interact with BACnet are able to request a suitably
 * configured service.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface BacnetNetwork extends Identifiable {

	/**
	 * Create a connection to a specific BACnet.
	 * 
	 * <p>
	 * The returned connection will not be opened and must be closed when
	 * finished being used.
	 * </p>
	 * 
	 * @return a new connection, or {@literal null} if a connection cannot be
	 *         created, such as from lack of required configuration
	 * @throws IllegalArgumentException
	 *         if {@code busName} is invalid
	 */
	BacnetConnection createConnection();

}

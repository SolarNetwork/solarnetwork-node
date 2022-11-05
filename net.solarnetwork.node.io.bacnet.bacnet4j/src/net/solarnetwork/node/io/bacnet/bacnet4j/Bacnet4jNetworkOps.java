/* ==================================================================
 * Bacnet4jNetworkOps.java - 5/11/2022 8:35:11 am
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

package net.solarnetwork.node.io.bacnet.bacnet4j;

import java.util.Collection;
import net.solarnetwork.node.io.bacnet.BacnetConnection;
import net.solarnetwork.node.io.bacnet.BacnetDeviceObjectPropertyRef;

/**
 * BACnet4J internal network operations API.
 * 
 * @author matt
 * @version 1.0
 */
public interface Bacnet4jNetworkOps {

	/**
	 * Get a description of the network.
	 * 
	 * @return the description
	 */
	String getNetworkDescription();

	/**
	 * Get another unique subscription ID value.
	 * 
	 * @return a unique subscription ID
	 */
	int nextSubscriptionId();

	/**
	 * Register a subscription for unconfirmed property change-of-value events.
	 * 
	 * @param subscriptionId
	 *        the subscription ID
	 * @param refs
	 *        the properties to subscribe to COV changes
	 * @param maxDelay
	 *        the maximum delay of events, in seconds
	 */
	void covSubscribe(int subscriptionId, Collection<BacnetDeviceObjectPropertyRef> refs, int maxDelay);

	/**
	 * Release a connection.
	 * 
	 * @param conn
	 *        the connection to release
	 */
	void releaseConnection(BacnetConnection conn);

}

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
import java.util.Map;
import java.util.function.Consumer;
import net.solarnetwork.node.io.bacnet.BacnetConnection;
import net.solarnetwork.node.io.bacnet.BacnetCovHandler;
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
	 * Add a handler to receive change-of-value property notifications.
	 * 
	 * @param handler
	 *        the handler to add
	 */
	void addCovHandler(BacnetCovHandler handler);

	/**
	 * Remove a handler previously registered with
	 * {@link #addCovHandler(Consumer)}.
	 * 
	 * @param handler
	 *        the handler to remove
	 */
	void removeCovHandler(BacnetCovHandler handler);

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
	 * Unregister a subscription for unconfirmed property change-of-value
	 * events.
	 * 
	 * @param subscriptionId
	 *        the subscription ID to unregister
	 */
	void covUnsubscribe(int subscriptionId);

	/**
	 * Release a connection.
	 * 
	 * @param conn
	 *        the connection to release
	 */
	void releaseConnection(BacnetConnection conn);

	/**
	 * Read a set of property values.
	 * 
	 * @param refs
	 *        the property references to read
	 * @return the associated values, never {@literal null}
	 */
	public Map<BacnetDeviceObjectPropertyRef, ?> propertyValues(
			Collection<BacnetDeviceObjectPropertyRef> refs);

}

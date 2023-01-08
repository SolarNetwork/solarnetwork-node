/* ==================================================================
 * BacnetConnection.java - 1/11/2022 5:49:00 pm
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

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * High level BACnet connection API.
 * 
 * @author matt
 * @version 1.0
 */
public interface BacnetConnection extends Closeable {

	/**
	 * Open the connection, if it is not already open.
	 * 
	 * <p>
	 * The connection must be opened before calling any of the other methods in
	 * this API. The {@link #close()} method must be called when the connection
	 * is longer needed.
	 * </p>
	 * 
	 * @throws IOException
	 *         if the connection cannot be opened
	 */
	void open() throws IOException;

	/**
	 * Test if the connection has been established.
	 * 
	 * @return {@literal true} if the connection has been established,
	 *         {@literal false} if the connection has never been opened or has
	 *         been closed
	 */
	boolean isEstablished();

	/**
	 * Test if {@link #close()} has been called.
	 * 
	 * <p>
	 * This method does not necessarily verify if the physical connection has
	 * been terminated, it is merely an indication if {@link #close()} has been
	 * invoked.
	 * </p>
	 * 
	 * @return {@literal true} if {@link #close()} has been invoked on this
	 *         connection
	 */
	boolean isClosed();

	/**
	 * Add a handler to receive change-of-value property notifications.
	 * 
	 * <p>
	 * The handler will be passed the subscription ID and any associated
	 * property updates.
	 * </p>
	 * 
	 * @param handler
	 *        the handler to add
	 */
	void addCovHandler(BacnetCovHandler handler);

	/**
	 * Remove a handler previously registered with
	 * {@link #addCovHandler(BacnetCovHandler)}.
	 * 
	 * @param handler
	 *        the handler to remove
	 */
	void removeCovHandler(BacnetCovHandler handler);

	/**
	 * Subscribe to unconfirmed change-of-value property notifications.
	 * 
	 * @param refs
	 *        the device object properties to subscribe to
	 * @param maxDelay
	 *        the maximum delay, in seconds, for changes to be published within
	 * @return the unique subscription ID
	 */
	int covSubscribe(Collection<BacnetDeviceObjectPropertyRef> refs, int maxDelay);

	/**
	 * Unsubscribe to unconfirmed change-of-value property notifications.
	 * 
	 * @param subscriptionId
	 *        the subscription ID to unsubscribe from, as previously returned
	 *        from {@link #covSubscribe(Collection, int)}
	 */
	void covUnsubscribe(int subscriptionId);

	/**
	 * Read property values.
	 * 
	 * @param refs
	 *        the property values to read
	 * @return the values
	 */
	Map<BacnetDeviceObjectPropertyRef, ?> propertyValues(Collection<BacnetDeviceObjectPropertyRef> refs);

	/**
	 * Write property values.
	 * 
	 * @param values
	 *        the property values to write
	 */
	void updatePropertyValues(Map<BacnetDeviceObjectPropertyRef, ?> values);

}

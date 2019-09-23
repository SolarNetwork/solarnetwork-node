/* ==================================================================
 * CanbusConnection.java - 19/09/2019 4:09:12 pm
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

package net.solarnetwork.node.io.canbus;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;

/**
 * High level CAN bus connection API.
 * 
 * @author matt
 * @version 1.0
 * @see CanbusNetwork for the main entry point to acquiring connection instances
 */
public interface CanbusConnection extends Closeable {

	/** A data filter representing "no filter". */
	long DATA_FILTER_NONE = 0;

	/**
	 * Get the CAN bus name the connection uses.
	 * 
	 * @return the bus name
	 */
	String getBusName();

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
	 * Subscribe to CAN bus changes.
	 * 
	 * <p>
	 * The {@code dataFilter} argument represents a 8-byte bitmask to filter CAN
	 * bus messages by, such that only CAN frame data that changes within the
	 * specified mask will result in a call back to {@code listener}.
	 * </p>
	 * 
	 * <p>
	 * Subscriptions are required to be unique only per address on a given
	 * connection. The behavior when subscribing to the same address with
	 * different filters is implementation specific.
	 * </p>
	 * 
	 * @param address
	 *        the CAN address to subscribe to
	 * @param forceExtendedAddress
	 *        {@literal true} to force {@code address} to be treated as an
	 *        extended address, even it if would otherwise fit
	 * @param limit
	 *        an optional rate to limit update messages to
	 * @param dataFilter
	 *        a bitmask filter to limit update message to, or {@literal 0} to
	 *        receive all frames
	 * @param listener
	 *        the listener to be notified of frame changes
	 */
	void subscribe(int address, boolean forceExtendedAddress, Duration limit, long dataFilter,
			CanbusFrameListener listener) throws IOException;

	/**
	 * Subscribe to multiplexed CAN bus changes.
	 * 
	 * <p>
	 * Multiplexed subscriptions can be used when a single CAN bus address can
	 * emit different types of data, using some portion of the data to represent
	 * a "type" identifier. For example the first byte of the frame data might
	 * represent the multiplex identifier, with the remainder of the frame data
	 * holding the logical content.
	 * </p>
	 * 
	 * <p>
	 * Each {@code dataFilters} value represents a 8-byte bitmask to filter CAN
	 * bus messages by, such that only CAN frame data that changes within the
	 * specified mask will result in a call back to {@code listener}. The
	 * {@code identifierMask} is a bitmask that represents
	 * </p>
	 * 
	 * <p>
	 * Subscriptions are required to be unique only per address on a given
	 * connection. The behavior when subscribing to the same address with
	 * different filters is implementation specific.
	 * </p>
	 * 
	 * @param address
	 *        the CAN address to subscribe to
	 * @param forceExtendedAddress
	 *        {@literal true} to force {@code address} to be treated as an
	 *        extended address, even it if would otherwise fit
	 * @param limit
	 *        an optional rate to limit update messages to
	 * @param identifierMask
	 *        an 8-byte bitmask that represents which data bits represent the
	 *        multiplex identifier
	 * @param dataFilters
	 *        a list of bitmask filters to limit update message to; must have at
	 *        least one value
	 * @param listener
	 *        the listener to be notified of frame changes
	 */
	void subscribe(int address, boolean forceExtendedAddress, Duration limit, long identifierMask,
			Iterable<Long> dataFilters, CanbusFrameListener listener) throws IOException;

	/**
	 * Unsubscribe from CAN bus changes.
	 * 
	 * <p>
	 * After calling this message, any {@code CanbusFrameListener} previously
	 * subscribed on {@code address} should stop receiving CAN bus changes.
	 * </p>
	 * 
	 * @param address
	 *        the CAN address to unsubscribe from
	 * @param forceExtendedAddress
	 *        {@literal true} to force {@code address} to be treated as an
	 *        extended address, even it if would otherwise fit
	 */
	void unsubscribe(int address, boolean forceExtendedAddress) throws IOException;

	/**
	 * Monitor all unfiltered CAN bus changes.
	 * 
	 * <p>
	 * After this method is called, any registered subscriptions are suspended
	 * and only {@code listener} will receive CAN bus changes.
	 * 
	 * @param listener
	 *        the listener to be notified of frame changes
	 */
	void monitor(CanbusFrameListener listener) throws IOException;

	/**
	 * Stop monitoring all unfiltered CAN bus changes.
	 * 
	 * <p>
	 * After this method is called, any registered subscriptions are resumed.
	 * </p>
	 */
	void unmonitor() throws IOException;

}

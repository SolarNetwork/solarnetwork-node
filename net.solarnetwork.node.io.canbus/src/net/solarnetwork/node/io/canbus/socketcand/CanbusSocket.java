/* ==================================================================
 * CanbusSocket.java - 23/09/2019 5:31:04 pm
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

package net.solarnetwork.node.io.canbus.socketcand;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * API for a CAN bus socket.
 * 
 * <p>
 * This API is used to enable unit testing primarily.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface CanbusSocket extends Closeable {

	/**
	 * Open the connection.
	 * 
	 * <p>
	 * The connection might be configured in a preliminary state. Once a
	 * successful response has been confirmed from the server, the
	 * {@link #connectionConfirmed()} method to signal to the socket that it can
	 * be configured for normal operations.
	 * </p>
	 * 
	 * @param host
	 *        the host name or IP address
	 * @param port
	 *        the port
	 * @throws IOException
	 *         if any IO error occurs
	 */
	void open(String host, int port) throws IOException;

	/**
	 * Confirm that the connection has been established by the server correctly.
	 * 
	 * @throws IOException
	 *         if any IO error occurs
	 */
	void connectionConfirmed() throws IOException;

	/**
	 * Test if the connection has been established.
	 * 
	 * <p>
	 * This will not return {@literal true} unless
	 * {@link #connectionConfirmed()} has been invoked.
	 * </p>
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
	 * Read the next message from the socket, blocking until another message is
	 * available.
	 * 
	 * @param timeout
	 *        an optional maximum amount of time to wait, if greater than zero
	 * @param unit
	 *        the time unit for {@code timeout}
	 * @return the next message
	 * @throws IOException
	 *         if any IO error occurs
	 */
	Message nextMessage(long timeout, TimeUnit unit) throws IOException;

	/**
	 * Write a message to the socket.
	 * 
	 * @param message
	 *        the message to write
	 * @throws IOException
	 *         if any IO error occurs
	 */
	void writeMessage(Message message) throws IOException;

}

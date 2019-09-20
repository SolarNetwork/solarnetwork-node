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

/**
 * High level CAN bus connection API.
 * 
 * @author matt
 * @version 1.0
 * @see CanbusNetwork for the main entry point to acquiring connection instances
 */
public interface CanbusConnection extends Closeable {

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

}

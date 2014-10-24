/* ==================================================================
 * SerialConnection.java - Oct 23, 2014 2:10:45 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.serial;

import java.io.IOException;
import net.solarnetwork.node.LockTimeoutException;

/**
 * @author matt
 * @version 1.0
 */
public interface SerialConnection {

	/**
	 * Open the connection, if it is not already open. The connection must be
	 * opened before calling any of the other methods in this API.
	 * 
	 * @throws IOException
	 *         if the connection cannot be opened
	 */
	void open() throws IOException, LockTimeoutException;

	/**
	 * Close the connection, if it is open.
	 */
	void close();

	/**
	 * Read a message that is marked by start and end "magic" bytes. The
	 * returned bytes will include both the start and end marker bytes.
	 * 
	 * @param startMarker
	 *        the starting byte sequence
	 * @param endMarker
	 *        the ending byte sequence
	 * @return the message bytes
	 * @throws IOException
	 *         if the connection fails
	 */
	byte[] readMarkedMessage(byte[] startMarker, byte[] endMarker) throws IOException;

}

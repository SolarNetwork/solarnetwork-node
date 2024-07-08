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

import java.io.Closeable;
import java.io.IOException;
import net.solarnetwork.node.service.LockTimeoutException;

/**
 * API for a serial connection.
 *
 * @author matt
 * @version 2.0
 */
public interface SerialConnection extends Closeable {

	/**
	 * Get the name of the serial port used by this connection.
	 *
	 * @return the serial port name, or {@literal null} if not known
	 * @since 1.1
	 */
	String getPortName();

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
	@Override
	void close();

	/**
	 * Read a message that is marked by start and end "magic" bytes. The
	 * returned bytes will include both the start and end marker bytes.
	 *
	 * @param startMarker
	 *        the starting byte sequence
	 * @param endMarker
	 *        the ending byte sequence
	 * @return the message bytes, <b>including</b> {@code startMarker} and
	 *         {@code endMarker}
	 * @throws IOException
	 *         if the connection fails
	 */
	byte[] readMarkedMessage(byte[] startMarker, byte[] endMarker) throws IOException;

	/**
	 * Read a message that is marked by some starting "magic" bytes and has a
	 * fixed length;
	 *
	 * @param startMarker
	 *        the starting byte sequence
	 * @param length
	 *        the length of the message to read, <b>including</b> the length of
	 *        {@code startMarker}
	 * @return the message bytes, <b>including</b> {@code startMarker}
	 * @throws IOException
	 *         if the connection fails
	 */
	byte[] readMarkedMessage(byte[] startMarker, int length) throws IOException;

	/**
	 * Write a message.
	 *
	 * @param message
	 *        the message to write
	 * @throws IOException
	 *         if the connection fails
	 */
	void writeMessage(byte[] message) throws IOException;

	/**
	 * Drain the input buffer until it is empty.
	 *
	 * @throws IOException
	 *         if the connection fails
	 * @return the drained bytes (never {@literal null})
	 */
	byte[] drainInputBuffer() throws IOException;

}


package net.solarnetwork.node.io.modbus;

import net.solarnetwork.node.LockTimeoutException;
import net.wimpi.modbus.net.SerialConnection;

/* ==================================================================
 * ModbusSerialConnectionFactory.java - Jul 10, 2013 7:33:43 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

/**
 * Factory for Modbus SerialConnection objects.
 * 
 * @author matt
 * @version 1.0
 */
public interface ModbusSerialConnectionFactory {

	/**
	 * Get a unique identifier for this factory.
	 * 
	 * <p>
	 * This should be meaningful to the factory implementation. For example a
	 * serial port based implementation could use the port identifier as the
	 * UID.
	 * </p>
	 * 
	 * @return unique identifier
	 */
	String getUID();

	/**
	 * Get a configured and open SerialConnection.
	 * 
	 * <p>
	 * The returned connection will be opened. If the connection cannot be
	 * opened, a {@link RuntimeException} will be thrown. The caller should
	 * always call {@link SerialConnection#close()} to free up resources when
	 * finished.
	 * </p>
	 * 
	 * @return the connection
	 * @throws LockTimeoutException
	 *         if cannot obtain the connection because another thread has
	 *         already obtained it
	 */
	SerialConnection getSerialConnection();

	/**
	 * Perform some work with a Modbus {@link SerialConnection}.
	 * 
	 * <p>
	 * This is a convenient way to open the connection, perform some work, and
	 * have the connection automatically closed for you.
	 * </p>
	 * 
	 * <p>
	 * This method attempts to obtain a {@link SerialConnection} via
	 * {@link #getSerialConnection()}. If the connection is obtained, it will
	 * call {@link ModbusConnectionCallback#doInConnection(SerialConnection)},
	 * and then close the connection when finished.
	 * </p>
	 * 
	 * <p>
	 * <b>Note</b> that if either the connection factory is unavailable, or it
	 * fails to return a connection, the callback method will never be called.
	 * </p>
	 * 
	 * @param action
	 *        the connection callback
	 * @return the result of the callback, or <em>null</em> if the callback is
	 *         never invoked
	 */
	<T> T execute(ModbusConnectionCallback<T> action);

}

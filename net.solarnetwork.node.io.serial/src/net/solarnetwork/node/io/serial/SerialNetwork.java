/* ==================================================================
 * SerialNetwork.java - Oct 23, 2014 2:10:11 PM
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
import net.solarnetwork.service.Identifiable;

/**
 * High level serial network API.
 * 
 * <p>
 * This API aims to simplify accessing serial capable devices without having any
 * direct dependency on RXTX (or any other serial implementation).
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public interface SerialNetwork extends Identifiable {

	/**
	 * Get the name of the serial port used by this connection.
	 * 
	 * @return the serial port name, or {@literal null} if not known
	 * @since 1.1
	 */
	String getPortName();

	/**
	 * Perform some action that requires a {@link SerialConnection}, returning
	 * the result. The
	 * {@link SerialConnectionAction#doWithConnection(SerialConnection)} method
	 * will be called and the result returned by this method.
	 * 
	 * The {@link SerialConnection} passed will already be opened, and it will
	 * be closed automatically after the action is complete.
	 * 
	 * @param <T>
	 *        the action result type
	 * @param action
	 *        the callback whose result to return
	 * @return the result of calling
	 *         {@link SerialConnectionAction#doWithConnection(SerialConnection)}
	 * @throws IOException
	 *         if any IO error occurs
	 */
	<T> T performAction(SerialConnectionAction<T> action) throws IOException;

	/**
	 * Create a connection to a specific Serial device. The returned connection
	 * will not be opened and must be closed when finished being used.
	 * 
	 * @return a new connection
	 */
	SerialConnection createConnection();

}

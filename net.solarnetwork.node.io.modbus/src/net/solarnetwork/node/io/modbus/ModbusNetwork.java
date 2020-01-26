/* ==================================================================
 * ModbusNetwork.java - Jul 29, 2014 11:17:48 AM
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

package net.solarnetwork.node.io.modbus;

import java.io.IOException;
import net.solarnetwork.node.Identifiable;

/**
 * High level Modbus API.
 * 
 * <p>
 * This API aims to simplify accessing Modbus capable devices without having any
 * direct dependency on any specific Modbus implementation.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public interface ModbusNetwork extends Identifiable {

	/**
	 * Perform some action that requires a {@link ModbusConnection}, returning
	 * the result.
	 * 
	 * <p>
	 * The {@link ModbusConnectionAction#doWithConnection(ModbusConnection)}
	 * method will be called and the result returned by this method. The
	 * {@link ModbusConnection} passed will already be opened, and it will be
	 * closed automatically after the action is complete.
	 * </p>
	 * 
	 * @param unitId
	 *        the Modbus unit ID to address
	 * @param action
	 *        the callback whose result to return
	 * 
	 * @return the result of calling
	 *         {@link ModbusConnectionAction#doWithConnection(ModbusConnection)}
	 * @throws IOException
	 *         if any IO error occurs
	 */
	<T> T performAction(int unitId, ModbusConnectionAction<T> action) throws IOException;

	/**
	 * Create a connection to a specific Modbus device. The returned connection
	 * will not be opened and must be closed when finished being used.
	 * 
	 * @param unitId
	 *        the Modbus unit ID to connect with
	 * @return a new connection
	 */
	ModbusConnection createConnection(int unitId);

}

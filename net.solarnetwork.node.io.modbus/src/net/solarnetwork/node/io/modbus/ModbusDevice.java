/* ==================================================================
 * ModbusDevice.java - Jul 29, 2014 11:17:48 AM
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
 * direct dependency on Jamod (or any other Modbus implementation).
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public interface ModbusDevice extends Identifiable {

	/**
	 * Get the Modbus Unit ID this device represents.
	 * 
	 * @return the unit ID
	 */
	int getUnitId();

	/**
	 * Perform some action that requires a {@link ModbusConnection}, returning
	 * the result. The
	 * {@link ModbusConnectionAction#doWithConnection(ModbusConnection)} method
	 * will be called and the result returned by this method.
	 * 
	 * @param action
	 *        the callback whose result to return
	 * @return the result of calling
	 *         {@link ModbusConnectionAction#doWithConnection(ModbusConnection)}
	 * @throws IOException
	 *         if any IO error occurs
	 */
	<T> T performAction(ModbusConnectionAction<T> action) throws IOException;

}

/* ==================================================================
 * ModbusConnectionAction.java - Jul 29, 2014 12:44:57 PM
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

/**
 * Callback API for performing an action with a {@link ModbusConnection}.
 * 
 * <p>
 * If no result object is needed, simply use {@link Object} as the parameter
 * type and return <em>null</em> from
 * {@link #doWithConnection(ModbusConnection)}.
 * </p>
 * 
 * @param <T>
 *        the action return type
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public interface ModbusConnectionAction<T> {

	/**
	 * Perform an action with a {@link ModbusConnection}. If no result object is
	 * needed, simply return <em>null</em>.
	 * 
	 * @param conn
	 *        the connection
	 * @return the result
	 * @throws IOException
	 */
	T doWithConnection(ModbusConnection conn) throws IOException;

}

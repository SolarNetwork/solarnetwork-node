/* ==================================================================
 * SerialConnectionAction.java - Oct 23, 2014 2:11:01 PM
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

/**
 * Callback API for performing an action with a {@link SerialConnection}.
 * 
 * <p>
 * If no result object is needed, simply use {@link Object} as the parameter
 * type and return {@literal null} from
 * {@link #doWithConnection(SerialConnection)}.
 * </p>
 * 
 * @param <T>
 *        the action return type
 * @author matt
 * @version 1.1
 */
@FunctionalInterface
public interface SerialConnectionAction<T> {

	/**
	 * Perform an action with a {@link SerialConnection}. If no result object is
	 * needed, simply return {@literal null}.
	 * 
	 * @param conn
	 *        the connection
	 * @return the result
	 * @throws IOException
	 *         if any IO error occurs
	 */
	T doWithConnection(SerialConnection conn) throws IOException;

}

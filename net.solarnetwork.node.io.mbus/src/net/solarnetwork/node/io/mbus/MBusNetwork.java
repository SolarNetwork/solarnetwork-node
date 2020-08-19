/* ==================================================================
 * MBusNetwork.java - 8/05/2020 12:18:18 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.mbus;

import java.io.IOException;
import net.solarnetwork.node.Identifiable;

/**
 * High level M-Bus network API.
 * 
 * @author matt
 * @version 1.0
 */
public interface MBusNetwork extends Identifiable {

	/**
	 * Read data from the connection
	 * 
	 * @param address
	 *        Primary address
	 * @return M-Bus data
	 */
	MBusData read(int address) throws IOException;

	/**
	 * Create a connection to a wired M-Bus network. The returned connection
	 * will not be opened and must be closed when finished being used.
	 * 
	 * @param address
	 *        Primary address
	 * @return a new connection
	 */
	MBusConnection createConnection(int address);

}

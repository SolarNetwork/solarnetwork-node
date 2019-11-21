/* ==================================================================
 * CannelloniCanbusConnectionTest.java - 21/11/2019 3:00:57 pm
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

package net.solarnetwork.node.io.canbus.cannelloni.test;

import net.solarnetwork.node.io.canbus.cannelloni.CannelloniCanbusConnection;

/**
 * Test cases for the {@link CannelloniCanbusConnection} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CannelloniCanbusConnectionTest {

	public static void main(String[] args) throws Exception {
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		String bus = args[2];
		try (CannelloniCanbusConnection conn = new CannelloniCanbusConnection(null, bus, host, port)) {
			conn.open();
			System.in.read();
		}
	}

}

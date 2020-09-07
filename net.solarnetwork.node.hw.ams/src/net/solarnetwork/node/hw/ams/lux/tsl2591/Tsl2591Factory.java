/* ==================================================================
 * Tsl2591Factory.java - 1/09/2020 6:59:05 AM
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

package net.solarnetwork.node.hw.ams.lux.tsl2591;

/**
 * Factory for TSL25911 implementations.
 * 
 * @author matt
 * @version 1.0
 */
public final class Tsl2591Factory {

	public static final int DEFAULT_ADDRESS = 0x29;

	/**
	 * Create an operations instance.
	 * 
	 * @param devicePath
	 *        the device path
	 * @return the operations
	 */
	public static Tsl2591Operations createOperations(String devicePath) {
		return new Tsl2591Helper(devicePath, DEFAULT_ADDRESS);
	}

}

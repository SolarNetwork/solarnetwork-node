/* ==================================================================
 * ModbusReference.java - 15/05/2018 11:04:04 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

/**
 * A reference to a Modbus register (or registers).
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface ModbusReference {

	/**
	 * Get the register address.
	 * 
	 * @return the address
	 */
	int getAddress();

	/**
	 * Get the data type.
	 * 
	 * @return the data type
	 */
	public ModbusDataType getDataType();

	/**
	 * Get the read function for accessing the register.
	 * 
	 * @return the read function
	 */
	public ModbusReadFunction getFunction();

}

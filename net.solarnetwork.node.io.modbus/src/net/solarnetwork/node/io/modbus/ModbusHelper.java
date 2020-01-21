/* ==================================================================
 * ModbusHelper.java - Jul 15, 2013 7:54:17 AM
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

package net.solarnetwork.node.io.modbus;

/**
 * Helper methods for working with Modbus serial connection.
 * 
 * @author matt
 * @version 2.0
 */
public final class ModbusHelper {

	/**
	 * Get a {@link ModbusFunction} for a code value.
	 * 
	 * @param code
	 *        the code
	 * @return the function
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 * @since 1.5
	 */
	public static ModbusFunction functionForCode(int code) {
		ModbusFunction f;
		try {
			f = ModbusReadFunction.forCode(code);
		} catch ( IllegalArgumentException e ) {
			try {
				f = ModbusWriteFunction.forCode(code);
			} catch ( IllegalArgumentException e2 ) {
				throw new IllegalArgumentException("Unknown Modbus function code: " + code);
			}
		}
		return f;
	}

}

/* ==================================================================
 * ModbusFunction.java - 11/03/2018 8:43:19 AM
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
 * API for a modbus function.
 * 
 * @author matt
 * @version 2.0
 * @since 2.5
 */
public interface ModbusFunction {

	/**
	 * Get the function code.
	 * 
	 * @return the code
	 */
	int getCode();

	/**
	 * Get a friendly display string for this function.
	 * 
	 * @return a display string
	 */
	String toDisplayString();

	/**
	 * Return {@literal true} if this function represents a read operation.
	 * 
	 * @return {@literal true} if this function represents a read operation,
	 *         {@literal false} if a write operation
	 */
	boolean isReadFunction();

	/**
	 * Get an "opposite" function from this function.
	 * 
	 * <p>
	 * This method is used to get a read function for a given write function,
	 * and a write function for a given read function.
	 * </p>
	 * 
	 * @return the function, or {@literal null} if not applicable
	 */
	ModbusFunction oppositeFunction();

	/**
	 * Get a {@link ModbusFunction} for a code value.
	 * 
	 * @param code
	 *        the code
	 * @return the function
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 * @since 2.0
	 */
	static ModbusFunction functionForCode(int code) {
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

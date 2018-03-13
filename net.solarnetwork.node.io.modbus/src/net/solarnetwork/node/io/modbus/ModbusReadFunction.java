/* ==================================================================
 * ModbusReadFunction.java - 21/12/2017 2:48:46 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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
 * Modbus read functions.
 * 
 * @author matt
 * @version 1.0
 * @since 2.5
 */
public enum ModbusReadFunction implements ModbusFunction {

	ReadCoil(1),

	ReadDiscreteInput(2),

	ReadHoldingRegister(3),

	ReadInputRegister(4);

	private int code;

	private ModbusReadFunction(int code) {
		this.code = code;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String toDisplayString() {
		return this.toString() + " (" + this.code + ")";
	}

}

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
 * Modbus write functions.
 * 
 * <p>
 * These functions are separated from the read functions to help API methods be
 * explicit about requiring write versus read functions.
 * </p>
 * 
 * @author matt
 * @version 2.1
 * @since 2.5
 */
public enum ModbusWriteFunction implements ModbusFunction {

	/** Write a coil register. */
	WriteCoil(5, ModbusRegisterBlockType.Coil),

	/** Write a holding register. */
	WriteHoldingRegister(6, ModbusRegisterBlockType.Holding),

	/** Write multiple coils. */
	WriteMultipleCoils(15, ModbusRegisterBlockType.Coil),

	/** Write multiple holding. */
	WriteMultipleHoldingRegisters(16, ModbusRegisterBlockType.Holding);

	private final int code;
	private final ModbusRegisterBlockType blockType;

	private ModbusWriteFunction(int code, ModbusRegisterBlockType blockType) {
		this.code = code;
		this.blockType = blockType;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String toDisplayString() {
		return this.toString().replaceAll("([a-z])([A-Z])", "$1 $2") + " (" + this.code + ")";
	}

	/**
	 * Get an enum instance for a code value.
	 * 
	 * @param code
	 *        the code
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code code} is not a valid value
	 */
	public static ModbusWriteFunction forCode(int code) {
		for ( ModbusWriteFunction e : ModbusWriteFunction.values() ) {
			if ( code == e.code ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown ModbusWriteFunction code [" + code + "]");
	}

	@Override
	public boolean isReadFunction() {
		return false;
	}

	@Override
	public ModbusFunction oppositeFunction() {
		switch (this) {
			case WriteCoil:
			case WriteMultipleCoils:
				return ModbusReadFunction.ReadCoil;

			case WriteHoldingRegister:
			case WriteMultipleHoldingRegisters:
				return ModbusReadFunction.ReadHoldingRegister;

			default:
				return null;
		}
	}

	@Override
	public ModbusRegisterBlockType blockType() {
		return blockType;
	}

}

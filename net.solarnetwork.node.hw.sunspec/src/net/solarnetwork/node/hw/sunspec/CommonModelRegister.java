/* ==================================================================
 * CommonModelRegister.java - 21/05/2018 5:14:54 PM
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

package net.solarnetwork.node.hw.sunspec;

import static net.solarnetwork.node.io.modbus.ModbusDataType.StringAscii;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Enumeration of Modbus register mappings for SunSpec compliant meters.
 * 
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public enum CommonModelRegister implements ModbusReference {

	/** Manufacturer name, as NULL-terminated string. */
	Manufacturer(0, StringAscii, 16),

	/** Meter model, as NULL-terminated string. */
	Model(16, StringAscii, 16),

	/** Meter options, as NULL-terminated string. */
	Options(32, StringAscii, 8),

	/** Meter version, as NULL-terminated string. */
	Version(40, StringAscii, 8),

	/** Serial number, as NULL-terminated string. */
	SerialNumber(48, StringAscii, 16),

	/** The device address, which is the Modbus unit ID. */
	DeviceAddress(64, UInt16);

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;

	private CommonModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private CommonModelRegister(int address, ModbusDataType dataType, int wordLength) {
		this.address = address;
		this.dataType = dataType;
		this.wordLength = wordLength;
	}

	@Override
	public int getAddress() {
		return address;
	}

	@Override
	public ModbusDataType getDataType() {
		return dataType;
	}

	@Override
	public ModbusReadFunction getFunction() {
		return ModbusReadFunction.ReadHoldingRegister;
	}

	/**
	 * Get the data type word length.
	 * 
	 * @return the word length
	 */
	@Override
	public int getWordLength() {
		return wordLength;
	}

}

/* ==================================================================
 * WebBoxPlantRegister.java - 11/09/2020 3:54:36 PM
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

package net.solarnetwork.node.hw.sma.modbus.webbox;

import static net.solarnetwork.node.io.modbus.ModbusDataType.Int32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt64;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Enumeration of Modbus register mappings for the SMA WebBox plant (unit ID 2).
 * 
 * @author matt
 * @version 1.0
 */
public enum WebBoxPlantRegister implements ModbusReference {

	/** Version number of the SMA Modbus profile. */
	ModbusProfileVersion(30001, UInt32),

	/** Device ID of the WebBox. */
	DeviceId(30003, UInt32),

	/** Counter that increments when data in the profile changes. */
	DataChange(30007, UInt32),

	/** Serial number of the WebBox. */
	SerialNumber(30057, UInt32),

	/** The plant time (Unix epoch UTC). */
	Date(30193, UInt32),

	/** The plant time zone (enum). */
	TimeZone(30195, UInt32),

	/** The total energy (E-Total), in Wh. */
	TotalYield(30513, UInt64),

	/** The total active power (Pac), in W. */
	ActivePowerTotal(30775, Int32),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;

	/** The starting Modbus address for the connected device list. */
	public static final int DEVICE_UNIT_IDS_STARTING_ADDRESS = 42109;

	private WebBoxPlantRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private WebBoxPlantRegister(int address, ModbusDataType dataType, int wordLength) {
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

	@Override
	public int getWordLength() {
		return wordLength;
	}

}

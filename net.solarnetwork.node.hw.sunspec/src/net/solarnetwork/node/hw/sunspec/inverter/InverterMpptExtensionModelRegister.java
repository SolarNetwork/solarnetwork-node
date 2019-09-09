/* ==================================================================
 * InverterMpptExtensionModelRegister.java - 6/09/2019 5:59:57 pm
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

package net.solarnetwork.node.hw.sunspec.inverter;

import static net.solarnetwork.node.hw.sunspec.DataClassification.Accumulator;
import static net.solarnetwork.node.hw.sunspec.DataClassification.Bitfield;
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.StringAscii;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for the SunSpec compliant MPPT
 * inverter extension model.
 * 
 * <p>
 * These mappings correspond to the SunSpec model number <b>160</b>.
 * </p>
 *
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public enum InverterMpptExtensionModelRegister implements SunspecModbusReference {

	/** Current scale factor, as *10^X. */
	ScaleFactorDcCurrent(0, Int16, ScaleFactor),

	/** Voltage scale factor, as *10^X. */
	ScaleFactorDcVoltage(1, Int16, ScaleFactor),

	/** Power scale factor, as *10^X. */
	ScaleFactorDcPower(2, Int16, ScaleFactor),

	/** Energy scale factor, as *10^X. */
	ScaleFactorDcEnergy(3, Int16, ScaleFactor),

	/** Global events bitmask, see {@link InverterMpptExtensionEvent}. */
	EventsBitmask(4, UInt32, Bitfield),

	/** The count of modules (repeating block). */
	ModuleCount(6, UInt16),

	/** The timestamp period. */
	TimestampPeriod(7, UInt16),

	/** Module input ID. */
	ModuleInputId(0, UInt16),

	/** Module name. */
	ModuleName(1, StringAscii, 8),

	/** Module DC current, in amps. */
	ModuleDcCurrent(9, UInt16),

	/** Module DC voltage, in volts. */
	ModuleDcVoltage(10, UInt16),

	/** Module DC power, in watts. */
	ModuleDcPower(11, UInt16),

	/** Module lifetime energy, in watt-hours. */
	ModuleLifetimeEnergy(12, UInt32, Accumulator),

	/** Module timestamp, in seconds. */
	ModuleTimestamp(14, UInt32),

	/** Module temperature, in degrees celsius. */
	ModuleTemperature(16, Int16),

	/** Module operating state, see {@link InverterOperatingState}. */
	ModuleOperatingState(17, UInt16),

	/** Module events, see {@link InverterMpptExtensionEvent}. */
	ModuleEvents(18, UInt32, Bitfield);

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;
	private final DataClassification classification;

	private InverterMpptExtensionModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private InverterMpptExtensionModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, dataType.getWordLength(), classification);
	}

	private InverterMpptExtensionModelRegister(int address, ModbusDataType dataType, int wordLength) {
		this(address, dataType, wordLength, null);
	}

	private InverterMpptExtensionModelRegister(int address, ModbusDataType dataType, int wordLength,
			DataClassification classification) {
		this.address = address;
		this.dataType = dataType;
		this.wordLength = wordLength;
		this.classification = classification;
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

	@Override
	public DataClassification getClassification() {
		return classification;
	}

}

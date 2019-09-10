/* ==================================================================
 * StringCombinerModelRegister.java - 10/09/2019 7:17:54 am
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

package net.solarnetwork.node.hw.sunspec.combiner;

import static net.solarnetwork.node.hw.sunspec.DataClassification.Accumulator;
import static net.solarnetwork.node.hw.sunspec.DataClassification.Bitfield;
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for the SunSpec compliant basic
 * string combiner model.
 * 
 * <p>
 * These mappings correspond to the SunSpec model numbers <b>402</b> and
 * <b>404</b>.
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
public enum StringCombinerAdvancedModelRegister implements SunspecModbusReference {

	/** Current scale factor, as *10^X. */
	ScaleFactorDcCurrent(0, Int16, ScaleFactor),

	/** Charge (amp hour) scale factor, as *10^X. */
	ScaleFactorDcCharge(1, Int16, ScaleFactor),

	/** Voltage scale factor, as *10^X. */
	ScaleFactorDcVoltage(2, Int16, ScaleFactor),

	/** Power scale factor, as *10^X. */
	ScaleFactorDcPower(3, Int16, ScaleFactor),

	/** Energy scale factor, as *10^X. */
	ScaleFactorDcEnergy(4, Int16, ScaleFactor),

	/** The maximum DC current rating, in amps. */
	DcCurrentMaxRating(5, UInt16),

	/** The count of inputs (repeating block). */
	InputCount(6, UInt16),

	/** Events, see {@link StringCombinerModelEvent}. */
	EventsBitmask(7, UInt32, Bitfield),

	/** Vendor events. */
	VendorEventsBitmask(9, UInt32, Bitfield),

	/** DC current, in amps. */
	DcCurrent(11, Int16),

	/** Total metered charge, in amp-hours. */
	DcCharge(12, UInt32, Accumulator),

	/** Output voltage, in volts. */
	DcVoltage(14, UInt16),

	/** Internal operating temperature, in degrees celsius. */
	Temperature(15, Int16),

	/** Output power, in watts. */
	DcPower(16, UInt16),

	/** Performance ratio, as a percentage. */
	DcPerformanceRatio(17, UInt16),

	/** Output energy, in watt-hours. */
	DcEnergy(18, UInt32, Accumulator),

	/** Input current scale factor, as *10^X. */
	ScaleFactorInputDcCurrent(20, Int16, ScaleFactor),

	/** Input charge (amp hour) scale factor, as *10^X. */
	ScaleFactorInputDcCharge(21, Int16, ScaleFactor),

	/** Input voltage scale factor, as *10^X. */
	ScaleFactorInputDcVoltage(22, Int16, ScaleFactor),

	/** Input power scale factor, as *10^X. */
	ScaleFactorInputDcPower(23, Int16, ScaleFactor),

	/** Input energy scale factor, as *10^X. */
	ScaleFactorInputDcEnergy(24, Int16, ScaleFactor),

	/** Input ID. */
	InputId(0, UInt16),

	/** Input events, see {@link StringCombinerEvent}. */
	InputEventsBitmask(1, UInt32, Bitfield),

	/** Input vendor events. */
	InputVendorEventsBitmask(3, UInt32, Bitfield),

	/** DC current, in amps. */
	InputDcCurrent(5, Int16),

	/** Total metered charge, in amp-hours. */
	InputDcCharge(6, UInt32, Accumulator),

	/** String input voltage, in volts. */
	InputDcVoltage(8, UInt16),

	/** String input power, in watts. */
	InputDcPower(9, UInt16),

	/** String input energy, in watt-hours. */
	InputDcEnergy(10, UInt32, Accumulator),

	/** String performance ratio, as a percentage. */
	InputDcPerformanceRatio(12, UInt16),

	/** Number of modules in this input string. */
	InputModuleCount(13, UInt16);

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;
	private final DataClassification classification;

	private StringCombinerAdvancedModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private StringCombinerAdvancedModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, dataType.getWordLength(), classification);
	}

	private StringCombinerAdvancedModelRegister(int address, ModbusDataType dataType, int wordLength) {
		this(address, dataType, wordLength, null);
	}

	private StringCombinerAdvancedModelRegister(int address, ModbusDataType dataType, int wordLength,
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

	@Override
	public int getWordLength() {
		return wordLength;
	}

	@Override
	public DataClassification getClassification() {
		return classification;
	}

}

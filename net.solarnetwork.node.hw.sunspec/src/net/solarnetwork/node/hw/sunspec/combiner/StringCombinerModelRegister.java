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
 * These mappings correspond to the SunSpec model numbers <b>401</b> and
 * <b>403</b>.
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
public enum StringCombinerModelRegister implements SunspecModbusReference {

	/** Current scale factor, as *10^X. */
	ScaleFactorDcCurrent(0, Int16, ScaleFactor),

	/** Charge (amp hour) scale factor, as *10^X. */
	ScaleFactorDcCharge(1, Int16, ScaleFactor),

	/** Voltage scale factor, as *10^X. */
	ScaleFactorDcVoltage(2, Int16, ScaleFactor),

	/** The maximum DC current rating, in amps. */
	DcCurrentMaxRating(3, UInt16),

	/** The count of inputs (repeating block). */
	InputCount(4, UInt16),

	/** Events, see {@link StringCombinerModelEvent}. */
	EventsBitmask(5, UInt32, Bitfield),

	/** Vendor events. */
	VendorEventsBitmask(7, UInt32, Bitfield),

	/** DC current, in amps. */
	DcCurrent(9, Int16),

	/** Total metered charge, in amp-hours. */
	DcCharge(10, UInt32, Accumulator),

	/** Output voltage, in volts. */
	DcVoltage(12, UInt16),

	/** Internal operating temperature, in degrees celsius. */
	Temperature(13, Int16),

	/** Current scale factor, as *10^X. */
	ScaleFactorInputDcCurrent(14, Int16, ScaleFactor),

	/** Charge (amp hour) scale factor, as *10^X. */
	ScaleFactorInputDcCharge(15, Int16, ScaleFactor),

	/** Input ID. */
	InputId(0, UInt16),

	/** Input events, see {@link StringCombinerModelEvent}. */
	InputEventsBitmask(1, UInt32, Bitfield),

	/** Input vendor events. */
	InputVendorEventsBitmask(3, UInt32, Bitfield),

	/** DC current, in amps. */
	InputDcCurrent(5, Int16),

	/** Total metered charge, in amp-hours. */
	InputDcCharge(6, UInt32, Accumulator);

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;
	private final DataClassification classification;

	private StringCombinerModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private StringCombinerModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, dataType.getWordLength(), classification);
	}

	private StringCombinerModelRegister(int address, ModbusDataType dataType, int wordLength) {
		this(address, dataType, wordLength, null);
	}

	private StringCombinerModelRegister(int address, ModbusDataType dataType, int wordLength,
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

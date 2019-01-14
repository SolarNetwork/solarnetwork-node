/* ==================================================================
 * IntegerInverterModelRegister.java - 5/10/2018 4:16:34 PM
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

package net.solarnetwork.node.hw.sunspec.inverter;

import static net.solarnetwork.node.hw.sunspec.DataClassification.Accumulator;
import static net.solarnetwork.node.hw.sunspec.DataClassification.Bitfield;
import static net.solarnetwork.node.hw.sunspec.DataClassification.Enumeration;
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for SunSpec compliant integer
 * inverter models.
 * 
 * <p>
 * The integer inverter models includes the following model IDs:
 * </p>
 * 
 * <ul>
 * <li>101</li>
 * <li>102</li>
 * <li>103</li>
 * </ul>
 * 
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public enum IntegerInverterModelRegister implements SunspecModbusReference {

	// Current

	/** Current total, in A. */
	CurrentTotal(0, UInt16),

	/** Current on phase A, in A. */
	CurrentPhaseA(1, UInt16),

	/** Current on phase B, in A. */
	CurrentPhaseB(2, UInt16),

	/** Current on phase C, in A. */
	CurrentPhaseC(3, UInt16),

	/** Current scale factor, as *10^X. */
	ScaleFactorCurrent(4, Int16, ScaleFactor),

	// Voltage

	/** Phase A-to-Phase C voltage, reported in V. */
	VoltagePhaseAPhaseB(5, UInt16),

	/** Phase B-to-Phase C voltage, reported in V. */
	VoltagePhaseBPhaseC(6, UInt16),

	/** Phase C-to-Phase A voltage, reported in V. */
	VoltagePhaseCPhaseA(7, UInt16),

	/** Phase A-to-neutral voltage, reported in V. */
	VoltagePhaseANeutral(8, UInt16),

	/** Phase B-to-neutral voltage, reported in V. */
	VoltagePhaseBNeutral(9, UInt16),

	/** Phase C-to-neutral voltage, reported in V. */
	VoltagePhaseCNeutral(10, UInt16),

	/** Voltage scale factor, as *10^X. */
	ScaleFactorVoltage(11, Int16, ScaleFactor),

	// Active (Real) Power (W)

	/** Active power total, reported in W. */
	ActivePowerTotal(12, Int16),

	/** Active power scale factor, as *10^X. */
	ScaleFactorActivePower(13, Int16, ScaleFactor),

	// Frequency

	/** AC frequency, reported in Hz. */
	Frequency(14, UInt16),

	/** Frequency scale factor, as *10^X. */
	ScaleFactorFrequency(15, Int16, ScaleFactor),

	// Apparent Power (VA)

	/** Apparent power total, reported in VA. */
	ApparentPowerTotal(16, Int16),

	/** Apparent power scale factor, as *10^X. */
	ScaleFactorApparentPower(17, Int16, ScaleFactor),

	// Reactive Power (VAR)

	/** Reactive power total, reported in VAR. */
	ReactivePowerTotal(18, Int16),

	/** Reactive power scale factor, as *10^X. */
	ScaleFactorReactivePower(19, Int16, ScaleFactor),

	// Power factor

	/** AC power factor, reported a a decimal percentage -1..1. */
	PowerFactorAverage(20, Int16),

	/** AC power factor scale factor, as *10^X. */
	ScaleFactorPowerFactor(21, Int16, ScaleFactor),

	// Active energy

	/** Total active (real) energy exported (received), in Wh. */
	ActiveEnergyExportedTotal(22, UInt32, Accumulator),

	/** Active (real) energy scale factor, as *10^X. */
	ScaleFactorActiveEnergy(24, Int16, ScaleFactor),

	// DC current

	/** DC current total, in A. */
	DcCurrentTotal(25, UInt16),

	/** DC current scale factor, as *10^X. */
	ScaleFactorDcCurrent(26, Int16, ScaleFactor),

	// DC voltage

	/** DC voltage total, in V. */
	DcVoltageTotal(27, UInt16),

	/** DC voltage scale factor, as *10^X. */
	ScaleFactorDcVoltage(28, Int16, ScaleFactor),

	// Active (Real) Power (W)

	/** DC power total, reported in W. */
	DcPowerTotal(29, Int16),

	/** DC power scale factor, as *10^X. */
	ScaleFactorDcPower(30, Int16, ScaleFactor),

	// Temperatures (degrees C)

	/** Cabinet temperature, reported in degrees C. */
	TemperatureCabinet(31, Int16),

	/** Heat sink temperature, reported in degrees C. */
	TemperatureHeatSink(32, Int16),

	/** Transformer temperature, reported in degrees C. */
	TemperatureTransformer(33, Int16),

	/** Other temperature, reported in degrees C. */
	TemperatureOther(34, Int16),

	/** Temperature scale factor, as *10^X. */
	ScaleFactorTemperature(35, Int16, ScaleFactor),

	/** Operating state, see {@link InverterOperatingState}. */
	OperatingState(36, UInt16, Enumeration),

	/** Vendor specific operating state. */
	OperatingStateVendor(37, UInt16, Enumeration),

	/** Events bitmask, see {@link InverterModelEvent}. */
	EventsBitmask(38, UInt32, Bitfield),

	/** Events bitmask 2, reserved for future use. */
	Events2Bitmask(40, UInt32, Bitfield),

	/** Vendor events bitmask. */
	EventsVendorBitmask(42, UInt32, Bitfield),

	/** Vendor events bitmask 2. */
	Events2VendorBitmask(44, UInt32, Bitfield),

	/** Vendor events bitmask 2. */
	Events3VendorBitmask(46, UInt32, Bitfield),

	/** Vendor events bitmask 2. */
	Events4VendorBitmask(48, UInt32, Bitfield);

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;
	private final DataClassification classification;

	private IntegerInverterModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private IntegerInverterModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, dataType.getWordLength(), classification);
	}

	private IntegerInverterModelRegister(int address, ModbusDataType dataType, int wordLength) {
		this(address, dataType, wordLength, null);
	}

	private IntegerInverterModelRegister(int address, ModbusDataType dataType, int wordLength,
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

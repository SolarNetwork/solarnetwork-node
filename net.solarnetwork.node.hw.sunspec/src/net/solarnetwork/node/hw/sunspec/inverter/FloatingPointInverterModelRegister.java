/* ==================================================================
 * FloatingPointInverterModelRegister.java - 11/10/2019 5:13:59 pm
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
import static net.solarnetwork.node.hw.sunspec.DataClassification.Enumeration;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Float32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for SunSpec compliant floating point
 * inverter models.
 * 
 * <p>
 * The floating point inverter models includes the following model IDs:
 * </p>
 * 
 * <ul>
 * <li>111</li>
 * <li>112</li>
 * <li>113</li>
 * </ul>
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
public enum FloatingPointInverterModelRegister implements SunspecModbusReference {

	// Current

	/** Current total, in A. */
	CurrentTotal(0, Float32),

	/** Current on phase A, in A. */
	CurrentPhaseA(2, Float32),

	/** Current on phase B, in A. */
	CurrentPhaseB(4, Float32),

	/** Current on phase C, in A. */
	CurrentPhaseC(6, Float32),

	// Voltage

	/** Phase A-to-Phase B voltage, reported in V. */
	VoltagePhaseAPhaseB(8, Float32),

	/** Phase B-to-Phase C voltage, reported in V. */
	VoltagePhaseBPhaseC(10, Float32),

	/** Phase C-to-Phase A voltage, reported in V. */
	VoltagePhaseCPhaseA(12, Float32),

	/** Phase A-to-neutral voltage, reported in V. */
	VoltagePhaseANeutral(14, Float32),

	/** Phase B-to-neutral voltage, reported in V. */
	VoltagePhaseBNeutral(16, Float32),

	/** Phase C-to-neutral voltage, reported in V. */
	VoltagePhaseCNeutral(18, Float32),

	// Active (Real) Power (W)

	/** Active power total, reported in W. */
	ActivePowerTotal(20, Float32),

	// Frequency

	/** AC frequency, reported in Hz. */
	Frequency(22, Float32),

	// Apparent Power (VA)

	/** Apparent power total, reported in VA. */
	ApparentPowerTotal(24, Float32),

	// Reactive Power (VAR)

	/** Reactive power total, reported in VAR. */
	ReactivePowerTotal(26, Float32),

	// Power factor

	/** AC power factor, reported a a decimal percentage -1..1. */
	PowerFactorAverage(28, Float32),

	// Active energy

	/** Total active (real) energy exported (received), in Wh. */
	ActiveEnergyExportedTotal(30, Float32, Accumulator),

	// DC current

	/** DC current total, in A. */
	DcCurrentTotal(32, Float32),

	// DC voltage

	/** DC voltage total, in V. */
	DcVoltageTotal(34, Float32),

	// Active (Real) Power (W)

	/** DC power total, reported in W. */
	DcPowerTotal(36, Float32),

	// Temperatures (degrees C)

	/** Cabinet temperature, reported in degrees C. */
	TemperatureCabinet(38, Float32),

	/** Heat sink temperature, reported in degrees C. */
	TemperatureHeatSink(40, Float32),

	/** Transformer temperature, reported in degrees C. */
	TemperatureTransformer(42, Float32),

	/** Other temperature, reported in degrees C. */
	TemperatureOther(44, Float32),

	/** Operating state, see {@link InverterOperatingState}. */
	OperatingState(46, UInt16, Enumeration),

	/** Vendor specific operating state. */
	OperatingStateVendor(47, UInt16, Enumeration),

	/** Events bitmask, see {@link InverterModelEvent}. */
	EventsBitmask(48, UInt32, Bitfield),

	/** Events bitmask 2, reserved for future use. */
	Events2Bitmask(50, UInt32, Bitfield),

	/** Vendor events bitmask. */
	EventsVendorBitmask(52, UInt32, Bitfield),

	/** Vendor events bitmask 2. */
	Events2VendorBitmask(54, UInt32, Bitfield),

	/** Vendor events bitmask 3. */
	Events3VendorBitmask(56, UInt32, Bitfield),

	/** Vendor events bitmask 4. */
	Events4VendorBitmask(58, UInt32, Bitfield);

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;
	private final DataClassification classification;

	private FloatingPointInverterModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private FloatingPointInverterModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, dataType.getWordLength(), classification);
	}

	private FloatingPointInverterModelRegister(int address, ModbusDataType dataType, int wordLength) {
		this(address, dataType, wordLength, null);
	}

	private FloatingPointInverterModelRegister(int address, ModbusDataType dataType, int wordLength,
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

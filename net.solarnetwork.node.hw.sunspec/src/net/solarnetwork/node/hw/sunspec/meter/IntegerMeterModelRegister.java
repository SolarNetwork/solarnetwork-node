/* ==================================================================
 * IntegerMeterModelRegister.java - 21/05/2018 5:14:54 PM
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

package net.solarnetwork.node.hw.sunspec.meter;

import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Enumeration of Modbus register mappings for SunSpec compliant integer meter
 * model.
 * 
 * <p>
 * The integer meter model includes the following model IDs:
 * </p>
 * 
 * <ul>
 * <li>201</li>
 * <li>202</li>
 * <li>203</li>
 * <li>204</li>
 * </ul>
 * 
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public enum IntegerMeterModelRegister implements ModbusReference {

	// Current

	/** Current total, in A. */
	CurrentTotal(0, Int16),

	/** Current on phase A, in A. */
	CurrentPhaseA(1, Int16),

	/** Current on phase B, in A. */
	CurrentPhaseB(2, Int16),

	/** Current on phase C, in A. */
	CurrentPhaseC(3, Int16),

	/** Current scale factor, as *10^X. */
	ScaleFactorCurrent(4, Int16),

	// Voltage

	/** Line-to-neutral voltage average, reported in V. */
	VoltageLineNeutralAverage(5, Int16),

	/** Phase A-to-neutral voltage, reported in V. */
	VoltagePhaseANeutral(6, Int16),

	/** Phase B-to-neutral voltage, reported in V. */
	VoltagePhaseBNeutral(7, Int16),

	/** Phase C-to-neutral voltage, reported in V. */
	VoltagePhaseCNeutral(8, Int16),

	/** Line-to-line voltage average, reported in V. */
	VoltageLineLineAverage(9, Int16),

	/** Phase A-to-Phase C voltage, reported in V. */
	VoltagePhaseAPhaseB(10, Int16),

	/** Phase B-to-Phase C voltage, reported in V. */
	VoltagePhaseBPhaseC(11, Int16),

	/** Phase C-to-Phase A voltage, reported in V. */
	VoltagePhaseCPhaseA(12, Int16),

	/** Voltage scale factor, as *10^X. */
	ScaleFactorVoltage(13, Int16),

	// Frequency

	/** AC frequency, reported in Hz. */
	Frequency(14, Int16),

	/** Frequency scale factor, as *10^X. */
	ScaleFactorFrequency(15, Int16),

	// Active (Real) Power (W)

	/** Active power total, reported in W. */
	ActivePowerTotal(16, Int16),

	/** Active power total, reported in W. */
	ActivePowerPhaseA(17, Int16),

	/** Active power total, reported in W. */
	ActivePowerPhaseB(18, Int16),

	/** Active power total, reported in W. */
	ActivePowerPhaseC(19, Int16),

	/** Active power scale factor, as *10^X. */
	ScaleFactorActivePower(20, Int16),

	// Apparent Power (VA)

	/** Apparent power total, reported in VA. */
	ApparentPowerTotal(21, Int16),

	/** Apparent power total, reported in VA. */
	ApparentPowerPhaseA(22, Int16),

	/** Apparent power total, reported in VA. */
	ApparentPowerPhaseB(23, Int16),

	/** Apparent power total, reported in VA. */
	ApparentPowerPhaseC(24, Int16),

	/** Apparent power scale factor, as *10^X. */
	ScaleFactorApparentPower(25, Int16),

	// Reactive Power (VAR)

	/** Reactive power total, reported in VAR. */
	ReactivePowerTotal(26, Int16),

	/** Reactive power total, reported in VAR. */
	ReactivePowerPhaseA(27, Int16),

	/** Reactive power total, reported in VAR. */
	ReactivePowerPhaseB(28, Int16),

	/** Reactive power total, reported in VAR. */
	ReactivePowerPhaseC(29, Int16),

	/** Reactive power scale factor, as *10^X. */
	ScaleFactorReactivePower(30, Int16),

	// Power factor

	/** Reactive power total, reported in VAR. */
	PowerFactorAverage(31, Int16),

	/** Reactive power total, reported in VAR. */
	PowerFactorPhaseA(32, Int16),

	/** Reactive power total, reported in VAR. */
	PowerFactorPhaseB(33, Int16),

	/** Reactive power total, reported in VAR. */
	PowerFactorPhaseC(34, Int16),

	/** Reactive power scale factor, as *10^X. */
	ScaleFactorPowerFactor(35, Int16),

	// Active energy

	/** Total active (real) energy exported (received), in Wh. */
	ActiveEnergyExportedTotal(36, UInt32),

	/** Phase A active (real) energy exported (received), in Wh. */
	ActiveEnergyExportedPhaseA(38, UInt32),

	/** Phase B active (real) energy exported (received), in Wh. */
	ActiveEnergyExportedPhaseB(40, UInt32),

	/** Phase C active (real) energy exported (received), in Wh. */
	ActiveEnergyExportedPhaseC(42, UInt32),

	/** Total active (real) energy imported (delivered), in Wh. */
	ActiveEnergyImportedTotal(44, UInt32),

	/** Phase A active (real) energy imported (delivered), in Wh. */
	ActiveEnergyImportedPhaseA(46, UInt32),

	/** Phase B active (real) energy imported (delivered), in Wh. */
	ActiveEnergyImportedPhaseB(48, UInt32),

	/** Phase C active (real) energy imported (delivered), in Wh. */
	ActiveEnergyImportedPhaseC(50, UInt32),

	/** Active (real) energy scale factor, as *10^X. */
	ScaleFactorActiveEnergy(52, Int16),

	// Apparent energy

	/** Total apparent energy exported (received), in VAh. */
	ApparentEnergyExportedTotal(53, UInt32),

	/** Phase A apparent energy exported (received), in VAh. */
	ApparentEnergyExportedPhaseA(55, UInt32),

	/** Phase B apparent energy exported (received), in VAh. */
	ApparentEnergyExportedPhaseB(57, UInt32),

	/** Phase C apparent energy exported (received), in VAh. */
	ApparentEnergyExportedPhaseC(59, UInt32),

	/** Total apparent energy imported (delivered), in VAh. */
	ApparentEnergyImportedTotal(61, UInt32),

	/** Phase A apparent energy imported (delivered), in VAh. */
	ApparentEnergyImportedPhaseA(63, UInt32),

	/** Phase B apparent energy imported (delivered), in VAh. */
	ApparentEnergyImportedPhaseB(65, UInt32),

	/** Phase C apparent energy imported (delivered), in VAh. */
	ApparentEnergyImportedPhaseC(67, UInt32),

	/** Apparent energy scale factor, as *10^X. */
	ScaleFactorApparentEnergy(69, Int16),

	// Reactive energy

	/** Total reactive energy imported Q1, in VARh. */
	ReactiveEnergyImportedQ1Total(70, UInt32),

	/** Phase A reactive energy imported Q1, in VARh. */
	ReactiveEnergyImportedQ1PhaseA(72, UInt32),

	/** Phase B reactive energy imported Q1, in VARh. */
	ReactiveEnergyImportedQ1PhaseB(74, UInt32),

	/** Phase C reactive energy imported Q1, in VARh. */
	ReactiveEnergyImportedQ1PhaseC(76, UInt32),

	/** Total reactive energy imported Q2, in VARh. */
	ReactiveEnergyImportedQ2Total(78, UInt32),

	/** Phase A reactive energy imported Q2, in VARh. */
	ReactiveEnergyImportedQ2PhaseA(80, UInt32),

	/** Phase B reactive energy imported Q2, in VARh. */
	ReactiveEnergyImportedQ2PhaseB(82, UInt32),

	/** Phase C reactive energy imported Q2, in VARh. */
	ReactiveEnergyImportedQ2PhaseC(84, UInt32),

	/** Total reactive energy exported Q3, in VARh. */
	ReactiveEnergyExportedQ3Total(86, UInt32),

	/** Phase A reactive energy exported Q3, in VARh. */
	ReactiveEnergyExportedQ3PhaseA(88, UInt32),

	/** Phase B reactive energy exported Q3, in VARh. */
	ReactiveEnergyExportedQ3PhaseB(90, UInt32),

	/** Phase C reactive energy exported Q3, in VARh. */
	ReactiveEnergyExportedQ3PhaseC(92, UInt32),

	/** Total reactive energy exported Q4, in VARh. */
	ReactiveEnergyExportedQ4Total(94, UInt32),

	/** Phase A reactive energy exported Q4, in VARh. */
	ReactiveEnergyExportedQ4PhaseA(96, UInt32),

	/** Phase B reactive energy exported Q4, in VARh. */
	ReactiveEnergyExportedQ4PhaseB(98, UInt32),

	/** Phase C reactive energy exported Q4, in VARh. */
	ReactiveEnergyExportedQ4PhaseC(100, UInt32),

	/** Reactive energy scale factor, as *10^X. */
	ScaleFactorReactiveEnergy(102, Int16),

	/** Events bitmask, see {@link MeterModelEvent}. */
	EventsBitmask(103, UInt32);

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;

	private IntegerMeterModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private IntegerMeterModelRegister(int address, ModbusDataType dataType, int wordLength) {
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

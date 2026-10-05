/* ==================================================================
 * FloatingPointMeterModelRegister.java - 6/10/2026 4:12:47 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

import static net.solarnetwork.node.hw.sunspec.DataClassification.Bitfield;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Float32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for SunSpec compliant floating point
 * meter models.
 *
 * <p>
 * The floating point meter model includes the following model IDs:
 * </p>
 *
 * <ul>
 * <li>211</li>
 * <li>212</li>
 * <li>213</li>
 * <li>214</li>
 * </ul>
 *
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum FloatingPointMeterModelRegister implements SunspecModbusReference {

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

	/** Line-to-neutral voltage average, reported in V. */
	VoltageLineNeutralAverage(8, Float32),

	/** Phase A-to-neutral voltage, reported in V. */
	VoltagePhaseANeutral(10, Float32),

	/** Phase B-to-neutral voltage, reported in V. */
	VoltagePhaseBNeutral(12, Float32),

	/** Phase C-to-neutral voltage, reported in V. */
	VoltagePhaseCNeutral(14, Float32),

	/** Line-to-line voltage average, reported in V. */
	VoltageLineLineAverage(16, Float32),

	/** Phase A-to-Phase B voltage, reported in V. */
	VoltagePhaseAPhaseB(18, Float32),

	/** Phase B-to-Phase C voltage, reported in V. */
	VoltagePhaseBPhaseC(20, Float32),

	/** Phase C-to-Phase A voltage, reported in V. */
	VoltagePhaseCPhaseA(22, Float32),

	// Frequency

	/** AC frequency, reported in Hz. */
	Frequency(24, Float32),

	// Active (Real) Power (W)

	/** Active power total, reported in W. */
	ActivePowerTotal(26, Float32),

	/** Active power total, reported in W. */
	ActivePowerPhaseA(28, Float32),

	/** Active power total, reported in W. */
	ActivePowerPhaseB(30, Float32),

	/** Active power total, reported in W. */
	ActivePowerPhaseC(32, Float32),

	// Apparent Power (VA)

	/** Apparent power total, reported in VA. */
	ApparentPowerTotal(34, Float32),

	/** Apparent power total, reported in VA. */
	ApparentPowerPhaseA(36, Float32),

	/** Apparent power total, reported in VA. */
	ApparentPowerPhaseB(38, Float32),

	/** Apparent power total, reported in VA. */
	ApparentPowerPhaseC(40, Float32),

	// Reactive Power (VAR)

	/** Reactive power total, reported in VAR. */
	ReactivePowerTotal(42, Float32),

	/** Reactive power total, reported in VAR. */
	ReactivePowerPhaseA(44, Float32),

	/** Reactive power total, reported in VAR. */
	ReactivePowerPhaseB(46, Float32),

	/** Reactive power total, reported in VAR. */
	ReactivePowerPhaseC(48, Float32),

	// Power factor

	/** Power factor total average, reported in percentage. */
	PowerFactorAverage(50, Float32),

	/** Power factor total average, reported in percentage. */
	PowerFactorPhaseA(52, Float32),

	/** Power factor total average, reported in percentage. */
	PowerFactorPhaseB(54, Float32),

	/** Power factor total average, reported in percentage. */
	PowerFactorPhaseC(56, Float32),

	// Active (Real) Energy (Wh)

	/** Total active (real) energy exported (received), in Wh. */
	ActiveEnergyExportedTotal(58, Float32),

	/** Phase A active (real) energy exported (received), in Wh. */
	ActiveEnergyExportedPhaseA(60, Float32),

	/** Phase B active (real) energy exported (received), in Wh. */
	ActiveEnergyExportedPhaseB(62, Float32),

	/** Phase C active (real) energy exported (received), in Wh. */
	ActiveEnergyExportedPhaseC(64, Float32),

	/** Total active (real) energy imported (delivered), in Wh. */
	ActiveEnergyImportedTotal(66, Float32),

	/** Phase A active (real) energy imported (delivered), in Wh. */
	ActiveEnergyImportedPhaseA(68, Float32),

	/** Phase B active (real) energy imported (delivered), in Wh. */
	ActiveEnergyImportedPhaseB(70, Float32),

	/** Phase C active (real) energy imported (delivered), in Wh. */
	ActiveEnergyImportedPhaseC(72, Float32),

	// Apparent Energy (VAh)

	/** Total apparent energy exported (received), in VAh. */
	ApparentEnergyExportedTotal(74, Float32),

	/** Phase A apparent energy exported (received), in VAh. */
	ApparentEnergyExportedPhaseA(76, Float32),

	/** Phase B apparent energy exported (received), in VAh. */
	ApparentEnergyExportedPhaseB(78, Float32),

	/** Phase C apparent energy exported (received), in VAh. */
	ApparentEnergyExportedPhaseC(80, Float32),

	/** Total apparent energy imported (delivered), in VAh. */
	ApparentEnergyImportedTotal(82, Float32),

	/** Phase A apparent energy imported (delivered), in VAh. */
	ApparentEnergyImportedPhaseA(84, Float32),

	/** Phase B apparent energy imported (delivered), in VAh. */
	ApparentEnergyImportedPhaseB(86, Float32),

	/** Phase C apparent energy imported (delivered), in VAh. */
	ApparentEnergyImportedPhaseC(88, Float32),

	// Reactive Energy (VARh)

	/** Total reactive energy imported Q1, in VARh. */
	ReactiveEnergyImportedQ1Total(90, Float32),

	/** Phase A reactive energy imported Q1, in VARh. */
	ReactiveEnergyImportedQ1PhaseA(92, Float32),

	/** Phase B reactive energy imported Q1, in VARh. */
	ReactiveEnergyImportedQ1PhaseB(94, Float32),

	/** Phase C reactive energy imported Q1, in VARh. */
	ReactiveEnergyImportedQ1PhaseC(96, Float32),

	/** Total reactive energy imported Q2, in VARh. */
	ReactiveEnergyImportedQ2Total(98, Float32),

	/** Phase A reactive energy imported Q2, in VARh. */
	ReactiveEnergyImportedQ2PhaseA(100, Float32),

	/** Phase B reactive energy imported Q2, in VARh. */
	ReactiveEnergyImportedQ2PhaseB(102, Float32),

	/** Phase C reactive energy imported Q2, in VARh. */
	ReactiveEnergyImportedQ2PhaseC(104, Float32),

	/** Total reactive energy exported Q3, in VARh. */
	ReactiveEnergyExportedQ3Total(106, Float32),

	/** Phase A reactive energy exported Q3, in VARh. */
	ReactiveEnergyExportedQ3PhaseA(108, Float32),

	/** Phase B reactive energy exported Q3, in VARh. */
	ReactiveEnergyExportedQ3PhaseB(110, Float32),

	/** Phase C reactive energy exported Q3, in VARh. */
	ReactiveEnergyExportedQ3PhaseC(112, Float32),

	/** Total reactive energy exported Q4, in VARh. */
	ReactiveEnergyExportedQ4Total(114, Float32),

	/** Phase A reactive energy exported Q4, in VARh. */
	ReactiveEnergyExportedQ4PhaseA(116, Float32),

	/** Phase B reactive energy exported Q4, in VARh. */
	ReactiveEnergyExportedQ4PhaseB(118, Float32),

	/** Phase C reactive energy exported Q4, in VARh. */
	ReactiveEnergyExportedQ4PhaseC(120, Float32),

	// Events

	/** Events bitmask, see {@link MeterModelEvent}. */
	EventsBitmask(122, UInt32, Bitfield);

	private final int address;
	private final ModbusDataType dataType;
	private final @Nullable DataClassification classification;

	private FloatingPointMeterModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, null);
	}

	private FloatingPointMeterModelRegister(int address, ModbusDataType dataType,
			@Nullable DataClassification classification) {
		this.address = address;
		this.dataType = dataType;
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
		return dataType.getWordLength();
	}

	@Override
	public @Nullable DataClassification getClassification() {
		return classification;
	}

}

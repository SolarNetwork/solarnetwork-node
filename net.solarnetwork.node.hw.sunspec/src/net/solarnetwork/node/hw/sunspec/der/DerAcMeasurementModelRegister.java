/* ==================================================================
 * DerAcMeasurementModelRegister.java - 5/10/2026 8:27:22 am
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

package net.solarnetwork.node.hw.sunspec.der;

import static net.solarnetwork.node.hw.sunspec.DataClassification.Bitfield;
import static net.solarnetwork.node.hw.sunspec.DataClassification.Enumeration;
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.StringUtf8;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt64;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for the SunSpec DER AC measurement
 * model.
 *
 * <p>
 * These mappings correspond to the SunSpec model number <b>701</b>.
 * </p>
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
public enum DerAcMeasurementModelRegister implements SunspecModbusReference {

	/** AC wiring type, see {@link DerAcWiringType}. */
	AcWiringType(0, UInt16, Enumeration),

	/** DER operating state, see {@link DerOperatingState}. */
	OperatingState(1, UInt16, Enumeration),

	/** Inverter state, see {@link DerInverterState}. */
	InverterState(2, UInt16, Enumeration),

	/** Grid connection state, see {@link DerGridConnectionState}. */
	GridConnectionState(3, UInt16, Enumeration),

	/** Active alarms bitmask, see {@link DerAlarm}. */
	AlarmsBitmask(4, UInt32, Bitfield),

	/**
	 * Operational characteristics bitmask, see
	 * {@link DerOperationalCharacteristic}.
	 */
	OperationalCharacteristicsBitmask(6, UInt32, Bitfield),

	// Totals

	/**
	 * Total active power, in W, positive for generation and negative for
	 * absorption.
	 */
	ActivePowerTotal(8, Int16),

	/** Total apparent power, in VA. */
	ApparentPowerTotal(9, Int16),

	/** Total reactive power, in VAR. */
	ReactivePowerTotal(10, Int16),

	/** Power factor, with the sign of the active power. */
	PowerFactorTotal(11, Int16),

	/** Total AC current, in A. */
	CurrentTotal(12, Int16),

	/** Line to line AC voltage as an average of active phases, in V. */
	VoltageLineLineAverage(13, UInt16),

	/** Line to neutral AC voltage as an average of active phases, in V. */
	VoltageLineNeutralAverage(14, UInt16),

	/** AC frequency, in Hz. */
	Frequency(15, UInt32),

	/** Total active energy injected (quadrants 1 and 4), in Wh. */
	ActiveEnergyInjectedTotal(17, UInt64),

	/** Total active energy absorbed (quadrants 2 and 3), in Wh. */
	ActiveEnergyAbsorbedTotal(21, UInt64),

	/** Total reactive energy injected (quadrants 1 and 2), in VARh. */
	ReactiveEnergyInjectedTotal(25, UInt64),

	/** Total reactive energy absorbed (quadrants 3 and 4), in VARh. */
	ReactiveEnergyAbsorbedTotal(29, UInt64),

	// Temperatures

	/** Ambient temperature, in degrees Celsius. */
	TemperatureAmbient(33, Int16),

	/** Cabinet temperature, in degrees Celsius. */
	TemperatureCabinet(34, Int16),

	/** Heat sink temperature, in degrees Celsius. */
	TemperatureHeatSink(35, Int16),

	/** Transformer temperature, in degrees Celsius. */
	TemperatureTransformer(36, Int16),

	/** IGBT/MOSFET temperature, in degrees Celsius. */
	TemperatureSwitch(37, Int16),

	/** Other temperature, in degrees Celsius. */
	TemperatureOther(38, Int16),

	// Phase A (L1)

	/** Active power phase A, in W. */
	ActivePowerPhaseA(39, Int16),

	/** Apparent power phase A, in VA. */
	ApparentPowerPhaseA(40, Int16),

	/** Reactive power phase A, in VAR. */
	ReactivePowerPhaseA(41, Int16),

	/** Power factor phase A. */
	PowerFactorPhaseA(42, Int16),

	/** Current phase A, in A. */
	CurrentPhaseA(43, Int16),

	/** Phase A to phase B voltage, in V. */
	VoltagePhaseAPhaseB(44, UInt16),

	/** Phase A to neutral voltage, in V. */
	VoltagePhaseANeutral(45, UInt16),

	/** Total active energy injected phase A, in Wh. */
	ActiveEnergyInjectedPhaseA(46, UInt64),

	/** Total active energy absorbed phase A, in Wh. */
	ActiveEnergyAbsorbedPhaseA(50, UInt64),

	/** Total reactive energy injected phase A, in VARh. */
	ReactiveEnergyInjectedPhaseA(54, UInt64),

	/** Total reactive energy absorbed phase A, in VARh. */
	ReactiveEnergyAbsorbedPhaseA(58, UInt64),

	// Phase B (L2)

	/** Active power phase B, in W. */
	ActivePowerPhaseB(62, Int16),

	/** Apparent power phase B, in VA. */
	ApparentPowerPhaseB(63, Int16),

	/** Reactive power phase B, in VAR. */
	ReactivePowerPhaseB(64, Int16),

	/** Power factor phase B. */
	PowerFactorPhaseB(65, Int16),

	/** Current phase B, in A. */
	CurrentPhaseB(66, Int16),

	/** Phase B to phase C voltage, in V. */
	VoltagePhaseBPhaseC(67, UInt16),

	/** Phase B to neutral voltage, in V. */
	VoltagePhaseBNeutral(68, UInt16),

	/** Total active energy injected phase B, in Wh. */
	ActiveEnergyInjectedPhaseB(69, UInt64),

	/** Total active energy absorbed phase B, in Wh. */
	ActiveEnergyAbsorbedPhaseB(73, UInt64),

	/** Total reactive energy injected phase B, in VARh. */
	ReactiveEnergyInjectedPhaseB(77, UInt64),

	/** Total reactive energy absorbed phase B, in VARh. */
	ReactiveEnergyAbsorbedPhaseB(81, UInt64),

	// Phase C (L3)

	/** Active power phase C, in W. */
	ActivePowerPhaseC(85, Int16),

	/** Apparent power phase C, in VA. */
	ApparentPowerPhaseC(86, Int16),

	/** Reactive power phase C, in VAR. */
	ReactivePowerPhaseC(87, Int16),

	/** Power factor phase C. */
	PowerFactorPhaseC(88, Int16),

	/** Current phase C, in A. */
	CurrentPhaseC(89, Int16),

	/** Phase C to phase A voltage, in V. */
	VoltagePhaseCPhaseA(90, UInt16),

	/** Phase C to neutral voltage, in V. */
	VoltagePhaseCNeutral(91, UInt16),

	/** Total active energy injected phase C, in Wh. */
	ActiveEnergyInjectedPhaseC(92, UInt64),

	/** Total active energy absorbed phase C, in Wh. */
	ActiveEnergyAbsorbedPhaseC(96, UInt64),

	/** Total reactive energy injected phase C, in VARh. */
	ReactiveEnergyInjectedPhaseC(100, UInt64),

	/** Total reactive energy absorbed phase C, in VARh. */
	ReactiveEnergyAbsorbedPhaseC(104, UInt64),

	// Active power throttling

	/** Active power throttling, as a percentage of maximum active power. */
	ThrottlePercent(108, UInt16),

	/** Active power throttling sources bitmask, see {@link DerThrottleSource}. */
	ThrottleSourcesBitmask(109, UInt32, Bitfield),

	// Scale factors

	/** Current scale factor, as *10^X. */
	ScaleFactorCurrent(111, Int16, ScaleFactor),

	/** Voltage scale factor, as *10^X. */
	ScaleFactorVoltage(112, Int16, ScaleFactor),

	/** Frequency scale factor, as *10^X. */
	ScaleFactorFrequency(113, Int16, ScaleFactor),

	/** Active power scale factor, as *10^X. */
	ScaleFactorActivePower(114, Int16, ScaleFactor),

	/** Power factor scale factor, as *10^X. */
	ScaleFactorPowerFactor(115, Int16, ScaleFactor),

	/** Apparent power scale factor, as *10^X. */
	ScaleFactorApparentPower(116, Int16, ScaleFactor),

	/** Reactive power scale factor, as *10^X. */
	ScaleFactorReactivePower(117, Int16, ScaleFactor),

	/** Active energy scale factor, as *10^X. */
	ScaleFactorActiveEnergy(118, Int16, ScaleFactor),

	/** Reactive energy scale factor, as *10^X. */
	ScaleFactorReactiveEnergy(119, Int16, ScaleFactor),

	/** Temperature scale factor, as *10^X. */
	ScaleFactorTemperature(120, Int16, ScaleFactor),

	/**
	 * Manufacturer alarm information, valid when the
	 * {@link DerAlarm#ManufacturerAlarm} alarm is active.
	 */
	ManufacturerAlarmInfo(121, StringUtf8, 32),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;
	private final @Nullable DataClassification classification;

	private DerAcMeasurementModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private DerAcMeasurementModelRegister(int address, ModbusDataType dataType,
			@Nullable DataClassification classification) {
		this(address, dataType, dataType.getWordLength(), classification);
	}

	private DerAcMeasurementModelRegister(int address, ModbusDataType dataType, int wordLength) {
		this(address, dataType, wordLength, null);
	}

	private DerAcMeasurementModelRegister(int address, ModbusDataType dataType, int wordLength,
			@Nullable DataClassification classification) {
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
	public @Nullable DataClassification getClassification() {
		return classification;
	}

}

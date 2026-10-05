/* ==================================================================
 * InverterBasicSettingsRegister.java - 15/10/2018 2:20:12 PM
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

import static net.solarnetwork.node.hw.sunspec.DataClassification.Enumeration;
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.hw.sunspec.PointAccess.ReadWrite;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.hw.sunspec.ApparentPowerCalculationMethod;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.PointAccess;
import net.solarnetwork.node.hw.sunspec.ReactivePowerAction;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for SunSpec model 121.
 *
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block.
 * </p>
 *
 * @author matt
 * @version 2.1
 * @since 1.2
 */
public enum InverterBasicSettingsRegister implements SunspecModbusReference {

	// Active (Real) Power (W)

	/** Active power maximum, reported in W. */
	ActivePowerMaximum(0, UInt16, ReadWrite),

	/** Active power maximum scale factor, as *10^X. */
	ScaleFactorActivePowerMaximum(20, Int16, ScaleFactor),

	// Voltage

	/** PCC voltage, in V. */
	VoltagePcc(1, UInt16, ReadWrite),

	/** PCC voltage scale factor, as *10^X. */
	ScaleFactorVoltagePcc(21, Int16, ScaleFactor),

	/** Voltage offset from the PCC to the inverter, in V. */
	VoltagePccOffset(2, Int16, ReadWrite),

	/** PCC voltage offset scale factor, as *10^X. */
	ScaleFactorVoltagePccOffset(22, Int16, ScaleFactor),

	/** Maximum voltage, in V. */
	VoltageMaximum(3, UInt16, ReadWrite),

	/** Minimum voltage, in V. */
	VoltageMinimum(4, UInt16, ReadWrite),

	/** Minimum and maximum voltage scale factor, as *10^X. */
	ScaleFactorVoltageMinimumMaximum(23, Int16, ScaleFactor),

	// Apparent Power (VA)

	/** Apparent power maximum, reported in VA. */
	ApparentPowerMaximum(5, UInt16, ReadWrite),

	/** Apparent power maximum scale factor, as *10^X. */
	ScaleFactorApparentPowerMaximum(24, Int16, ScaleFactor),

	// Reactive Power (VAR)

	/** Reactive power maximum Q1, reported in VAR. */
	ReactivePowerQ1Maximum(6, Int16, ReadWrite),

	/** Reactive power maximum Q2, reported in VAR. */
	ReactivePowerQ2Maximum(7, Int16, ReadWrite),

	/** Reactive power maximum Q3, reported in VAR. */
	ReactivePowerQ3Maximum(8, Int16, ReadWrite),

	/** Reactive power maximum Q4, reported in VAR. */
	ReactivePowerQ4Maximum(9, Int16, ReadWrite),

	/** Reactive power maximum scale factor, as *10^X. */
	ScaleFactorReactivePowerMaximum(25, Int16, ScaleFactor),

	// Ramp rate

	/** Active power ramp rate, in % active power maximum / second. */
	ActivePowerRampRate(10, UInt16, ReadWrite),

	/** Active power ramp rate scale factor, as *10^X. */
	ScaleFactorActivePowerRampRate(26, Int16, ScaleFactor),

	/** Active power ramp rate maximum, in % ActivePowerRampRate. */
	ActivePowerRampRateMaximum(17, UInt16, ReadWrite),

	/** Active power ramp rate maximum scale factor, as *10^X. */
	ScaleFactorActivePowerRampRateMaximum(28, Int16, ScaleFactor),

	// Power factor

	/** AC power factor minimum Q1, reported as a decimal -1..1. */
	PowerFactorQ1Minimum(11, Int16, ReadWrite),

	/** AC power factor minimum Q2, reported as a decimal -1..1. */
	PowerFactorQ2Minimum(12, Int16, ReadWrite),

	/** AC power factor minimum Q3, reported as a decimal -1..1. */
	PowerFactorQ3Minimum(13, Int16, ReadWrite),

	/** AC power factor minimum Q4, reported as a decimal -1..1. */
	PowerFactorQ4Minimum(14, Int16, ReadWrite),

	/** AC power factor minimum scale factor, as *10^X. */
	ScaleFactorPowerFactorMinimum(27, Int16, ScaleFactor),

	// Reactive power action

	/**
	 * Reactive power charge/discharge change action, see
	 * {@link ReactivePowerAction}.
	 */
	ImportExportChangeReactivePowerAction(15, UInt16, Enumeration, ReadWrite),

	// Apparent power calculation method

	/**
	 * Apparent power calculation method, see
	 * {@link ApparentPowerCalculationMethod}.
	 */
	ApparentPowerCalculationMethod(16, UInt16, Enumeration, ReadWrite),

	// Frequency

	/** Nominal frequency at the electrical connection point (ECP), in Hz. */
	EcpNominalFrequency(18, UInt16, ReadWrite),

	/** Nominal ECP frequency scale factor, as *10^X. */
	ScaleFactorEcpNominalFrequency(29, Int16, ScaleFactor),

	// Phase

	/** Connected phase for single-phase inverters, see {@link AcPhase}. */
	ConnectedPhase(19, UInt16, Enumeration, ReadWrite);

	private final int address;
	private final ModbusDataType dataType;
	private final @Nullable DataClassification classification;
	private final PointAccess access;

	private InverterBasicSettingsRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, classification, PointAccess.ReadOnly);
	}

	private InverterBasicSettingsRegister(int address, ModbusDataType dataType, PointAccess access) {
		this(address, dataType, null, access);
	}

	private InverterBasicSettingsRegister(int address, ModbusDataType dataType,
			@Nullable DataClassification classification, PointAccess access) {
		this.address = address;
		this.dataType = dataType;
		this.classification = classification;
		this.access = access;
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
		return dataType.getWordLength();
	}

	@Override
	public @Nullable DataClassification getClassification() {
		return classification;
	}

	@Override
	public PointAccess getAccess() {
		return access;
	}

}

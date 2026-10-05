/* ==================================================================
 * DerAcControlsModelRegister.java - 5/10/2026 2:52:06 pm
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

import static net.solarnetwork.node.hw.sunspec.DataClassification.Enumeration;
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.hw.sunspec.PointAccess.ReadWrite;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.PointAccess;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for the SunSpec DER AC controls
 * model.
 *
 * <p>
 * These mappings correspond to the SunSpec model number <b>704</b>. Each power
 * factor setpoint and its excitation form a SunSpec synchronization group,
 * which must be written in a single request.
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
public enum DerAcControlsModelRegister implements SunspecModbusReference {

	/** The power factor when injecting active power enable setting. */
	PowerFactorWhenInjectingEnabled(0, UInt16, Enumeration, ReadWrite),

	/**
	 * The power factor when injecting active power reversion timer enable
	 * setting.
	 */
	PowerFactorWhenInjectingReversionEnabled(1, UInt16, Enumeration, ReadWrite),

	/**
	 * The power factor when injecting active power reversion timeout, in
	 * seconds.
	 */
	PowerFactorWhenInjectingReversionTime(2, UInt32, ReadWrite),

	/**
	 * The power factor when injecting active power reversion time remaining, in
	 * seconds.
	 */
	PowerFactorWhenInjectingReversionTimeRemaining(4, UInt32),

	/** The power factor when absorbing active power enable setting. */
	PowerFactorWhenAbsorbingEnabled(6, UInt16, Enumeration, ReadWrite),

	/**
	 * The power factor when absorbing active power reversion timer enable
	 * setting.
	 */
	PowerFactorWhenAbsorbingReversionEnabled(7, UInt16, Enumeration, ReadWrite),

	/**
	 * The power factor when absorbing active power reversion timeout, in
	 * seconds.
	 */
	PowerFactorWhenAbsorbingReversionTime(8, UInt32, ReadWrite),

	/**
	 * The power factor when absorbing active power reversion time remaining, in
	 * seconds.
	 */
	PowerFactorWhenAbsorbingReversionTimeRemaining(10, UInt32),

	/** The limit maximum active power enable setting. */
	ActivePowerLimitEnabled(12, UInt16, Enumeration, ReadWrite),

	/**
	 * The limit maximum active power setpoint, as a percentage of the maximum
	 * active power.
	 */
	ActivePowerLimitPercent(13, UInt16, ReadWrite),

	/**
	 * The reversion limit maximum active power setpoint, as a percentage of the
	 * maximum active power.
	 */
	ReversionActivePowerLimitPercent(14, UInt16, ReadWrite),

	/** The limit maximum active power reversion timer enable setting. */
	ActivePowerLimitReversionEnabled(15, UInt16, Enumeration, ReadWrite),

	/** The limit maximum active power reversion timeout, in seconds. */
	ActivePowerLimitReversionTime(16, UInt32, ReadWrite),

	/** The limit maximum active power reversion time remaining, in seconds. */
	ActivePowerLimitReversionTimeRemaining(18, UInt32),

	/** The set active power enable setting. */
	ActivePowerSetpointEnabled(20, UInt16, Enumeration, ReadWrite),

	/** The set active power mode, see {@link DerActivePowerSetpointMode}. */
	ActivePowerSetpointMode(21, UInt16, Enumeration, ReadWrite),

	/** The active power setpoint, in W. */
	ActivePowerSetpoint(22, Int32, ReadWrite),

	/** The reversion active power setpoint, in W. */
	ReversionActivePowerSetpoint(24, Int32, ReadWrite),

	/**
	 * The active power setpoint, as a percentage of the maximum active power.
	 */
	ActivePowerSetpointPercent(26, Int16, ReadWrite),

	/**
	 * The reversion active power setpoint, as a percentage of the maximum
	 * active power.
	 */
	ReversionActivePowerSetpointPercent(27, Int16, ReadWrite),

	/** The set active power reversion timer enable setting. */
	ActivePowerSetpointReversionEnabled(28, UInt16, Enumeration, ReadWrite),

	/** The set active power reversion timeout, in seconds. */
	ActivePowerSetpointReversionTime(29, UInt32, ReadWrite),

	/** The set active power reversion time remaining, in seconds. */
	ActivePowerSetpointReversionTimeRemaining(31, UInt32),

	/** The set reactive power enable setting. */
	ReactivePowerSetpointEnabled(33, UInt16, Enumeration, ReadWrite),

	/**
	 * The set reactive power mode, see {@link DerReactivePowerSetpointMode}.
	 */
	ReactivePowerSetpointMode(34, UInt16, Enumeration, ReadWrite),

	/** The reactive power priority, see {@link DerReactivePowerPriority}. */
	ReactivePowerPriority(35, UInt16, Enumeration, ReadWrite),

	/** The reactive power setpoint, in var. */
	ReactivePowerSetpoint(36, Int32, ReadWrite),

	/** The reversion reactive power setpoint, in var. */
	ReversionReactivePowerSetpoint(38, Int32, ReadWrite),

	/**
	 * The reactive power setpoint, as a percentage of the reference given by
	 * the mode.
	 */
	ReactivePowerSetpointPercent(40, Int16, ReadWrite),

	/**
	 * The reversion reactive power setpoint, as a percentage of the reference
	 * given by the mode.
	 */
	ReversionReactivePowerSetpointPercent(41, Int16, ReadWrite),

	/** The set reactive power reversion timer enable setting. */
	ReactivePowerSetpointReversionEnabled(42, UInt16, Enumeration, ReadWrite),

	/** The set reactive power reversion timeout, in seconds. */
	ReactivePowerSetpointReversionTime(43, UInt32, ReadWrite),

	/** The set reactive power reversion time remaining, in seconds. */
	ReactivePowerSetpointReversionTimeRemaining(45, UInt32),

	/**
	 * The ramp rate for increases in active power during normal generation, as
	 * a percentage of the ramp rate reference per second.
	 */
	ActivePowerRampRate(47, UInt16, ReadWrite),

	/**
	 * The active power ramp rate reference, see {@link DerRampRateReference}.
	 */
	ActivePowerRampRateReference(48, UInt16, Enumeration, ReadWrite),

	/**
	 * The reactive power ramp rate, as a percentage of the maximum reactive
	 * power per second.
	 */
	ReactivePowerRampRate(49, UInt16, ReadWrite),

	/** The anti-islanding enable setting. */
	AntiIslandingEnabled(50, UInt16, Enumeration, ReadWrite),

	/** Power factor scale factor. */
	ScaleFactorPowerFactor(51, Int16, ScaleFactor),

	/** Limit maximum active power scale factor. */
	ScaleFactorActivePowerLimitPercent(52, Int16, ScaleFactor),

	/** Active power setpoint scale factor. */
	ScaleFactorActivePowerSetpoint(53, Int16, ScaleFactor),

	/** Active power setpoint percentage scale factor. */
	ScaleFactorActivePowerSetpointPercent(54, Int16, ScaleFactor),

	/** Reactive power setpoint scale factor. */
	ScaleFactorReactivePowerSetpoint(55, Int16, ScaleFactor),

	/** Reactive power setpoint percentage scale factor. */
	ScaleFactorReactivePowerSetpointPercent(56, Int16, ScaleFactor),

	// power factor synchronization groups

	/** The power factor setpoint when injecting active power. */
	PowerFactorWhenInjecting(57, UInt16, ReadWrite),

	/**
	 * The power factor excitation setpoint when injecting active power, see
	 * {@link DerPowerFactorExcitation}.
	 */
	PowerFactorExcitationWhenInjecting(58, UInt16, Enumeration, ReadWrite),

	/** The reversion power factor setpoint when injecting active power. */
	ReversionPowerFactorWhenInjecting(59, UInt16, ReadWrite),

	/**
	 * The reversion power factor excitation setpoint when injecting active
	 * power, see {@link DerPowerFactorExcitation}.
	 */
	ReversionPowerFactorExcitationWhenInjecting(60, UInt16, Enumeration, ReadWrite),

	/** The power factor setpoint when absorbing active power. */
	PowerFactorWhenAbsorbing(61, UInt16, ReadWrite),

	/**
	 * The power factor excitation setpoint when absorbing active power, see
	 * {@link DerPowerFactorExcitation}.
	 */
	PowerFactorExcitationWhenAbsorbing(62, UInt16, Enumeration, ReadWrite),

	/** The reversion power factor setpoint when absorbing active power. */
	ReversionPowerFactorWhenAbsorbing(63, UInt16, ReadWrite),

	/**
	 * The reversion power factor excitation setpoint when absorbing active
	 * power, see {@link DerPowerFactorExcitation}.
	 */
	ReversionPowerFactorExcitationWhenAbsorbing(64, UInt16, Enumeration, ReadWrite),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final @Nullable DataClassification classification;
	private final PointAccess access;

	private DerAcControlsModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, null, PointAccess.ReadOnly);
	}

	private DerAcControlsModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, classification, PointAccess.ReadOnly);
	}

	private DerAcControlsModelRegister(int address, ModbusDataType dataType, PointAccess access) {
		this(address, dataType, null, access);
	}

	private DerAcControlsModelRegister(int address, ModbusDataType dataType,
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

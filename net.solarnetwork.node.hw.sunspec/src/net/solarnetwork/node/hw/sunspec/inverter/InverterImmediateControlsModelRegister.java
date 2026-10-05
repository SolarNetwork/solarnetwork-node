/* ==================================================================
 * InverterImmediateControlsModelRegister.java - 6/10/2026 8:05:52 am
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

package net.solarnetwork.node.hw.sunspec.inverter;

import static net.solarnetwork.node.hw.sunspec.DataClassification.Enumeration;
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.hw.sunspec.PointAccess.ReadWrite;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.PointAccess;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for the SunSpec immediate inverter
 * controls model.
 *
 * <p>
 * These mappings correspond to the SunSpec model number <b>123</b>.
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
public enum InverterImmediateControlsModelRegister implements SunspecModbusReference {

	/** Time window for connection control changes, in seconds. */
	ConnectionTimeWindow(0, UInt16, ReadWrite),

	/** Connection control reversion timeout, in seconds. */
	ConnectionReversionTime(1, UInt16, ReadWrite),

	/** Connection control, see {@link InverterConnectionControl}. */
	ConnectionControl(2, UInt16, Enumeration, ReadWrite),

	/**
	 * Active power limit setpoint, as a percentage of the maximum active power.
	 */
	ActivePowerLimitPercent(3, UInt16, ReadWrite),

	/** Time window for active power limit changes, in seconds. */
	ActivePowerLimitTimeWindow(4, UInt16, ReadWrite),

	/** Active power limit reversion timeout, in seconds. */
	ActivePowerLimitReversionTime(5, UInt16, ReadWrite),

	/** Active power limit ramp time, in seconds. */
	ActivePowerLimitRampTime(6, UInt16, ReadWrite),

	/** Active power limit enable setting. */
	ActivePowerLimitEnabled(7, UInt16, Enumeration, ReadWrite),

	/** Fixed power factor setpoint, as the cosine of the phase angle. */
	FixedPowerFactor(8, Int16, ReadWrite),

	/** Time window for fixed power factor changes, in seconds. */
	FixedPowerFactorTimeWindow(9, UInt16, ReadWrite),

	/** Fixed power factor reversion timeout, in seconds. */
	FixedPowerFactorReversionTime(10, UInt16, ReadWrite),

	/** Fixed power factor ramp time, in seconds. */
	FixedPowerFactorRampTime(11, UInt16, ReadWrite),

	/** Fixed power factor enable setting. */
	FixedPowerFactorEnabled(12, UInt16, Enumeration, ReadWrite),

	/** Reactive power, as a percentage of the maximum active power. */
	ReactivePowerPercentOfMaximumActivePower(13, Int16, ReadWrite),

	/** Reactive power, as a percentage of the maximum reactive power. */
	ReactivePowerPercentOfMaximumReactivePower(14, Int16, ReadWrite),

	/** Reactive power, as a percentage of the available reactive power. */
	ReactivePowerPercentOfAvailableReactivePower(15, Int16, ReadWrite),

	/** Time window for reactive power percentage changes, in seconds. */
	ReactivePowerPercentTimeWindow(16, UInt16, ReadWrite),

	/** Reactive power percentage reversion timeout, in seconds. */
	ReactivePowerPercentReversionTime(17, UInt16, ReadWrite),

	/** Reactive power percentage ramp time, in seconds. */
	ReactivePowerPercentRampTime(18, UInt16, ReadWrite),

	/**
	 * Reactive power percentage mode, see
	 * {@link InverterReactivePowerPercentMode}.
	 */
	ReactivePowerPercentMode(19, UInt16, Enumeration, ReadWrite),

	/** Reactive power percentage enable setting. */
	ReactivePowerPercentEnabled(20, UInt16, Enumeration, ReadWrite),

	/** Active power limit setpoint scale factor, as *10^X. */
	ScaleFactorActivePowerLimitPercent(21, Int16, ScaleFactor),

	/** Fixed power factor setpoint scale factor, as *10^X. */
	ScaleFactorFixedPowerFactor(22, Int16, ScaleFactor),

	/** Reactive power percentage scale factor, as *10^X. */
	ScaleFactorReactivePowerPercent(23, Int16, ScaleFactor),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final @Nullable DataClassification classification;
	private final PointAccess access;

	private InverterImmediateControlsModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, classification, PointAccess.ReadOnly);
	}

	private InverterImmediateControlsModelRegister(int address, ModbusDataType dataType,
			PointAccess access) {
		this(address, dataType, null, access);
	}

	private InverterImmediateControlsModelRegister(int address, ModbusDataType dataType,
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

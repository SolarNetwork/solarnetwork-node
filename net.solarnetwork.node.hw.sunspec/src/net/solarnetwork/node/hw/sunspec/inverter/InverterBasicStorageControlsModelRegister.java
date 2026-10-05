/* ==================================================================
 * InverterBasicStorageControlsModelRegister.java - 6/10/2026 8:45:07 am
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

import static net.solarnetwork.node.hw.sunspec.DataClassification.Bitfield;
import static net.solarnetwork.node.hw.sunspec.DataClassification.Enumeration;
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.hw.sunspec.PointAccess.ReadWrite;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.PointAccess;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.hw.sunspec.storage.BatteryChargeStatus;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for the SunSpec basic storage
 * controls model.
 *
 * <p>
 * These mappings correspond to the SunSpec model number <b>124</b>.
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
public enum InverterBasicStorageControlsModelRegister implements SunspecModbusReference {

	/** Maximum charge rate setpoint, in W. */
	ActivePowerChargeRateMaximum(0, UInt16, ReadWrite),

	/**
	 * Maximum charging ramp rate setpoint, as a percentage of the maximum
	 * charge rate per second.
	 */
	ChargeRampRate(1, UInt16, ReadWrite),

	/**
	 * Maximum discharging ramp rate setpoint, as a percentage of the maximum
	 * charge rate per second.
	 */
	DischargeRampRate(2, UInt16, ReadWrite),

	/** Storage control modes, see {@link InverterStorageControlMode}. */
	StorageControlModes(3, UInt16, Bitfield, ReadWrite),

	/** Maximum charging apparent power setpoint, in VA. */
	ApparentPowerChargeRateMaximum(4, UInt16, ReadWrite),

	/**
	 * Minimum reserve setpoint, as a percentage of the nominal maximum storage.
	 */
	StateOfChargeReserveMinimum(5, UInt16, ReadWrite),

	/** Available energy, as a percentage of the capacity rating. */
	StateOfCharge(6, UInt16),

	/**
	 * Storage available, in Ah: the state of charge less the minimum reserve,
	 * times the capacity rating.
	 */
	StorageAvailable(7, UInt16),

	/** Internal battery voltage, in V. */
	BatteryVoltage(8, UInt16),

	/** Charge status, see {@link BatteryChargeStatus}. */
	ChargeStatus(9, UInt16, Enumeration),

	/** Discharge rate, as a percentage of the maximum discharge rate. */
	DischargeRatePercent(10, Int16, ReadWrite),

	/** Charge rate, as a percentage of the maximum charge rate. */
	ChargeRatePercent(11, Int16, ReadWrite),

	/** Time window for charge and discharge rate changes, in seconds. */
	ChargeDischargeRateTimeWindow(12, UInt16, ReadWrite),

	/** Charge and discharge rate reversion timeout, in seconds. */
	ChargeDischargeRateReversionTime(13, UInt16, ReadWrite),

	/** Charge and discharge rate ramp time, in seconds. */
	ChargeDischargeRateRampTime(14, UInt16, ReadWrite),

	/** Charge source setting, see {@link InverterChargeSource}. */
	ChargeSource(15, UInt16, Enumeration, ReadWrite),

	/** Maximum charge rate scale factor, as *10^X. */
	ScaleFactorActivePowerChargeRateMaximum(16, Int16, ScaleFactor),

	/** Charging and discharging ramp rate scale factor, as *10^X. */
	ScaleFactorChargeDischargeRampRate(17, Int16, ScaleFactor),

	/** Maximum charging apparent power scale factor, as *10^X. */
	ScaleFactorApparentPowerChargeRateMaximum(18, Int16, ScaleFactor),

	/** Minimum reserve scale factor, as *10^X. */
	ScaleFactorStateOfChargeReserveMinimum(19, Int16, ScaleFactor),

	/** State of charge scale factor, as *10^X. */
	ScaleFactorStateOfCharge(20, Int16, ScaleFactor),

	/** Storage available scale factor, as *10^X. */
	ScaleFactorStorageAvailable(21, Int16, ScaleFactor),

	/** Battery voltage scale factor, as *10^X. */
	ScaleFactorBatteryVoltage(22, Int16, ScaleFactor),

	/** Charge and discharge rate scale factor, as *10^X. */
	ScaleFactorChargeDischargeRatePercent(23, Int16, ScaleFactor),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final @Nullable DataClassification classification;
	private final PointAccess access;

	private InverterBasicStorageControlsModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, null, PointAccess.ReadOnly);
	}

	private InverterBasicStorageControlsModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, classification, PointAccess.ReadOnly);
	}

	private InverterBasicStorageControlsModelRegister(int address, ModbusDataType dataType,
			PointAccess access) {
		this(address, dataType, null, access);
	}

	private InverterBasicStorageControlsModelRegister(int address, ModbusDataType dataType,
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

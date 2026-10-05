/* ==================================================================
 * BatteryBaseModelRegister.java - 5/10/2026 7:58:02 pm
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

package net.solarnetwork.node.hw.sunspec.storage;

import static net.solarnetwork.node.hw.sunspec.DataClassification.Bitfield;
import static net.solarnetwork.node.hw.sunspec.DataClassification.Enumeration;
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.hw.sunspec.PointAccess.ReadWrite;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.PointAccess;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.hw.sunspec.der.DerLocalRemoteControl;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for the SunSpec battery base model.
 *
 * <p>
 * These mappings correspond to the SunSpec model number <b>802</b>.
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
public enum BatteryBaseModelRegister implements SunspecModbusReference {

	/** Nameplate charge capacity, in Ah. */
	ChargeCapacityRating(0, UInt16),

	/** Nameplate energy capacity, in Wh. */
	EnergyCapacityRating(1, UInt16),

	/** Nameplate maximum charge rate, in W. */
	ChargeRateMaximumRating(2, UInt16),

	/** Nameplate maximum discharge rate, in W. */
	DischargeRateMaximumRating(3, UInt16),

	/** Self discharge rate, as a percentage of the energy capacity per day. */
	SelfDischargeRate(4, UInt16),

	/** Nameplate maximum state of charge, as a percentage. */
	StateOfChargeMaximumRating(5, UInt16),

	/** Nameplate minimum state of charge, as a percentage. */
	StateOfChargeMinimumRating(6, UInt16),

	/** Maximum reserve, as a percentage of the nominal maximum storage. */
	StateOfChargeReserveMaximum(7, UInt16, ReadWrite),

	/** Minimum reserve, as a percentage of the nominal maximum storage. */
	StateOfChargeReserveMinimum(8, UInt16, ReadWrite),

	/** State of charge, as a percentage. */
	StateOfCharge(9, UInt16),

	/** Depth of discharge, as a percentage. */
	DepthOfDischarge(10, UInt16),

	/** State of health, as a percentage. */
	StateOfHealth(11, UInt16),

	/** The number of cycles executed. */
	CycleCount(12, UInt32),

	/** Charge status, see {@link BatteryChargeStatus}. */
	ChargeStatus(14, UInt16, Enumeration),

	/** Local or remote control mode, see {@link DerLocalRemoteControl}. */
	LocalRemoteControl(15, UInt16, Enumeration),

	/** Battery heartbeat, incremented every second by the battery. */
	BatteryHeartbeat(16, UInt16),

	/** Controller heartbeat, incremented every second by the controller. */
	ControllerHeartbeat(17, UInt16, ReadWrite),

	/** Alarm reset, written as {@literal 1} to reset latched alarms. */
	AlarmReset(18, UInt16, ReadWrite),

	/** Battery type, see {@link BatteryType}. */
	BatteryType(19, UInt16, Enumeration),

	/** Battery bank state, see {@link BatteryState}. */
	BatteryState(20, UInt16, Enumeration),

	/** Vendor specific battery bank state. */
	VendorBatteryState(21, UInt16, Enumeration),

	/** Warranty expiry date, as the number of days since 1 January 2000. */
	WarrantyDate(22, UInt32),

	/** Battery events, see {@link BatteryEvent}. */
	EventsBitmask(24, UInt32, Bitfield),

	/** Battery events 2, reserved by SunSpec. */
	Events2Bitmask(26, UInt32, Bitfield),

	/** Vendor events 1. */
	VendorEventsBitmask(28, UInt32, Bitfield),

	/** Vendor events 2. */
	VendorEvents2Bitmask(30, UInt32, Bitfield),

	/** DC bus voltage, in V. */
	DcVoltage(32, UInt16),

	/** Instantaneous maximum battery voltage, in V. */
	MaximumVoltage(33, UInt16),

	/** Instantaneous minimum battery voltage, in V. */
	MinimumVoltage(34, UInt16),

	/** Maximum cell voltage, in V. */
	MaximumCellVoltage(35, UInt16),

	/** Index of the string containing the cell with the maximum voltage. */
	MaximumCellVoltageStringIndex(36, UInt16),

	/** Index of the module containing the cell with the maximum voltage. */
	MaximumCellVoltageModuleIndex(37, UInt16),

	/** Minimum cell voltage, in V. */
	MinimumCellVoltage(38, UInt16),

	/** Index of the string containing the cell with the minimum voltage. */
	MinimumCellVoltageStringIndex(39, UInt16),

	/** Index of the module containing the cell with the minimum voltage. */
	MinimumCellVoltageModuleIndex(40, UInt16),

	/** Average cell voltage, in V. */
	AverageCellVoltage(41, UInt16),

	/** Total DC current, in A. */
	DcCurrent(42, Int16),

	/** Instantaneous maximum DC charge current, in A. */
	MaximumChargeCurrent(43, UInt16),

	/** Instantaneous maximum DC discharge current, in A. */
	MaximumDischargeCurrent(44, UInt16),

	/** Total DC power, in W. */
	DcPower(45, Int16),

	/**
	 * Battery request to start or stop the inverter, see
	 * {@link BatteryInverterStateRequest}.
	 */
	InverterStateRequest(46, UInt16, Enumeration),

	/** AC power requested by the battery, in W. */
	PowerRequest(47, Int16),

	/**
	 * Operation for the battery bank to perform, see {@link BatteryOperation}.
	 */
	Operation(48, UInt16, Enumeration, ReadWrite),

	/** Inverter state, see {@link BatteryInverterState}. */
	InverterState(49, UInt16, Enumeration, ReadWrite),

	/** Charge capacity scale factor. */
	ScaleFactorChargeCapacity(50, Int16, ScaleFactor),

	/** Energy capacity scale factor. */
	ScaleFactorEnergyCapacity(51, Int16, ScaleFactor),

	/** Maximum charge and discharge rate scale factor. */
	ScaleFactorChargeDischargeRate(52, Int16, ScaleFactor),

	/** Self discharge rate scale factor. */
	ScaleFactorSelfDischargeRate(53, Int16, ScaleFactor),

	/** State of charge scale factor. */
	ScaleFactorStateOfCharge(54, Int16, ScaleFactor),

	/** Depth of discharge scale factor. */
	ScaleFactorDepthOfDischarge(55, Int16, ScaleFactor),

	/** State of health scale factor. */
	ScaleFactorStateOfHealth(56, Int16, ScaleFactor),

	/** DC bus voltage scale factor. */
	ScaleFactorVoltage(57, Int16, ScaleFactor),

	/** Cell voltage scale factor. */
	ScaleFactorCellVoltage(58, Int16, ScaleFactor),

	/** DC current scale factor. */
	ScaleFactorCurrent(59, Int16, ScaleFactor),

	/** Maximum charge and discharge current scale factor. */
	ScaleFactorCurrentMaximum(60, Int16, ScaleFactor),

	/** Power scale factor. */
	ScaleFactorPower(61, Int16, ScaleFactor),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final @Nullable DataClassification classification;
	private final PointAccess access;

	private BatteryBaseModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, null, PointAccess.ReadOnly);
	}

	private BatteryBaseModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, classification, PointAccess.ReadOnly);
	}

	private BatteryBaseModelRegister(int address, ModbusDataType dataType, PointAccess access) {
		this(address, dataType, null, access);
	}

	private BatteryBaseModelRegister(int address, ModbusDataType dataType,
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

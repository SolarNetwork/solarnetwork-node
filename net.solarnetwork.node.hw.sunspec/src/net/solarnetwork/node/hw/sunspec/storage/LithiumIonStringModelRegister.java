/* ==================================================================
 * LithiumIonStringModelRegister.java - 5/10/2026 9:05:44 pm
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
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for the SunSpec lithium-ion string
 * model.
 *
 * <p>
 * These mappings correspond to the SunSpec model number <b>804</b>.
 * </p>
 *
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block, except for the {@code Module*} registers, which
 * are encoded as an offset from the start of each module block.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum LithiumIonStringModelRegister implements SunspecModbusReference {

	/** The index of the string within the bank, starting from 1. */
	StringIndex(0, UInt16),

	/** The number of modules in the string. */
	NumberOfModules(1, UInt16),

	/** String status, see {@link BatteryConnectionStatus}. */
	Status(2, UInt32, Bitfield),

	/** Connection failure reason, see {@link BatteryConnectionFailure}. */
	ConnectionFailure(4, UInt16, Enumeration),

	/** The number of cells currently being balanced. */
	BalancingCellCount(5, UInt16),

	/** String state of charge, as a percentage. */
	StateOfCharge(6, UInt16),

	/** String depth of discharge, as a percentage. */
	DepthOfDischarge(7, UInt16),

	/** The number of discharge cycles executed. */
	CycleCount(8, UInt32),

	/** String state of health, as a percentage. */
	StateOfHealth(10, UInt16),

	/** String current, in A. */
	DcCurrent(11, Int16),

	/** String voltage, in V. */
	DcVoltage(12, UInt16),

	/** Maximum cell voltage in the string. */
	MaximumCellVoltage(13, UInt16),

	/**
	 * Index of the module containing the cell with the maximum cell voltage.
	 */
	MaximumCellVoltageModuleIndex(14, UInt16),

	/** Minimum cell voltage in the string. */
	MinimumCellVoltage(15, UInt16),

	/**
	 * Index of the module containing the cell with the minimum cell voltage.
	 */
	MinimumCellVoltageModuleIndex(16, UInt16),

	/** Average cell voltage in the string. */
	AverageCellVoltage(17, UInt16),

	/** Maximum module temperature in the string. */
	MaximumModuleTemperature(18, Int16),

	/** Index of the module with the maximum module temperature. */
	MaximumModuleTemperatureModuleIndex(19, UInt16),

	/** Minimum module temperature in the string. */
	MinimumModuleTemperature(20, Int16),

	/** Index of the module with the minimum module temperature. */
	MinimumModuleTemperatureModuleIndex(21, UInt16),

	/** Average module temperature in the string. */
	AverageModuleTemperature(22, Int16),

	/** Contactor status bitmask, where each set bit is a closed contactor. */
	ContactorStatus(24, UInt32, Bitfield),

	/** String events, see {@link LithiumIonStringEvent}. */
	EventsBitmask(26, UInt32, Bitfield),

	/** String events 2, which SunSpec defines no events for. */
	Events2Bitmask(28, UInt32, Bitfield),

	/** Vendor events 1. */
	VendorEventsBitmask(30, UInt32, Bitfield),

	/** Vendor events 2. */
	VendorEvents2Bitmask(32, UInt32, Bitfield),

	/**
	 * String enable or disable operation, see {@link BatteryEnableOperation}.
	 */
	EnableOperation(34, UInt16, Enumeration, ReadWrite),

	/** String connect or disconnect operation, see {@link BatteryOperation}. */
	ConnectOperation(35, UInt16, Enumeration, ReadWrite),

	/** State of charge scale factor. */
	ScaleFactorStateOfCharge(36, Int16, ScaleFactor),

	/** State of health scale factor. */
	ScaleFactorStateOfHealth(37, Int16, ScaleFactor),

	/** Depth of discharge scale factor. */
	ScaleFactorDepthOfDischarge(38, Int16, ScaleFactor),

	/** Current scale factor. */
	ScaleFactorCurrent(39, Int16, ScaleFactor),

	/** Voltage scale factor. */
	ScaleFactorVoltage(40, Int16, ScaleFactor),

	/** Cell voltage scale factor. */
	ScaleFactorCellVoltage(41, Int16, ScaleFactor),

	/** Module temperature scale factor. */
	ScaleFactorModuleTemperature(42, Int16, ScaleFactor),

	// modules

	/** The number of cells in the module. */
	ModuleCellCount(0, UInt16),

	/** Module state of charge, as a percentage. */
	ModuleStateOfCharge(1, UInt16),

	/** Module state of health, as a percentage. */
	ModuleStateOfHealth(2, UInt16),

	/** Maximum cell voltage in the module. */
	ModuleMaximumCellVoltage(3, UInt16),

	/** Index of the cell with the maximum cell voltage. */
	ModuleMaximumCellVoltageCellIndex(4, UInt16),

	/** Minimum cell voltage in the module. */
	ModuleMinimumCellVoltage(5, UInt16),

	/** Index of the cell with the minimum cell voltage. */
	ModuleMinimumCellVoltageCellIndex(6, UInt16),

	/** Average cell voltage in the module. */
	ModuleAverageCellVoltage(7, UInt16),

	/** Maximum cell temperature in the module. */
	ModuleMaximumCellTemperature(8, Int16),

	/** Index of the cell with the maximum cell temperature. */
	ModuleMaximumCellTemperatureCellIndex(9, UInt16),

	/** Minimum cell temperature in the module. */
	ModuleMinimumCellTemperature(10, Int16),

	/** Index of the cell with the minimum cell temperature. */
	ModuleMinimumCellTemperatureCellIndex(11, UInt16),

	/** Average cell temperature in the module. */
	ModuleAverageCellTemperature(12, Int16),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;
	private final @Nullable DataClassification classification;
	private final PointAccess access;

	private LithiumIonStringModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength(), null, PointAccess.ReadOnly);
	}

	private LithiumIonStringModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, dataType.getWordLength(), classification, PointAccess.ReadOnly);
	}

	private LithiumIonStringModelRegister(int address, ModbusDataType dataType,
			DataClassification classification, PointAccess access) {
		this(address, dataType, dataType.getWordLength(), classification, access);
	}

	private LithiumIonStringModelRegister(int address, ModbusDataType dataType, int wordLength,
			@Nullable DataClassification classification, PointAccess access) {
		this.address = address;
		this.dataType = dataType;
		this.wordLength = wordLength;
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
		return wordLength;
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

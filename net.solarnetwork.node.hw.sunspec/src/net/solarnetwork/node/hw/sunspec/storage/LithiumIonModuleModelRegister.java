/* ==================================================================
 * LithiumIonModuleModelRegister.java - 5/10/2026 9:05:44 pm
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
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.StringUtf8;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.PointAccess;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for the SunSpec lithium-ion module
 * model.
 *
 * <p>
 * These mappings correspond to the SunSpec model number <b>805</b>.
 * </p>
 *
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block, except for the {@code Cell*} registers, which are
 * encoded as an offset from the start of each cell block.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum LithiumIonModuleModelRegister implements SunspecModbusReference {

	/** The index of the string containing the module, starting from 1. */
	StringIndex(0, UInt16),

	/** The index of the module within the string, starting from 1. */
	ModuleIndex(1, UInt16),

	/** The number of cells in the module. */
	NumberOfCells(2, UInt16),

	/** Module state of charge, as a percentage. */
	StateOfCharge(3, UInt16),

	/** Module depth of discharge, as a percentage. */
	DepthOfDischarge(4, UInt16),

	/** Module state of health, as a percentage. */
	StateOfHealth(5, UInt16),

	/** The number of cycles executed. */
	CycleCount(6, UInt32),

	/** Module voltage, in V. */
	DcVoltage(8, UInt16),

	/** Maximum cell voltage in the module. */
	MaximumCellVoltage(9, UInt16),

	/** Index of the cell with the maximum cell voltage. */
	MaximumCellVoltageCellIndex(10, UInt16),

	/** Minimum cell voltage in the module. */
	MinimumCellVoltage(11, UInt16),

	/** Index of the cell with the minimum cell voltage. */
	MinimumCellVoltageCellIndex(12, UInt16),

	/** Average cell voltage in the module. */
	AverageCellVoltage(13, UInt16),

	/** Maximum cell temperature in the module. */
	MaximumCellTemperature(14, Int16),

	/** Index of the cell with the maximum cell temperature. */
	MaximumCellTemperatureCellIndex(15, UInt16),

	/** Minimum cell temperature in the module. */
	MinimumCellTemperature(16, Int16),

	/** Index of the cell with the minimum cell temperature. */
	MinimumCellTemperatureCellIndex(17, UInt16),

	/** Average cell temperature in the module. */
	AverageCellTemperature(18, Int16),

	/** The number of cells currently being balanced. */
	BalancingCellCount(19, UInt16),

	/** The module serial number. */
	SerialNumber(20, StringUtf8, 16),

	/** State of charge scale factor. */
	ScaleFactorStateOfCharge(36, Int16, ScaleFactor),

	/** State of health scale factor. */
	ScaleFactorStateOfHealth(37, Int16, ScaleFactor),

	/** Depth of discharge scale factor. */
	ScaleFactorDepthOfDischarge(38, Int16, ScaleFactor),

	/** Voltage scale factor. */
	ScaleFactorVoltage(39, Int16, ScaleFactor),

	/** Cell voltage scale factor. */
	ScaleFactorCellVoltage(40, Int16, ScaleFactor),

	/** Cell temperature scale factor. */
	ScaleFactorTemperature(41, Int16, ScaleFactor),

	// cells

	/** Cell voltage, in V. */
	CellVoltage(0, UInt16),

	/** Cell temperature, in degrees Celsius. */
	CellTemperature(1, Int16),

	/** Cell status, see {@link LithiumIonCellStatus}. */
	CellStatus(2, UInt32, Bitfield),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;
	private final @Nullable DataClassification classification;
	private final PointAccess access;

	private LithiumIonModuleModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength(), null, PointAccess.ReadOnly);
	}

	private LithiumIonModuleModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, dataType.getWordLength(), classification, PointAccess.ReadOnly);
	}

	private LithiumIonModuleModelRegister(int address, ModbusDataType dataType, int wordLength) {
		this(address, dataType, wordLength, null, PointAccess.ReadOnly);
	}

	private LithiumIonModuleModelRegister(int address, ModbusDataType dataType, int wordLength,
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

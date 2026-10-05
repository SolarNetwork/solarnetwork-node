/* ==================================================================
 * DerFrequencyDroopModelRegister.java - 5/10/2026 7:12:33 pm
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
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.PointAccess;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for the SunSpec DER frequency droop
 * model.
 *
 * <p>
 * These mappings correspond to the SunSpec model number <b>711</b>.
 * </p>
 *
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block, except for the {@code Control*} registers, which
 * are encoded as an offset from the start of each control block.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum DerFrequencyDroopModelRegister implements SunspecModbusReference {

	/** The function enable setting. */
	Enabled(0, UInt16, Enumeration, ReadWrite),

	/** The index of the control to make active. */
	AdoptControlRequest(1, UInt16, ReadWrite),

	/** The result of the last adopt control request. */
	AdoptControlResult(2, UInt16, Enumeration),

	/** The number of controls. */
	NumberOfControls(3, UInt16),

	/** The reversion timeout, in seconds. */
	ReversionTime(4, UInt32, ReadWrite),

	/** The reversion time remaining, in seconds. */
	ReversionTimeRemaining(6, UInt32),

	/** The index of the control to adopt when the reversion timeout expires. */
	ReversionControl(8, UInt16, ReadWrite),

	/** The deadband scale factor. */
	ScaleFactorDeadband(9, Int16, ScaleFactor),

	/** The frequency change ratio scale factor. */
	ScaleFactorChangeRatio(10, Int16, ScaleFactor),

	/** The open loop response time scale factor. */
	ScaleFactorResponseTime(11, Int16, ScaleFactor),

	// controls

	/** The over-frequency deadband, in hertz. */
	ControlOverFrequencyDeadband(0, UInt32, ReadWrite),

	/** The under-frequency deadband, in hertz. */
	ControlUnderFrequencyDeadband(2, UInt32, ReadWrite),

	/** The over-frequency change ratio. */
	ControlOverFrequencyChangeRatio(4, UInt16, ReadWrite),

	/** The under-frequency change ratio. */
	ControlUnderFrequencyChangeRatio(5, UInt16, ReadWrite),

	/** The open loop response time, in seconds. */
	ControlOpenLoopResponseTime(6, UInt32, ReadWrite),

	/**
	 * The minimum active power, as a percentage of the active power rating.
	 */
	ControlMinimumActivePower(8, Int16, ReadWrite),

	/** The control read-only setting. */
	ControlReadOnly(9, UInt16, Enumeration),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final @Nullable DataClassification classification;
	private final PointAccess access;

	private DerFrequencyDroopModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, null, PointAccess.ReadOnly);
	}

	private DerFrequencyDroopModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, classification, PointAccess.ReadOnly);
	}

	private DerFrequencyDroopModelRegister(int address, ModbusDataType dataType, PointAccess access) {
		this(address, dataType, null, access);
	}

	private DerFrequencyDroopModelRegister(int address, ModbusDataType dataType,
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

/* ==================================================================
 * DerEnterServiceModelRegister.java - 5/10/2026 9:32:29 am
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
 * Enumeration of Modbus register mappings for the SunSpec DER enter service
 * model.
 *
 * <p>
 * These mappings correspond to the SunSpec model number <b>703</b>.
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
public enum DerEnterServiceModelRegister implements SunspecModbusReference {

	/** Permit enter service, as disabled (0) or enabled (1). */
	Permitted(0, UInt16, Enumeration, ReadWrite),

	/**
	 * Enter service voltage high threshold, as a percentage of nominal voltage.
	 */
	VoltageHigh(1, UInt16, ReadWrite),

	/**
	 * Enter service voltage low threshold, as a percentage of nominal voltage.
	 */
	VoltageLow(2, UInt16, ReadWrite),

	/** Enter service frequency high threshold, in Hz. */
	FrequencyHigh(3, UInt32, ReadWrite),

	/** Enter service frequency low threshold, in Hz. */
	FrequencyLow(5, UInt32, ReadWrite),

	/** Enter service delay time, in seconds. */
	Delay(7, UInt32, ReadWrite),

	/** Enter service random delay, in seconds. */
	RandomDelay(9, UInt32, ReadWrite),

	/** Enter service ramp time, in seconds. */
	RampTime(11, UInt32, ReadWrite),

	/** Enter service delay time remaining, in seconds. */
	DelayRemaining(13, UInt32),

	/** Voltage percentage scale factor, as *10^X. */
	ScaleFactorVoltage(15, Int16, ScaleFactor),

	/** Frequency scale factor, as *10^X. */
	ScaleFactorFrequency(16, Int16, ScaleFactor),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final @Nullable DataClassification classification;
	private final PointAccess access;

	private DerEnterServiceModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, null, PointAccess.ReadOnly);
	}

	private DerEnterServiceModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, classification, PointAccess.ReadOnly);
	}

	private DerEnterServiceModelRegister(int address, ModbusDataType dataType, PointAccess access) {
		this(address, dataType, null, access);
	}

	private DerEnterServiceModelRegister(int address, ModbusDataType dataType,
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

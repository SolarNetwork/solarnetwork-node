/* ==================================================================
 * DerCurveModelRegister.java - 5/10/2026 4:58:20 pm
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
import static net.solarnetwork.node.hw.sunspec.PointAccess.ReadWrite;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.PointAccess;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of the Modbus register mappings shared by the SunSpec DER curve
 * models.
 *
 * <p>
 * These mappings are common to the SunSpec model numbers <b>705</b>,
 * <b>706</b>, and <b>712</b>. The registers specific to each model are defined
 * in {@link DerVoltVarModelRegister}, {@link DerVoltWattModelRegister}, and
 * {@link DerWattVarModelRegister}.
 * </p>
 *
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block, except for the {@code Curve*} registers, which
 * are encoded as an offset from the start of each curve block.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum DerCurveModelRegister implements SunspecModbusReference {

	/** The function enable setting. */
	Enabled(0, UInt16, Enumeration, ReadWrite),

	/** The index of the curve to adopt as the active curve. */
	AdoptCurveRequest(1, UInt16, ReadWrite),

	/** The result of the last adopt curve request. */
	AdoptCurveResult(2, UInt16, Enumeration),

	/** The number of points in each curve. */
	NumberOfPoints(3, UInt16),

	/** The number of curves. */
	NumberOfCurves(4, UInt16),

	/** The reversion timeout, in seconds. */
	ReversionTime(5, UInt32, ReadWrite),

	/** The reversion time remaining, in seconds. */
	ReversionTimeRemaining(7, UInt32),

	/** The index of the curve to adopt when the reversion timeout expires. */
	ReversionCurve(9, UInt16, ReadWrite),

	// curves

	/** The number of active points in a curve. */
	CurveActivePointCount(0, UInt16, ReadWrite),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final @Nullable DataClassification classification;
	private final PointAccess access;

	private DerCurveModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, null, PointAccess.ReadOnly);
	}

	private DerCurveModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, classification, PointAccess.ReadOnly);
	}

	private DerCurveModelRegister(int address, ModbusDataType dataType, PointAccess access) {
		this(address, dataType, null, access);
	}

	private DerCurveModelRegister(int address, ModbusDataType dataType,
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
